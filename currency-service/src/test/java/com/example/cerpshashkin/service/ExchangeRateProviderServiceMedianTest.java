package com.example.cerpshashkin.service;

import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExchangeRateProviderServiceMedianTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 1);

    private ExchangeRateProviderService providerService;
    private List<ExchangeRateClient> clients;

    @BeforeEach
    void setUp() {
        ExchangeRateClient client1 = mock(ExchangeRateClient.class);
        ExchangeRateClient client2 = mock(ExchangeRateClient.class);
        ExchangeRateClient client3 = mock(ExchangeRateClient.class);

        when(client1.getProviderName()).thenReturn("Provider1");
        when(client2.getProviderName()).thenReturn("Provider2");
        when(client3.getProviderName()).thenReturn("Provider3");

        clients = List.of(client1, client2, client3);
        providerService = new ExchangeRateProviderService(clients);
        ReflectionTestUtils.setField(providerService, "baseCurrencyCode", "EUR");
    }

    @Test
    void selectMedianRates_WithOddNumberOfProviders_ShouldReturnMiddleRate() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.17)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.19)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2, response3);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(EUR);
        assertThat(result.rates()).containsEntry(USD, BigDecimal.valueOf(1.18));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithEvenNumberOfProviders_ShouldReturnAverageOfMiddleTwo() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.17)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.19)), false
        );
        CurrencyExchangeResponse response4 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.20)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2, response3, response4);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(EUR);
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.185"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithOutlier_ShouldIgnoreOutlier() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.19)), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(5.00)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2, response3);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, BigDecimal.valueOf(1.19));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithSingleProvider_ShouldReturnThatRate() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, BigDecimal.valueOf(1.18));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithMultipleCurrencies_ShouldCalculateMedianForEach() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, BigDecimal.valueOf(1.17), GBP, BigDecimal.valueOf(0.86)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, BigDecimal.valueOf(1.18), GBP, BigDecimal.valueOf(0.87)), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, BigDecimal.valueOf(1.19), GBP, BigDecimal.valueOf(0.88)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2, response3);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, BigDecimal.valueOf(1.18));
        assertThat(result.rates()).containsEntry(GBP, BigDecimal.valueOf(0.87));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithIdenticalRates_ShouldReturnThatRate() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2, response3);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, false
        );

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, BigDecimal.valueOf(1.18));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void selectMedianRates_WithMockData_ShouldSetFlagCorrectly() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.17)), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );

        List<CurrencyExchangeResponse> responses = List.of(response1, response2);
        CurrencyExchangeResponse result = ReflectionTestUtils.invokeMethod(
                providerService, "selectMedianRates", responses, true
        );

        assertThat(result.success()).isTrue();
        assertThat(result.isMockData()).isTrue();
    }
}
