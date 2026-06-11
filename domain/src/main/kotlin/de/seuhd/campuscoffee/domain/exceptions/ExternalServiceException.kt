package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when an external service (e.g., the OpenStreetMap API) cannot be reached or fails, so the
 * request cannot be answered right now. Distinct from [NotFoundException]: The requested resource may
 * exist, but the external service could not answer. Clients may retry later.
 *
 * @param serviceName the name of the external service (e.g., "OpenStreetMap")
 * @param cause       the underlying transport or server error, if any
 */
class ExternalServiceException(
    serviceName: String,
    cause: Throwable? = null
) : RuntimeException(
        "The external service '$serviceName' could not be reached or returned an error. Please try again later.",
        cause
    )
