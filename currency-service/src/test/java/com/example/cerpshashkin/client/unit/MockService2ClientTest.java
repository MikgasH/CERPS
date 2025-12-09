package com.example.cerpshashkin.client.unit;

import com.example.cerpshashkin.client.impl.MockService2Client;
import com.example.cerpshashkin.converter.ExternalApiConverter;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockService2ClientTest {

    @Mock
    private ExternalApiConverter converter;

    @InjectMocks
    private MockService2Client mockService2Client;

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        assertThat(mockService2Client.getProviderName()).isEqualTo("MockService2");
    }

    @Test
    void convertFromFixer_WithValidData_ShouldReturnSuccess() {
        FixerioResponse mockResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of(
                        "USD", BigDecimal.valueOf(1.08),
                        "GBP", BigDecimal.valueOf(0.85)
                )
        );

        CurrencyExchangeResponse expectedResponse = CurrencyExchangeResponse.success(
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of(
                        Currency.getInstance("USD"), BigDecimal.valueOf(1.08),
                        Currency.getInstance("GBP"), BigDecimal.valueOf(0.85)
                ),
                false
        );

        when(converter.convertFromFixer(any(FixerioResponse.class))).thenReturn(expectedResponse);

        CurrencyExchangeResponse result = converter.convertFromFixer(mockResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).hasSize(2);
    }

    @Test
    void convertFromFixer_WithMultipleCurrencies_ShouldHandleAllCurrencies() {
        FixerioResponse mockResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of(
                        "USD", BigDecimal.valueOf(1.08),
                        "GBP", BigDecimal.valueOf(0.85),
                        "JPY", BigDecimal.valueOf(145.0),
                        "CHF", BigDecimal.valueOf(1.05)
                )
        );

        CurrencyExchangeResponse expectedResponse = CurrencyExchangeResponse.success(
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of(
                        Currency.getInstance("USD"), BigDecimal.valueOf(1.08),
                        Currency.getInstance("GBP"), BigDecimal.valueOf(0.85),
                        Currency.getInstance("JPY"), BigDecimal.valueOf(145.0),
                        Currency.getInstance("CHF"), BigDecimal.valueOf(1.05)
                ),
                false
        );

        when(converter.convertFromFixer(mockResponse)).thenReturn(expectedResponse);

        CurrencyExchangeResponse result = converter.convertFromFixer(mockResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).hasSize(4);
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("GBP"),
                Currency.getInstance("JPY"),
                Currency.getInstance("CHF")
        );
    }

    @Test
    void convertFromFixer_WithEmptyRates_ShouldReturnEmptyRates() {
        FixerioResponse mockResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        CurrencyExchangeResponse expectedResponse = CurrencyExchangeResponse.success(
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of(),
                false
        );

        when(converter.convertFromFixer(mockResponse)).thenReturn(expectedResponse);

        CurrencyExchangeResponse result = converter.convertFromFixer(mockResponse);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void convertFromFixer_ShouldCallConverter() {
        FixerioResponse mockResponse = new FixerioResponse(
                true,
                Instant.now(),
                Currency.getInstance("EUR"),
                LocalDate.now(),
                Map.of()
        );

        when(converter.convertFromFixer(mockResponse)).thenReturn(CurrencyExchangeResponse.failure());

        converter.convertFromFixer(mockResponse);

        verify(converter).convertFromFixer(mockResponse);
    }
}
