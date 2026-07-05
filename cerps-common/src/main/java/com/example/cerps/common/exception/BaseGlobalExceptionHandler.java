package com.example.cerps.common.exception;

import com.example.cerps.common.CerpsConstants;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared RFC-7807 exception handling for all CERPS services.
 *
 * <p>Handles framework-level exceptions identically across services and provides
 * {@link #createProblemDetail} so every error response carries the same shape:
 * status, title, detail, a {@code type} URI under
 * {@link CerpsConstants#ERROR_URI_PREFIX} and a {@code timestamp} property.
 * Service-specific exceptions belong in the subclass, built via the same helper.
 */
@Slf4j
public abstract class BaseGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            final String fieldName = ((FieldError) error).getField();
            final String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        final ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Validation failed for request parameters",
                "validation"
        );
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(final ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage());

        final String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse(ex.getMessage());

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Error", message, "validation");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(
            final MissingServletRequestParameterException ex) {
        log.error("Missing request parameter: {}", ex.getParameterName());

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter",
                "Required parameter '" + ex.getParameterName() + "' is missing",
                "missing-parameter"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage(), "invalid-request");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(final Exception ex) {
        log.error("Unexpected error: ", ex);

        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                "internal"
        );
    }

    protected final ProblemDetail createProblemDetail(final HttpStatus status, final String title,
                                                      final String detail, final String errorType) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + errorType));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
