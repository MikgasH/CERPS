package com.example.cerpshashkin.integration.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.exception.CurrencyNotSupportedException;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.CurrencyService;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrencyServiceIntegrationTest extends BaseWireMockTest {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @Test
    void getSupportedCurrencies_ShouldReturnInitialCurrencies() {
        assertThat(currencyService.getSupportedCurrencies())
                .contains("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "SEK", "NZD")
                .isSorted();
    }

    @Test
    void addCurrency_WithValidCurrency_ShouldPersistToDatabase() {
        String currency = "NOK";

        currencyService.addCurrency(currency);

        assertThat(supportedCurrencyRepository.existsByCurrencyCode("NOK")).isTrue();
        assertThat(currencyService.getSupportedCurrencies()).contains("NOK");
    }

    @Test
    void addCurrency_WithDuplicate_ShouldNotCreateDuplicate() {
        String currency = "USD";

        int countBefore = currencyService.getSupportedCurrencies().size();

        currencyService.addCurrency(currency);
        int countAfter = currencyService.getSupportedCurrencies().size();

        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void addCurrency_WithInvalidCurrency_ShouldThrowException() {
        assertThatThrownBy(() -> currencyService.addCurrency("INVALID"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Invalid currency code: INVALID");
    }

    @Test
    void convertCurrency_WithSameCurrencies_ShouldReturnSameAmount() {
        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("USD")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result)
                .extracting(
                        ConversionResponse::success,
                        ConversionResponse::convertedAmount,
                        ConversionResponse::exchangeRate
                )
                .containsExactly(
                        true,
                        BigDecimal.valueOf(100),
                        BigDecimal.ONE
                );
    }

    @Test
    void convertCurrency_WithSupportedCurrencies_ShouldReturnConversion() {
        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("EUR")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result)
                .satisfies(response -> {
                    assertThat(response.success()).isTrue();
                    assertThat(response.originalAmount()).isEqualTo(BigDecimal.valueOf(100));
                    assertThat(response.fromCurrency()).isEqualTo("USD");
                    assertThat(response.toCurrency()).isEqualTo("EUR");
                    assertThat(response.convertedAmount()).isNotNull();
                    assertThat(response.exchangeRate()).isNotNull();
                });
    }

    @Test
    void validateSupportedCurrencies_WithBothSupported_ShouldNotThrow() {
        currencyService.validateSupportedCurrencies("USD", "EUR");
    }

    @Test
    void validateSupportedCurrencies_WithUnsupportedCurrency_ShouldThrow() {
        assertThatThrownBy(() -> currencyService.validateSupportedCurrencies("USD", "ZZZ"))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("Currency 'ZZZ' is not supported");
    }

    @Test
    void refreshExchangeRates_WithMockProvider_ShouldSucceed() {
        currencyService.refreshExchangeRates();

        assertThat(currencyService.getSupportedCurrencies()).isNotEmpty();
    }

    @Test
    void addCurrency_AndConvert_ShouldWorkEndToEnd() {
        String newCurrency = "CNY";
        currencyService.addCurrency(newCurrency);

        assertThat(currencyService.getSupportedCurrencies()).contains("CNY");

        exchangeRateService.refreshRates();

        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("EUR")
                .to("CNY")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result.success()).isTrue();
        assertThat(result.fromCurrency()).isEqualTo("EUR");
        assertThat(result.toCurrency()).isEqualTo("CNY");
    }
}
