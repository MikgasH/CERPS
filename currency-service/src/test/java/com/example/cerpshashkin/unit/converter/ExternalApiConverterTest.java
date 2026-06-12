package com.example.cerpshashkin.unit.converter;

import com.example.cerpshashkin.converter.ExternalApiConverter;
import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.dto.FrankfurterRateEntry;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
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

    @Test
    void convertFromFrankfurter_WithValidEntries_ShouldReturnSuccess() {
        List<FrankfurterRateEntry> entries = List.of(
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        "USD", BigDecimal.valueOf(1.1645)),
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        "PLN", BigDecimal.valueOf(4.214))
        );

        CurrencyExchangeResponse result = converter.convertFromFrankfurter(entries);

        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rateDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(result.rates())
                .containsEntry(Currency.getInstance("USD"), BigDecimal.valueOf(1.1645))
                .containsEntry(Currency.getInstance("PLN"), BigDecimal.valueOf(4.214));
    }

    @Test
    void convertFromFrankfurter_WithEmptyList_ShouldReturnSuccessWithEmptyRates() {
        CurrencyExchangeResponse result = converter.convertFromFrankfurter(List.of());

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromFrankfurter_WithUnknownQuoteCurrency_ShouldSkipIt() {
        List<FrankfurterRateEntry> entries = List.of(
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        "USD", BigDecimal.valueOf(1.1645)),
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        "XXY", BigDecimal.valueOf(2.0))
        );

        CurrencyExchangeResponse result = converter.convertFromFrankfurter(entries);

        assertThat(result.rates()).containsOnlyKeys(Currency.getInstance("USD"));
    }

    @Test
    void convertFromFrankfurter_WithNullFieldsInEntry_ShouldSkipEntry() {
        List<FrankfurterRateEntry> entries = List.of(
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        null, BigDecimal.valueOf(2.0)),
                new FrankfurterRateEntry(LocalDate.of(2026, 1, 15), Currency.getInstance("EUR"),
                        "USD", null)
        );

        CurrencyExchangeResponse result = converter.convertFromFrankfurter(entries);

        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromFrankfurter_WithNullList_ShouldThrowException() {
        assertThatThrownBy(() -> converter.convertFromFrankfurter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
