package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyApiConverterTest {

    @InjectMocks
    private CurrencyApiConverter converter;

    @Test
    void convertToCurrencyExchange_WithValidData_ShouldReturnSuccess() {
        CurrencyApiRawResponse.Meta meta = new CurrencyApiRawResponse.Meta(ZonedDateTime.now());
        CurrencyApiRawResponse.CurrencyData eurData = new CurrencyApiRawResponse.CurrencyData("EUR", BigDecimal.valueOf(0.85));
        CurrencyApiRawResponse.CurrencyData gbpData = new CurrencyApiRawResponse.CurrencyData("GBP", BigDecimal.valueOf(0.75));

        CurrencyApiRawResponse rawResponse = new CurrencyApiRawResponse(
                meta,
                Map.of("EUR", eurData, "GBP", gbpData)
        );

        CurrencyExchangeResponse result = converter.convertToCurrencyExchange(rawResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("USD"));
        assertThat(result.rates()).hasSize(2);
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("EUR"),
                Currency.getInstance("GBP")
        );
        assertThat(result.rates()).containsEntry(Currency.getInstance("EUR"), BigDecimal.valueOf(0.85));
    }

    @Test
    void convertToCurrencyExchange_WithNullInput_ShouldReturnFailure() {
        CurrencyExchangeResponse result = converter.convertToCurrencyExchange(null);

        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
    }

    @Test
    void convertToCurrencyExchange_WithNullData_ShouldReturnFailure() {
        CurrencyApiRawResponse rawResponse = new CurrencyApiRawResponse(
                new CurrencyApiRawResponse.Meta(ZonedDateTime.now()),
                null
        );

        CurrencyExchangeResponse result = converter.convertToCurrencyExchange(rawResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
    }

    @Test
    void convertToCurrencyExchange_WithInvalidCurrency_ShouldSkipInvalidCurrency() {
        CurrencyApiRawResponse.Meta meta = new CurrencyApiRawResponse.Meta(ZonedDateTime.now());
        CurrencyApiRawResponse.CurrencyData validData = new CurrencyApiRawResponse.CurrencyData("USD", BigDecimal.valueOf(1.0));
        CurrencyApiRawResponse.CurrencyData invalidData = new CurrencyApiRawResponse.CurrencyData("INVALID", BigDecimal.valueOf(1.0));

        CurrencyApiRawResponse rawResponse = new CurrencyApiRawResponse(
                meta,
                Map.of("USD", validData, "INVALID", invalidData)
        );

        CurrencyExchangeResponse result = converter.convertToCurrencyExchange(rawResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).hasSize(1);
        assertThat(result.rates()).containsKey(Currency.getInstance("USD"));
    }

    @Test
    void convertToCurrencyExchange_WithEmptyData_ShouldReturnSuccessWithEmptyRates() {
        CurrencyApiRawResponse rawResponse = new CurrencyApiRawResponse(
                new CurrencyApiRawResponse.Meta(ZonedDateTime.now()),
                Map.of()
        );

        CurrencyExchangeResponse result = converter.convertToCurrencyExchange(rawResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }
}
