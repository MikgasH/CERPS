package com.example.cerpshashkin.unit.exception;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidCurrencyException_ShouldReturnBadRequest() {
        InvalidCurrencyException ex = new InvalidCurrencyException("XXX");

        ProblemDetail response = handler.handleInvalidCurrencyException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Invalid currency code");
        assertThat(response.getDetail()).contains("XXX");
        assertRfc7807Shape(response, "invalid-currency");
    }

    @Test
    void handleProviderKeyNotFoundException_ShouldReturnNotFound() {
        ProviderKeyNotFoundException ex = new ProviderKeyNotFoundException(1L);

        ProblemDetail response = handler.handleProviderKeyNotFoundException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Provider key not found");
        assertRfc7807Shape(response, "provider-key-not-found");
    }

    @Test
    void handleProviderKeyNotFoundByNameException_ShouldReturnNotFound() {
        ProviderKeyNotFoundByNameException ex = new ProviderKeyNotFoundByNameException("fixer");

        ProblemDetail response = handler.handleProviderKeyNotFoundByNameException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Provider key not found");
        assertRfc7807Shape(response, "provider-key-not-found");
    }

    @Test
    void handleCurrencyNotSupportedException_ShouldReturnBadRequest() {
        CurrencyNotSupportedException ex = new CurrencyNotSupportedException("ABC", List.of("USD", "EUR"));

        ProblemDetail response = handler.handleCurrencyNotSupported(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Currency Not Supported");
        assertRfc7807Shape(response, "currency-not-supported");
    }

    @Test
    void handleExchangeRateNotAvailableException_ShouldReturnServiceUnavailable() {
        ExchangeRateNotAvailableException ex = new ExchangeRateNotAvailableException("USD", "EUR");

        ProblemDetail response = handler.handleExchangeRateNotAvailableException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("Exchange rate unavailable");
        assertRfc7807Shape(response, "rate-not-available");
    }

    @Test
    void handleRateNotAvailableException_ShouldReturnServiceUnavailable() {
        RateNotAvailableException ex = new RateNotAvailableException("USD", "EUR");

        ProblemDetail response = handler.handleRateNotAvailableException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("Exchange rate not available");
        assertRfc7807Shape(response, "rate-not-available");
    }

    @Test
    void handleAllProvidersFailedException_ShouldReturnServiceUnavailable() {
        AllProvidersFailedException ex = new AllProvidersFailedException(List.of("Provider1", "Provider2"));

        ProblemDetail response = handler.handleAllProvidersFailedException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("All providers failed");
        assertRfc7807Shape(response, "all-providers-failed");
    }

    @Test
    void handleExternalApiException_ShouldReturnBadGateway() {
        ExternalApiException ex = new ExternalApiException("fetch rates", "Fixer.io", "Connection timeout");

        ProblemDetail response = handler.handleExternalApiException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getTitle()).isEqualTo("External API error");
        assertRfc7807Shape(response, "external-api");
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getMessage()).thenReturn("must not be null");
        when(violation.getPropertyPath().toString()).thenReturn("field");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ProblemDetail response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Validation Error");
        assertRfc7807Shape(response, "validation");
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ProblemDetail response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Validation Error");
        assertThat(response.getDetail()).isEqualTo("Validation failed for request parameters");
        assertThat(response.getProperties().get("errors"))
                .isEqualTo(Map.of("field", "must not be null"));
        assertRfc7807Shape(response, "validation");
    }

    @Test
    void handleMissingServletRequestParameterException_ShouldReturnBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("amount", "BigDecimal");

        ProblemDetail response = handler.handleMissingServletRequestParameterException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Missing required parameter");
        assertThat(response.getDetail()).contains("amount");
        assertRfc7807Shape(response, "missing-parameter");
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ProblemDetail response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Invalid Request");
        assertThat(response.getDetail()).isEqualTo("Invalid argument");
        assertRfc7807Shape(response, "invalid-request");
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ProblemDetail response = handler.handleGenericException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getTitle()).isEqualTo("Internal Server Error");
        assertThat(response.getDetail()).contains("unexpected error");
        assertRfc7807Shape(response, "internal");
    }

    private void assertRfc7807Shape(final ProblemDetail response, final String errorType) {
        assertThat(response.getType())
                .hasToString(CerpsConstants.ERROR_URI_PREFIX + errorType);
        assertThat(response.getProperties()).containsKey("timestamp");
    }
}
