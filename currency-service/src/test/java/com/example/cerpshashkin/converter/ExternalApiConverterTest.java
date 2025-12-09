package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExternalApiConverterTest {

    @InjectMocks
    private ExternalApiConverter converter;

    @Test
    void convertFromFixer_WithValidData_ShouldReturnSuccess() {
        FixerioResponse fixerResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of("USD", BigDecimal.valueOf(1.18))
        );

        CurrencyExchangeResponse result = converter.convertFromFixer(fixerResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).containsKey(Currency.getInstance("USD"));
        assertThat(result.rates()).containsEntry(Currency.getInstance("USD"), BigDecimal.valueOf(1.18));
    }

    @Test
    void convertFromFixer_WithUnsuccessfulResponse_ShouldReturnFailure() {
        FixerioResponse fixerResponse = new FixerioResponse(
                false,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        CurrencyExchangeResponse result = converter.convertFromFixer(fixerResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
    }

    @Test
    void convertFromFixer_WithNullInput_ShouldThrowException() {
        assertThatThrownBy(() -> converter.convertFromFixer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FixerioResponse cannot be null");
    }

    @Test
    void convertFromFixer_WithMultipleCurrencies_ShouldProcessAllValidCurrencies() {
        FixerioResponse fixerResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of("USD", BigDecimal.valueOf(1.18), "GBP", BigDecimal.valueOf(0.87))
        );

        CurrencyExchangeResponse result = converter.convertFromFixer(fixerResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).hasSize(2);
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("GBP")
        );
    }

    @Test
    void convertFromExchangeRates_WithValidData_ShouldReturnSuccess() {
        ExchangeRatesApiResponse exchangeRatesResponse = new ExchangeRatesApiResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of("USD", BigDecimal.valueOf(1.18))
        );

        CurrencyExchangeResponse result = converter.convertFromExchangeRates(exchangeRatesResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).containsKey(Currency.getInstance("USD"));
        assertThat(result.rates()).containsEntry(Currency.getInstance("USD"), BigDecimal.valueOf(1.18));
    }

    @Test
    void convertFromExchangeRates_WithUnsuccessfulResponse_ShouldReturnFailure() {
        ExchangeRatesApiResponse exchangeRatesResponse = new ExchangeRatesApiResponse(
                false,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        CurrencyExchangeResponse result = converter.convertFromExchangeRates(exchangeRatesResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
    }

    @Test
    void convertFromExchangeRates_WithNullInput_ShouldThrowException() {
        assertThatThrownBy(() -> converter.convertFromExchangeRates(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ExchangeRatesApiResponse cannot be null");
    }

    @Test
    void convertFromExchangeRates_WithMultipleCurrencies_ShouldProcessAllValidCurrencies() {
        ExchangeRatesApiResponse exchangeRatesResponse = new ExchangeRatesApiResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of("USD", BigDecimal.valueOf(1.18), "GBP", BigDecimal.valueOf(0.87))
        );

        CurrencyExchangeResponse result = converter.convertFromExchangeRates(exchangeRatesResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).hasSize(2);
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("GBP")
        );
    }

    @Test
    void convertFromFixer_WithEmptyRates_ShouldReturnSuccessWithEmptyMap() {
        FixerioResponse fixerResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        CurrencyExchangeResponse result = converter.convertFromFixer(fixerResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromExchangeRates_WithEmptyRates_ShouldReturnSuccessWithEmptyMap() {
        ExchangeRatesApiResponse exchangeRatesResponse = new ExchangeRatesApiResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        CurrencyExchangeResponse result = converter.convertFromExchangeRates(exchangeRatesResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromFixer_WithNullRates_ShouldReturnSuccessWithEmptyMap() {
        FixerioResponse fixerResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                null
        );

        CurrencyExchangeResponse result = converter.convertFromFixer(fixerResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromExchangeRates_WithNullRates_ShouldReturnSuccessWithEmptyMap() {
        ExchangeRatesApiResponse exchangeRatesResponse = new ExchangeRatesApiResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                null
        );

        CurrencyExchangeResponse result = converter.convertFromExchangeRates(exchangeRatesResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }
}
