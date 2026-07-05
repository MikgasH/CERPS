package com.example.cerpshashkin.exception;

import com.example.cerps.common.exception.BaseGlobalExceptionHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(InvalidCurrencyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidCurrencyException(final InvalidCurrencyException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid currency code", ex.getMessage(),
                "invalid-currency");
    }

    @ExceptionHandler(ProviderKeyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProviderKeyNotFoundException(final ProviderKeyNotFoundException ex) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Provider key not found", ex.getMessage(),
                "provider-key-not-found");
    }

    @ExceptionHandler(ProviderKeyNotFoundByNameException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProviderKeyNotFoundByNameException(final ProviderKeyNotFoundByNameException ex) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Provider key not found", ex.getMessage(),
                "provider-key-not-found");
    }

    @ExceptionHandler(HistoricalRatesNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleHistoricalRatesNotFoundException(final HistoricalRatesNotFoundException ex) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Historical rates not found", ex.getMessage(),
                "historical-rates-not-found");
    }

    @ExceptionHandler(RateNotAvailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleRateNotAvailableException(final RateNotAvailableException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Exchange rate not available", ex.getMessage(),
                "rate-not-available");
    }

    @ExceptionHandler(ExchangeRateNotAvailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleExchangeRateNotAvailableException(final ExchangeRateNotAvailableException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Exchange rate unavailable", ex.getMessage(),
                "rate-not-available");
    }

    @ExceptionHandler(AllProvidersFailedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleAllProvidersFailedException(final AllProvidersFailedException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "All providers failed", ex.getMessage(),
                "all-providers-failed");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleServiceUnavailableException(final ServiceUnavailableException ex) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", ex.getMessage(),
                "service-unavailable");
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleExternalApiException(final ExternalApiException ex) {
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "External API error", ex.getMessage(),
                "external-api");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleCircuitBreakerOpen(final CallNotPermittedException ex) {
        // An open circuit means a downstream provider is being shielded while it
        // recovers; surface a transient 503 rather than a generic 500.
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Service temporarily unavailable", "An upstream provider is temporarily unavailable",
                "service-unavailable");
    }

    @ExceptionHandler(CurrencyNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleCurrencyNotSupported(final CurrencyNotSupportedException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Currency Not Supported", ex.getMessage(),
                "currency-not-supported");
    }
}
