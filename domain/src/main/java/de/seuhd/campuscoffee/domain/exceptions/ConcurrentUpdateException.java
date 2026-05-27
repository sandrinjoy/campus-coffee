package de.seuhd.campuscoffee.domain.exceptions;

import de.seuhd.campuscoffee.domain.model.objects.DomainModel;

/**
 * Thrown when an update is rejected because the entity was modified concurrently since it was read
 * (an optimistic-locking conflict). The caller should reload the current state and retry.
 */
public class ConcurrentUpdateException extends RuntimeException {

    /**
     * @param <DOMAIN>    domain type
     * @param <ID>        ID type
     * @param domainClass class of the domain object (e.g., "Review")
     * @param id          the ID of the entity that was modified concurrently
     */
    public <DOMAIN extends DomainModel<ID>, ID> ConcurrentUpdateException(Class<DOMAIN> domainClass, ID id) {
        super(domainClass.getSimpleName() + " with ID " + id
                + " was modified concurrently. Please reload it and retry.");
    }
}
