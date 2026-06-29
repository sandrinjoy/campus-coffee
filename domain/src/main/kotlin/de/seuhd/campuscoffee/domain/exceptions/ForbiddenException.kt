package de.seuhd.campuscoffee.domain.exceptions

/**
 * Exception thrown when a user attempts an action they do not have permission to perform.
 */
class ForbiddenException(message: String = "Forbidden: Insufficient permissions to access this resource.") : RuntimeException(message)
