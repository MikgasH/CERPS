package org.example.analyticsservice.exception;

import com.example.cerps.common.CerpsConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientDataException.class)
    public ProblemDetail handleInsufficientDataException(final InsufficientDataException ex) {
        log.error("Insufficient data: {}", ex.getMessage());

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Insufficient Data");
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "insufficient-data"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("suggestion", "Try fetching exchange rates first using POST /api/v1/currencies/refresh");

        return problemDetail;
    }

    @ExceptionHandler(CurrencyNotSupportedException.class)
    public ProblemDetail handleCurrencyNotSupportedException(final CurrencyNotSupportedException ex) {
        log.error("Currency not supported: {}", ex.getMessage());

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Currency Not Supported");
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "currency-not-supported"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("invalidCurrency", ex.getInvalidCurrency());

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(final MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        final Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            final String fieldName = ((FieldError) error).getField();
            final String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request parameters"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Request");
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "invalid-request"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(final Exception ex) {
        log.error("Unexpected error: ", ex);

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "internal"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
