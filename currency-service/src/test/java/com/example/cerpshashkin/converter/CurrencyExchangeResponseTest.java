package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyExchangeResponseTest {

    @Test
    void success_ShouldCreateSuccessfulResponse_WithRealData() {
        Currency base = Currency.getInstance("USD");
        LocalDate rateDate = LocalDate.now();
        Map<Currency, BigDecimal> rates = Map.of(
                Currency.getInstance("EUR"), BigDecimal.valueOf(0.85),
                Currency.getInstance("GBP"), BigDecimal.valueOf(0.75)
        );

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                base,
                rateDate,
                rates,
                false
        );

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.base()).isEqualTo(base);
        assertThat(response.rateDate()).isEqualTo(rateDate);
        assertThat(response.rates()).isEqualTo(rates);
        assertThat(response.lastUpdated()).isNotNull();
        assertThat(response.lastUpdated()).isBeforeOrEqualTo(Instant.now());
        assertThat(response.isMockData()).isFalse();
    }

    @Test
    void failure_ShouldCreateFailureResponse() {
        CurrencyExchangeResponse response = CurrencyExchangeResponse.failure();

        assertThat(response).isNotNull();
        assertThat(response.success()).isFalse();
        assertThat(response.base()).isNull();
        assertThat(response.rateDate()).isNull();
        assertThat(response.rates()).isEmpty();
        assertThat(response.lastUpdated()).isNotNull();
        assertThat(response.lastUpdated()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void constructor_ShouldCreateResponseWithAllFields() {
        Instant lastUpdated = Instant.now();
        Currency base = Currency.getInstance("EUR");
        LocalDate rateDate = LocalDate.of(2025, 1, 15);
        Map<Currency, BigDecimal> rates = Map.of(Currency.getInstance("USD"), BigDecimal.valueOf(1.18));

        CurrencyExchangeResponse response = new CurrencyExchangeResponse(
                true,
                lastUpdated,
                base,
                rateDate,
                rates,
                false
        );

        assertThat(response.success()).isTrue();
        assertThat(response.lastUpdated()).isEqualTo(lastUpdated);
        assertThat(response.base()).isEqualTo(base);
        assertThat(response.rateDate()).isEqualTo(rateDate);
        assertThat(response.rates()).isEqualTo(rates);
        assertThat(response.isMockData()).isFalse();
    }

    @Test
    void success_WithEmptyRates_ShouldCreateValidResponse() {
        Currency base = Currency.getInstance("USD");
        LocalDate rateDate = LocalDate.now();
        Map<Currency, BigDecimal> emptyRates = Map.of();

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                base,
                rateDate,
                emptyRates,
                false
        );

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.rates()).isEmpty();
        assertThat(response.isMockData()).isFalse();
    }

    @Test
    void record_ShouldSupportEqualsAndHashCode() {
        Currency base = Currency.getInstance("USD");
        LocalDate rateDate = LocalDate.now();
        Map<Currency, BigDecimal> rates = Map.of(Currency.getInstance("EUR"), BigDecimal.valueOf(0.85));
        Instant timestamp = Instant.now();

        CurrencyExchangeResponse response1 = new CurrencyExchangeResponse(
                true,
                timestamp,
                base,
                rateDate,
                rates,
                false
        );
        CurrencyExchangeResponse response2 = new CurrencyExchangeResponse(
                true,
                timestamp,
                base,
                rateDate,
                rates,
                false
        );

        assertThat(response1).isEqualTo(response2)
                .hasSameHashCodeAs(response2);
    }

    @Test
    void record_ShouldHaveToString() {
        CurrencyExchangeResponse response = CurrencyExchangeResponse.failure();

        assertThat(response.toString()).isNotNull();
        assertThat(response.toString()).contains("CurrencyExchangeResponse");
        assertThat(response.toString()).contains("success=false");
    }
}
