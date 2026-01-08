package org.example.analyticsservice.unit.exception;

import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.example.analyticsservice.exception.GlobalExceptionHandler;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleInsufficientDataException_ShouldReturn404() {
        InsufficientDataException ex = new InsufficientDataException("Not enough data");

        ProblemDetail result = handler.handleInsufficientDataException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Insufficient Data");
        assertThat(result.getDetail()).isEqualTo("Not enough data");
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsKey("suggestion");
    }

    @Test
    void handleCurrencyNotSupportedException_ShouldReturn400() {
        List<String> supportedCurrencies = Arrays.asList("USD", "EUR", "GBP");
        CurrencyNotSupportedException ex = new CurrencyNotSupportedException("XXX", supportedCurrencies);

        ProblemDetail result = handler.handleCurrencyNotSupportedException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Currency Not Supported");
        assertThat(result.getDetail()).contains("XXX");
        assertThat(result.getDetail()).contains("USD, EUR, GBP");
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsKey("invalidCurrency");
        assertThat(result.getProperties().get("invalidCurrency")).isEqualTo("XXX");
    }

    @Test
    void handleIllegalArgumentException_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

        ProblemDetail result = handler.handleIllegalArgumentException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
        assertThat(result.getDetail()).isEqualTo("Invalid input");
        assertThat(result.getProperties()).containsKey("timestamp");
    }

    @Test
    void handleGenericException_ShouldReturn500() {
        Exception ex = new RuntimeException("Server error");

        ProblemDetail result = handler.handleGenericException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(result.getProperties()).containsKey("timestamp");
    }
}
