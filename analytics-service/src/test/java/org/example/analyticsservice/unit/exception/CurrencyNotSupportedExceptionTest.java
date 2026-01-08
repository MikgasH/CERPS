package org.example.analyticsservice.unit.exception;

import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyNotSupportedExceptionTest {

    @Test
    void constructor_ShouldSetMessageAndFields() {
        List<String> supportedCurrencies = Arrays.asList("USD", "EUR", "GBP");

        CurrencyNotSupportedException exception = new CurrencyNotSupportedException("XXX", supportedCurrencies);

        assertThat(exception.getMessage()).contains("XXX");
        assertThat(exception.getMessage()).contains("USD, EUR, GBP");
        assertThat(exception.getInvalidCurrency()).isEqualTo("XXX");
        assertThat(exception.getSupportedCurrencies()).containsExactly("USD", "EUR", "GBP");
    }

    @Test
    void constructor_WithEmptyList_ShouldHandleGracefully() {
        List<String> supportedCurrencies = List.of();

        CurrencyNotSupportedException exception = new CurrencyNotSupportedException("XXX", supportedCurrencies);

        assertThat(exception.getMessage()).contains("XXX");
        assertThat(exception.getInvalidCurrency()).isEqualTo("XXX");
        assertThat(exception.getSupportedCurrencies()).isEmpty();
    }

    @Test
    void constructor_WithSingleCurrency_ShouldFormatCorrectly() {
        List<String> supportedCurrencies = List.of("USD");

        CurrencyNotSupportedException exception = new CurrencyNotSupportedException("EUR", supportedCurrencies);

        assertThat(exception.getMessage()).contains("EUR");
        assertThat(exception.getMessage()).contains("USD");
        assertThat(exception.getInvalidCurrency()).isEqualTo("EUR");
        assertThat(exception.getSupportedCurrencies()).containsExactly("USD");
    }
}

