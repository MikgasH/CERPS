package com.example.cerpshashkin.exception;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ExceptionTest {

    @Test
    void invalidCurrencyException_WithCurrencyCode_ShouldContainCode() {
        InvalidCurrencyException ex = new InvalidCurrencyException("XXX");
        assertThat(ex.getMessage()).contains("Invalid currency code: XXX");
    }

    @Test
    void invalidCurrencyException_WithMessage_ShouldContainMessage() {
        InvalidCurrencyException ex = new InvalidCurrencyException("Custom message");
        assertThat(ex.getMessage()).isEqualTo("Invalid currency code: Custom message");
    }

    @Test
    void invalidCurrencyException_ShouldExtendRuntimeException() {
        InvalidCurrencyException ex = new InvalidCurrencyException("USD");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidCurrencyException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new InvalidCurrencyException("XXX");
        });

        assertThat(thrown)
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    void currencyNotFoundException_ShouldContainCurrencyCode() {
        CurrencyNotFoundException ex = new CurrencyNotFoundException("JPY");
        assertThat(ex.getMessage()).contains("Currency not found: JPY");
    }

    @Test
    void currencyNotFoundException_ShouldExtendRuntimeException() {
        CurrencyNotFoundException ex = new CurrencyNotFoundException("EUR");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void currencyNotFoundException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new CurrencyNotFoundException("EUR");
        });

        assertThat(thrown)
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("EUR");
    }

    @Test
    void allProvidersFailedException_ShouldListAllProviders() {
        List<String> providers = List.of("Fixer.io", "ExchangeRatesAPI", "CurrencyAPI");
        AllProvidersFailedException ex = new AllProvidersFailedException(providers);

        assertThat(ex.getMessage())
                .contains("All exchange rate providers failed")
                .contains("Fixer.io")
                .contains("ExchangeRatesAPI")
                .contains("CurrencyAPI");
    }

    @Test
    void allProvidersFailedException_WithEmptyList_ShouldHandleGracefully() {
        AllProvidersFailedException ex = new AllProvidersFailedException(List.of());
        assertThat(ex.getMessage()).contains("All exchange rate providers failed");
    }

    @Test
    void allProvidersFailedException_ShouldExtendRuntimeException() {
        AllProvidersFailedException ex = new AllProvidersFailedException(List.of("Provider1"));
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void allProvidersFailedException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new AllProvidersFailedException(List.of("P1"));
        });

        assertThat(thrown)
                .isInstanceOf(AllProvidersFailedException.class)
                .hasMessageContaining("providers failed");
    }

    @Test
    void exchangeRateNotAvailableException_WithTwoCurrencies_ShouldContainBoth() {
        ExchangeRateNotAvailableException ex =
                new ExchangeRateNotAvailableException("USD", "EUR");

        assertThat(ex.getMessage())
                .contains("Exchange rate not available for USD -> EUR");
    }

    @Test
    void exchangeRateNotAvailableException_WithMessage_ShouldContainMessage() {
        ExchangeRateNotAvailableException ex =
                new ExchangeRateNotAvailableException("Custom error");

        assertThat(ex.getMessage()).isEqualTo("Custom error");
    }

    @Test
    void exchangeRateNotAvailableException_ShouldExtendRuntimeException() {
        ExchangeRateNotAvailableException ex =
                new ExchangeRateNotAvailableException("Error");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exchangeRateNotAvailableException_WithNullCurrencies_ShouldHandleGracefully() {
        ExchangeRateNotAvailableException ex =
                new ExchangeRateNotAvailableException(null, null);
        assertThat(ex.getMessage()).isNotNull();
    }

    @Test
    void exchangeRateNotAvailableException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new ExchangeRateNotAvailableException("Error");
        });

        assertThat(thrown)
                .isInstanceOf(ExchangeRateNotAvailableException.class)
                .hasMessageContaining("Error");
    }

    @Test
    void rateNotAvailableException_ShouldContainCurrencyPair() {
        RateNotAvailableException ex = new RateNotAvailableException("GBP", "JPY");

        assertThat(ex.getMessage())
                .contains("Exchange rate not available for GBP -> JPY");
    }

    @Test
    void rateNotAvailableException_ShouldExtendRuntimeException() {
        RateNotAvailableException ex = new RateNotAvailableException("USD", "EUR");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rateNotAvailableException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new RateNotAvailableException("USD", "GBP");
        });

        assertThat(thrown)
                .isInstanceOf(RateNotAvailableException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("GBP");
    }

    @Test
    void externalApiException_ShouldContainOperationAndProvider() {
        ExternalApiException ex = new ExternalApiException(
                "fetch latest rates",
                "Fixer.io",
                "HTTP 500"
        );

        assertThat(ex.getMessage())
                .contains("Failed to fetch latest rates from Fixer.io")
                .contains("HTTP 500");
    }

    @Test
    void externalApiException_WithNullCause_ShouldHandleGracefully() {
        ExternalApiException ex = new ExternalApiException(
                "operation",
                "Provider",
                null
        );
        assertThat(ex.getMessage())
                .contains("Failed to operation from Provider");
    }

    @Test
    void externalApiException_ShouldExtendRuntimeException() {
        ExternalApiException ex = new ExternalApiException("op", "provider", "cause");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void externalApiException_WithEmptyStrings_ShouldNotThrow() {
        ExternalApiException ex = new ExternalApiException("", "", "");
        assertThat(ex.getMessage()).isNotNull();
    }

    @Test
    void externalApiException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new ExternalApiException("op", "provider", "cause");
        });

        assertThat(thrown)
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("provider");
    }

    @Test
    void currencyNotSupportedException_ShouldContainCurrencyAndList() {
        CurrencyNotSupportedException ex = new CurrencyNotSupportedException(
                "XYZ",
                List.of("USD", "EUR", "GBP")
        );

        assertThat(ex.getMessage())
                .contains("XYZ")
                .contains("USD")
                .contains("EUR")
                .contains("GBP");
    }

    @Test
    void currencyNotSupportedException_ShouldExtendRuntimeException() {
        CurrencyNotSupportedException ex = new CurrencyNotSupportedException(
                "XYZ",
                List.of("USD")
        );
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void currencyNotSupportedException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new CurrencyNotSupportedException("XYZ", List.of("USD", "EUR"));
        });

        assertThat(thrown)
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void insufficientDataException_ShouldContainMessage() {
        InsufficientDataException ex = new InsufficientDataException(
                "Insufficient data for trend analysis. Found 0 data points, need at least 2"
        );

        assertThat(ex.getMessage())
                .contains("Insufficient data")
                .contains("Found 0 data points")
                .contains("need at least 2");
    }

    @Test
    void insufficientDataException_ShouldExtendRuntimeException() {
        InsufficientDataException ex = new InsufficientDataException("Not enough data");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void insufficientDataException_ShouldBeThrowable() {
        Throwable thrown = catchThrowable(() -> {
            throw new InsufficientDataException("Insufficient data for analysis");
        });

        assertThat(thrown)
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("Insufficient data");
    }

    @Test
    void insufficientDataException_WithCustomMessage_ShouldWork() {
        String customMessage = "Need at least 5 points, found 2";
        InsufficientDataException ex = new InsufficientDataException(customMessage);

        assertThat(ex.getMessage()).isEqualTo(customMessage);
    }
}
