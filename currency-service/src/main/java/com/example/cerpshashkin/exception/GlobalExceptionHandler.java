package com.example.cerpshashkin.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCurrencyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidCurrencyException(final InvalidCurrencyException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid currency code", ex.getMessage());
    }

    @ExceptionHandler(CurrencyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleCurrencyNotFoundException(final CurrencyNotFoundException ex) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Currency not found", ex.getMessage());
    }

    @ExceptionHandler(RateNotAvailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleRateNotAvailableException(final RateNotAvailableException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Exchange rate not available", ex.getMessage());
    }

    @ExceptionHandler(ExchangeRateNotAvailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleExchangeRateNotAvailableException(final ExchangeRateNotAvailableException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Exchange rate unavailable", ex.getMessage());
    }

    @ExceptionHandler(AllProvidersFailedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleAllProvidersFailedException(final AllProvidersFailedException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "All providers failed", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraintViolationException(final ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse(ex.getMessage());

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Validation error", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Validation error", message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameterException(final MissingServletRequestParameterException ex) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter",
                "Required parameter '" + ex.getParameterName() + "' is missing"
        );
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleExternalApiException(final ExternalApiException ex) {
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "External API error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(final Exception ex) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred: " + ex.getMessage());
    }

    @ExceptionHandler(CurrencyNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleCurrencyNotSupported(final CurrencyNotSupportedException ex) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Currency Not Supported", ex.getMessage()
        );
    }

    @ExceptionHandler(InsufficientDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInsufficientDataException(final InsufficientDataException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Insufficient data", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentialsException(final BadCredentialsException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid username or password");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleDisabledException(final DisabledException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Account disabled", "User account is disabled");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUsernameNotFoundException(final UsernameNotFoundException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDeniedException(final AccessDeniedException ex) {
        return createProblemDetail(HttpStatus.FORBIDDEN, "Access denied", "You don't have permission to access this resource");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid argument", ex.getMessage());
    }

    private ProblemDetail createProblemDetail(final HttpStatus status, final String title, final String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status.value());
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
