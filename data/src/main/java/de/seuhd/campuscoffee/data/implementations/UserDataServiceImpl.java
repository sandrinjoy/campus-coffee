package de.seuhd.campuscoffee.data.implementations;

import de.seuhd.campuscoffee.data.mapper.UserEntityMapper;
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity;
import de.seuhd.campuscoffee.data.persistence.repositories.UserRepository;
import de.seuhd.campuscoffee.data.constraints.ConstraintMapping;
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException;
import de.seuhd.campuscoffee.domain.model.objects.User;
import de.seuhd.campuscoffee.domain.ports.data.UserDataService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implementation of the user data service that the domain layer provides as a port.
 * This layer is responsible for data access and persistence.
 * Business logic should be in the service layer.
 * Extends CrudDataServiceImpl to inherit common CRUD operations.
 */
@Service
class UserDataServiceImpl
        extends CrudDataServiceImpl<User, UserEntity, UserRepository, Long>
        implements UserDataService {

    /** Unique constraints on login name and email, used to report a duplicate as a {@code DuplicationException}. */
    private static final Set<ConstraintMapping<User>> UNIQUE_CONSTRAINTS = Set.of(
            new ConstraintMapping<User>(User::loginName, UserEntity.LOGIN_NAME_COLUMN, UserEntity.LOGIN_NAME_UNIQUE_CONSTRAINT),
            new ConstraintMapping<User>(User::emailAddress, UserEntity.EMAIL_ADDRESS_COLUMN, UserEntity.EMAIL_ADDRESS_UNIQUE_CONSTRAINT));

    /**
     * @param repository   the User repository for data access
     * @param entityMapper the mapper for converting between User domain objects and entities
     */
    UserDataServiceImpl(UserRepository repository, UserEntityMapper entityMapper) {
        super(repository, entityMapper, User.class, UNIQUE_CONSTRAINTS);
    }

    /**
     * Retrieves a User entity by its unique login name and returns it as a domain object.
     *
     * @param loginName the unique login name of the User to retrieve; must not be null
     * @return the User with the specified login name; never null
     * @throws NotFoundException if no User exists with the given login name
     */
    @Override
    @NonNull
    public User getByLoginName(@NonNull String loginName) {
        return findByFieldOrThrow(
                () -> repository.findByLoginName(loginName),
                UserEntity.LOGIN_NAME_COLUMN,
                loginName
        );
    }
}
