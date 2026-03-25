package com.example.cerpshashkin.integration.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.AdminService;
import com.example.cerpshashkin.service.CurrencyService;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CurrencyServiceIntegrationTest extends BaseWireMockTest {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private AdminService adminService;

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
    void addCurrency_WithDuplicate_ShouldNotCreateDuplicate() {
        String currency = "USD";

        int countBefore = currencyService.getSupportedCurrencies().size();

        adminService.addCurrency(currency);
        int countAfter = supportedCurrencyRepository.findAll().size();

        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void addCurrency_WithInvalidCurrency_ShouldThrowException() {
        assertThatThrownBy(() -> adminService.addCurrency("INVALID"))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void convertCurrency_WithSameCurrencies_ShouldReturnSameAmount() {
        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("USD")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result.convertedAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.exchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void convertCurrency_WithSupportedCurrencies_ShouldReturnConversion() {
        exchangeRateService.refreshRates();

        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("EUR")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result).satisfies(response -> {
            assertThat(response.originalAmount()).isEqualTo(BigDecimal.valueOf(100));
            assertThat(response.fromCurrency()).isEqualTo("USD");
            assertThat(response.toCurrency()).isEqualTo("EUR");
            assertThat(response.convertedAmount()).isNotNull();
            assertThat(response.exchangeRate()).isNotNull();
        });
    }

    @Test
    void refreshExchangeRates_ShouldSucceed() {
        adminService.refreshExchangeRates();

        assertThat(currencyService.getSupportedCurrencies()).isNotEmpty();
    }

    @Test
    void addCurrency_AndConvert_ShouldWorkEndToEnd() {
        String newCurrency = "CNY";
        adminService.addCurrency(newCurrency);

        assertThat(supportedCurrencyRepository.existsByCurrencyCode("CNY")).isTrue();

        exchangeRateService.refreshRates();

        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("EUR")
                .to("CNY")
                .build();

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result.fromCurrency()).isEqualTo("EUR");
        assertThat(result.toCurrency()).isEqualTo("CNY");
    }
}
