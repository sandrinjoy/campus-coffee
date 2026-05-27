package de.seuhd.campuscoffee.api.exceptions;

import de.seuhd.campuscoffee.domain.exceptions.ConcurrentUpdateException;
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException;
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.MissingFieldException;
import de.seuhd.campuscoffee.domain.exceptions.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers.
 * Provides centralized exception handling and standardized error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Configuration that maps exception types to their HTTP status codes and log messages.
     */
    private static final Map<Class<? extends Exception>, ExceptionConfig> EXCEPTION_MAPPINGS = Map.of(
            NotFoundException.class, new ExceptionConfig(HttpStatus.NOT_FOUND, "Resource not found: {}"),
            DuplicationException.class, new ExceptionConfig(HttpStatus.CONFLICT, "Duplicate resource: {}"),
            ConcurrentUpdateException.class, new ExceptionConfig(HttpStatus.CONFLICT, "Concurrent modification: {}"),
            IllegalArgumentException.class, new ExceptionConfig(HttpStatus.BAD_REQUEST, "Bad request: {}"),
            MissingFieldException.class, new ExceptionConfig(HttpStatus.BAD_REQUEST, "Bad request: {}"),
            ValidationException.class, new ExceptionConfig(HttpStatus.BAD_REQUEST, "Domain validation failed: {}")
    );

    /**
     * Unified handler for all mapped exceptions.
     * Returns appropriate HTTP status codes based on the exception type.
     *
     * @param exception the exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and appropriate HTTP status
     */
    @ExceptionHandler({
            NotFoundException.class,
            DuplicationException.class,
            ConcurrentUpdateException.class,
            IllegalArgumentException.class,
            MissingFieldException.class,
            ValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleMappedException(
            Exception exception,
            WebRequest request
    ) {
        if (EXCEPTION_MAPPINGS.containsKey(exception.getClass())) {
            ExceptionConfig config = EXCEPTION_MAPPINGS.get(exception.getClass());
            log.warn(config.logMessage(), exception.getMessage());
            return buildErrorResponse(exception, config.httpStatus(), request);
        } else {
            // fallback to generic handler
            return handleGenericException(exception, request);
        }
    }

    /**
     * Handles bean validation failures on request bodies, building the message from the field-level
     * binding errors.
     *
     * @param exception the validation exception raised when a request body fails bean validation
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            WebRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Domain validation failed: {}", message);
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST, request, message);
    }

    /**
     * Fallback handler for unexpected exceptions.
     * Returns HTTP 500 (Internal Server Error).
     *
     * @param exception the unexpected exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            WebRequest request
    ) {
        log.error("Unexpected error occurred", exception);
        return buildErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR, request,
                "An unexpected error occurred.");
    }

    /**
     * Builds a standardized error response using the exception message.
     *
     * @param exception the exception that was thrown
     * @param status the HTTP status to return
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and the specified HTTP status
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            HttpStatus status,
            WebRequest request
    ) {
        return buildErrorResponse(exception, status, request, exception.getMessage());
    }

    /**
     * Builds a standardized error response with a custom message.
     *
     * @param exception the exception that was thrown
     * @param status the HTTP status to return
     * @param request the web request
     * @param message custom error message (overrides exception message).
     * @return ResponseEntity with ErrorResponse and the specified HTTP status
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            HttpStatus status,
            WebRequest request,
            String message
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(exception.getClass().getSimpleName())
                .message(message)
                .statusCode(status.value())
                .statusMessage(status.getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Extracts the request path from the WebRequest.
     *
     * @param request the web request
     * @return the request URI or "unknown" if not available
     */
    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
            return servletRequest.getRequestURI();
        }
        return "unknown";
    }

    /**
     * Configuration record for exception handling.
     * Maps exception types to their HTTP status codes and log messages.
     *
     * @param httpStatus the HTTP status to return for this exception type
     * @param logMessage the log message template (use {} for exception message placeholder)
     */
    private record ExceptionConfig(HttpStatus httpStatus, String logMessage) { }
}
