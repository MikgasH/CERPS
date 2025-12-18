package com.example.cerpshashkin.unit.exception;

import com.example.cerpshashkin.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
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
    }

    @Test
    void handleCurrencyNotFoundException_ShouldReturnNotFound() {
        CurrencyNotFoundException ex = new CurrencyNotFoundException("ZZZ");

        ProblemDetail response = handler.handleCurrencyNotFoundException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Currency not found");
        assertThat(response.getDetail()).contains("ZZZ");
    }

    @Test
    void handleProviderKeyNotFoundException_ShouldReturnNotFound() {
        ProviderKeyNotFoundException ex = new ProviderKeyNotFoundException(1L);

        ProblemDetail response = handler.handleProviderKeyNotFoundException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Provider key not found");
    }

    @Test
    void handleProviderKeyNotFoundByNameException_ShouldReturnNotFound() {
        ProviderKeyNotFoundByNameException ex = new ProviderKeyNotFoundByNameException("fixer");

        ProblemDetail response = handler.handleProviderKeyNotFoundByNameException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Provider key not found");
    }

    @Test
    void handleCurrencyNotSupportedException_ShouldReturnBadRequest() {
        CurrencyNotSupportedException ex = new CurrencyNotSupportedException("ABC", List.of("USD", "EUR"));

        ProblemDetail response = handler.handleCurrencyNotSupported(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Currency Not Supported");
    }

    @Test
    void handleExchangeRateNotAvailableException_ShouldReturnServiceUnavailable() {
        ExchangeRateNotAvailableException ex = new ExchangeRateNotAvailableException("USD", "EUR");

        ProblemDetail response = handler.handleExchangeRateNotAvailableException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("Exchange rate unavailable");
    }

    @Test
    void handleRateNotAvailableException_ShouldReturnServiceUnavailable() {
        RateNotAvailableException ex = new RateNotAvailableException("USD", "EUR");

        ProblemDetail response = handler.handleRateNotAvailableException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("Exchange rate not available");
    }

    @Test
    void handleAllProvidersFailedException_ShouldReturnServiceUnavailable() {
        AllProvidersFailedException ex = new AllProvidersFailedException(List.of("Provider1", "Provider2"));

        ProblemDetail response = handler.handleAllProvidersFailedException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getTitle()).isEqualTo("All providers failed");
    }

    @Test
    void handleInsufficientDataException_ShouldReturnBadRequest() {
        InsufficientDataException ex = new InsufficientDataException("Not enough data points");

        ProblemDetail response = handler.handleInsufficientDataException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Insufficient data");
    }

    @Test
    void handleExternalApiException_ShouldReturnBadGateway() {
        ExternalApiException ex = new ExternalApiException("fetch rates", "Fixer.io", "Connection timeout");

        ProblemDetail response = handler.handleExternalApiException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getTitle()).isEqualTo("External API error");
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
        assertThat(response.getTitle()).isEqualTo("Validation error");
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Validation error");
        assertThat(response.getDetail()).contains("field: must not be null");
    }

    @Test
    void handleMissingServletRequestParameterException_ShouldReturnBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("amount", "BigDecimal");

        ProblemDetail response = handler.handleMissingServletRequestParameterException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Missing required parameter");
        assertThat(response.getDetail()).contains("amount");
    }

    @Test
    void handleBadCredentialsException_ShouldReturnUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ProblemDetail response = handler.handleBadCredentialsException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getTitle()).isEqualTo("Authentication failed");
        assertThat(response.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    void handleDisabledException_ShouldReturnUnauthorized() {
        DisabledException ex = new DisabledException("Account disabled");

        ProblemDetail response = handler.handleDisabledException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getTitle()).isEqualTo("Account disabled");
        assertThat(response.getDetail()).isEqualTo("User account is disabled");
    }

    @Test
    void handleUsernameNotFoundException_ShouldReturnUnauthorized() {
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");

        ProblemDetail response = handler.handleUsernameNotFoundException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getTitle()).isEqualTo("Authentication failed");
        assertThat(response.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ProblemDetail response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getTitle()).isEqualTo("Access denied");
        assertThat(response.getDetail()).contains("permission");
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ProblemDetail response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Invalid argument");
        assertThat(response.getDetail()).isEqualTo("Invalid argument");
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ProblemDetail response = handler.handleGenericException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getTitle()).isEqualTo("Internal server error");
        assertThat(response.getDetail()).contains("unexpected error");
    }
}
