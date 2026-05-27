package de.seuhd.campuscoffee.data.implementations;

import de.seuhd.campuscoffee.data.mapper.EntityMapper;
import de.seuhd.campuscoffee.data.persistence.entities.Entity;
import de.seuhd.campuscoffee.data.constraints.ConstraintMapping;
import de.seuhd.campuscoffee.data.persistence.repositories.ResettableSequenceRepository;
import de.seuhd.campuscoffee.domain.exceptions.ConcurrentUpdateException;
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException;
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException;
import de.seuhd.campuscoffee.domain.model.objects.DomainModel;
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base implementation of CRUD data service operations.
 * This abstract class provides common CRUD functionality that can be reused across
 * different entity types, reducing code duplication.
 * <p>
 * Subclasses must provide the repository, mapper, and domain class type via the constructor.
 * This follows the hexagonal architecture pattern where the data layer acts as an adapter
 * to the domain layer's port interface.
 *
 * @param <DOMAIN>     the domain model type (must implement Identifiable)
 * @param <ENTITY>     the JPA entity type
 * @param <ID>         the type of the unique identifier (e.g., Long, UUID, String)
 * @param <REPOSITORY> the repository type (must extend both JpaRepository and ResettableSequenceRepository)
 */
@RequiredArgsConstructor
public abstract class CrudDataServiceImpl<
        DOMAIN extends DomainModel<ID>,
        ENTITY extends Entity,
        REPOSITORY extends JpaRepository<ENTITY, ID> & ResettableSequenceRepository,
        ID>
        implements CrudDataService<DOMAIN, ID> {

    /*
     * JPA repository for entity persistence.
     */
    protected final REPOSITORY repository;
    /*
     * Mapper for converting between domain and entity objects.
     */
    protected final EntityMapper<DOMAIN, ENTITY> mapper;
    /*
     * The domain class type (used for exception messages).
     */
    protected final Class<DOMAIN> domainClass;
    /*
     * The entity's unique constraints, declared by the subclass. Each maps a database constraint name to the
     * domain field it guards, so a uniqueness violation can be reported as a DuplicationException on that field.
     */
    protected final Set<ConstraintMapping<DOMAIN>> uniqueConstraints;

    @Override
    public void clear() {
        repository.deleteAllInBatch();
        repository.flush();
        repository.resetSequence(); // ensure consistent IDs after clearing (for local testing)
    }

    @Override
    @NonNull
    public List<DOMAIN> getAll() {
        return repository.findAll().stream()
                .map(mapper::fromEntity)
                .toList();
    }

    @Override
    @NonNull
    public DOMAIN getById(@NonNull ID id) {
        return repository.findById(id)
                .map(mapper::fromEntity)
                .orElseThrow(() -> new NotFoundException(domainClass, id));
    }

    /**
     * Upserts a domain object with automatic constraint violation handling,
     * converting database constraint violations into domain-specific DuplicationExceptions.
     * <p>
     * The constraint-to-field mapping is provided by subclasses, allowing entity-specific
     * validation while keeping the exception handling logic centralized.
     *
     * @param domain the domain object to upsert
     * @throws DuplicationException if a uniqueness constraint is violated
     * @throws DataIntegrityViolationException if an unhandled constraint violation occurs
     */
    @Override
    @NonNull
    public DOMAIN upsert(@NonNull DOMAIN domain) {
        try {
            ID id = domain.getId();

            if (id == null) {
                // create new entity
                return mapper.fromEntity(
                        repository.saveAndFlush(mapper.toEntity(domain))
                );
            }

            // update existing entity
            ENTITY entity = repository.findById(id)
                    .orElseThrow(() -> new NotFoundException(domainClass, id));

            // use mapper to update entity fields automatically
            // note: timestamps are managed by JPA lifecycle callbacks (@PreUpdate)
            mapper.updateEntity(domain, entity);

            return mapper.fromEntity(repository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException e) {
            // the row changed between the read above and this write; surface it as a domain conflict
            throw new ConcurrentUpdateException(domainClass, domain.getId());
        } catch (DataIntegrityViolationException e) {
            // the database reports which named constraint was violated; map it to the declared domain field
            String violated = constraintNameOf(e);
            if (violated != null) {
                for (ConstraintMapping<DOMAIN> constraint : uniqueConstraints) {
                    if (violated.equalsIgnoreCase(constraint.constraintName())) {
                        throw new DuplicationException(domainClass, constraint.columnName(),
                                String.valueOf(constraint.extractValue(domain)));
                    }
                }
            }
            // no declared unique constraint matched (e.g. a CHECK or foreign-key violation) -> rethrow the original exception
            throw e;
        }
    }

    @Override
    public void delete(@NonNull ID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(domainClass, id);
        }
        repository.deleteById(id);
    }

    /**
     * Generic helper method for querying by a unique field.
     * Follows the common pattern: repository.findByX() -> map -> orElseThrow.
     * This reduces code duplication across data service implementations that need to
     * query entities by unique fields other than the primary key.
     *
     * @param queryFunction function that queries the repository and returns Optional&lt;ENTITY&gt;
     * @param fieldName     the name of the field being queried (for the exception message)
     * @param fieldValue    the value being queried for (for the exception message)
     * @return the domain object if found
     * @throws NotFoundException if no entity matches the query
     */
    protected DOMAIN findByFieldOrThrow(
            Supplier<Optional<ENTITY>> queryFunction,
            String fieldName,
            String fieldValue) {
        return queryFunction.get()
                .map(mapper::fromEntity)
                .orElseThrow(() -> new NotFoundException(domainClass, fieldName, fieldValue));
    }

    /**
     * Returns the name of the database constraint reported by a data-integrity violation, or {@code null}
     * when the cause chain contains no Hibernate {@link ConstraintViolationException}. Reading the name the
     * driver reported avoids matching on database-specific error-message text.
     *
     * @param exception the data-integrity violation to inspect
     * @return the violated constraint name, or null if none is reported
     */
    // package-private (not private) so it can be unit-tested directly with a crafted exception
    static String constraintNameOf(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
