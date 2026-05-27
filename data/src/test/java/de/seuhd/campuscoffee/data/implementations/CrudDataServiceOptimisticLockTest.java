package de.seuhd.campuscoffee.data.implementations;

import de.seuhd.campuscoffee.data.mapper.PosEntityMapper;
import de.seuhd.campuscoffee.data.persistence.entities.PosEntity;
import de.seuhd.campuscoffee.data.persistence.repositories.PosRepository;
import de.seuhd.campuscoffee.domain.exceptions.ConcurrentUpdateException;
import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.tests.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The repository is mocked to throw a JPA optimistic-locking failure on save; {@code upsert} should map it
 * to a {@link ConcurrentUpdateException}.
 */
class CrudDataServiceOptimisticLockTest {

    @Test
    void optimisticLockFailureBecomesConcurrentUpdateException() {
        PosRepository repository = mock(PosRepository.class);
        PosEntityMapper mapper = mock(PosEntityMapper.class);
        PosDataServiceImpl service = new PosDataServiceImpl(repository, mapper);

        Pos existing = TestFixtures.getPosFixtures().getFirst(); // has a non-null id, so upsert takes the update path
        when(repository.findById(existing.getId())).thenReturn(Optional.of(new PosEntity()));
        when(repository.saveAndFlush(any(PosEntity.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(PosEntity.class, existing.getId()));

        assertThatThrownBy(() -> service.upsert(existing))
                .isInstanceOf(ConcurrentUpdateException.class);
    }
}
