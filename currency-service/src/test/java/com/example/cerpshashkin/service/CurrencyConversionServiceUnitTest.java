package com.example.cerpshashkin.service;

import com.example.cerpshashkin.exception.RateNotAvailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceUnitTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CurrencyConversionService conversionService;

    @Test
    void convertCurrency_WithSameCurrencies_ShouldReturnSameAmount() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("USD")
                .to("USD")
                .build();

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.originalAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.exchangeRate()).isEqualTo(BigDecimal.ONE);
        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.toCurrency()).isEqualTo("USD");

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void convertCurrency_WithValidDifferentCurrencies_ShouldReturnConvertedAmount() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.85")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.originalAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("85.000000"));
        assertThat(result.exchangeRate()).isEqualTo(new BigDecimal("0.85"));
        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.toCurrency()).isEqualTo("EUR");

        verify(exchangeRateService).getExchangeRate(Currency.getInstance("USD"), Currency.getInstance("EUR"));
    }

    @Test
    void convertCurrency_WithNoExchangeRateAvailable_ShouldThrowException() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.convertCurrency(request))
                .isInstanceOf(RateNotAvailableException.class)
                .hasMessageContaining("Exchange rate not available for USD -> EUR");
    }

    @Test
    void convertCurrency_WithLowercaseCurrencies_ShouldNormalizeToUppercase() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("usd")
                .to("eur")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.85")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.fromCurrency()).isEqualTo("usd");
        assertThat(result.toCurrency()).isEqualTo("eur");
        verify(exchangeRateService).getExchangeRate(Currency.getInstance("USD"), Currency.getInstance("EUR"));
    }

    @Test
    void convertCurrency_WithPreciseCalculation_ShouldReturnCorrectPrecision() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("123.45"))
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.876543")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        // 123.45 × 0.876543 = 108.20923335 → HALF_UP to 6 decimals = 108.209233
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("108.209233"));
        assertThat(result.convertedAmount().scale()).isEqualTo(6);
    }

    @Test
    void convertCurrency_WithExchangeRateServiceException_ShouldPropagateException() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() -> conversionService.convertCurrency(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service unavailable");
    }

    @Test
    void convertCurrency_WithZeroAmount_ShouldReturnZeroConvertedAmount() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.ZERO)
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.85")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("0.000000"));
    }

    @Test
    void convertCurrency_WithLargeAmount_ShouldHandleCorrectly() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("1000000.50"))
                .from("USD")
                .to("EUR")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.85")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("850000.425000"));
    }

    @Test
    void convertCurrency_WithMultipleDecimalPlaces_ShouldRoundCorrectly() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("99.99"))
                .from("GBP")
                .to("JPY")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("156.789123")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        // 99.99 × 156.789123 = 15677.34440877 → HALF_UP to 6 decimals = 15677.344409
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("15677.344409"));
        assertThat(result.convertedAmount().scale()).isEqualTo(6);
    }

    @Test
    void convertCurrency_WithRateOfOne_ShouldReturnSameAmount() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("100"))
                .from("EUR")
                .to("CHF")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(BigDecimal.ONE));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("100.000000"));
        assertThat(result.exchangeRate()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void convertCurrency_WithVerySmallRate_ShouldHandleCorrectly() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("1000"))
                .from("JPY")
                .to("USD")
                .build();

        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(new BigDecimal("0.006543")));

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("6.543000"));
    }

    @Test
    void convertCurrency_WithSameCurrencyLowercase_ShouldReturnSameAmount() {
        final ConversionRequest request = ConversionRequest.builder()
                .amount(new BigDecimal("50.50"))
                .from("gbp")
                .to("gbp")
                .build();

        final ConversionResponse result = conversionService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.originalAmount()).isEqualTo(new BigDecimal("50.50"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("50.50"));
        assertThat(result.exchangeRate()).isEqualTo(BigDecimal.ONE);

        verifyNoInteractions(exchangeRateService);
    }
}
