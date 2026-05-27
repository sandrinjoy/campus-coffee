package de.seuhd.campuscoffee.data.implementations;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CrudDataServiceImpl#constraintNameOf}, which reads the violated constraint name from
 * the data-integrity violation's Hibernate cause rather than matching on database-specific message text.
 */
class CrudDataServiceImplTest {

    @Test
    void readsConstraintNameFromHibernateCause() {
        String reportedName = "some_unique_constraint";
        ConstraintViolationException hibernateViolation = mock(ConstraintViolationException.class);
        when(hibernateViolation.getConstraintName()).thenReturn(reportedName);
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("could not execute statement", hibernateViolation);

        assertThat(CrudDataServiceImpl.constraintNameOf(exception)).isEqualTo(reportedName);
    }

    @Test
    void returnsNullWhenNoHibernateConstraintViolationInChain() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement", new RuntimeException("some unrelated database error"));

        assertThat(CrudDataServiceImpl.constraintNameOf(exception)).isNull();
    }
}
