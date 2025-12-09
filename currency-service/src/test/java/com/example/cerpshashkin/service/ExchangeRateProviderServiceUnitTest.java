package com.example.cerpshashkin.service;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateProviderServiceUnitTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 1);

    @Mock
    private ExchangeRateClient fixerClient;

    @Mock
    private ExchangeRateClient exchangeRatesClient;

    @Mock
    private ExchangeRateClient currencyApiClient;

    @Mock
    private ExchangeRateClient mockClient;

    private ExchangeRateProviderService providerService;

    @BeforeEach
    void setUp() {
        when(fixerClient.getProviderName()).thenReturn(ApiProvider.FIXER.getDisplayName());
        when(exchangeRatesClient.getProviderName()).thenReturn(ApiProvider.EXCHANGE_RATES.getDisplayName());
        when(currencyApiClient.getProviderName()).thenReturn(ApiProvider.CURRENCY_API.getDisplayName());
        when(mockClient.getProviderName()).thenReturn(ApiProvider.MOCK_SERVICE_1.getDisplayName());

        List<ExchangeRateClient> clients = List.of(fixerClient, exchangeRatesClient, currencyApiClient, mockClient);
        providerService = new ExchangeRateProviderService(clients);
        ReflectionTestUtils.setField(providerService, "baseCurrencyCode", "EUR");
    }

    @Test
    void getLatestRatesFromProviders_WithAllProvidersSuccess_ShouldReturnMedianRates() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.17")), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.18")), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.19")), false
        );

        when(fixerClient.getLatestRates()).thenReturn(response1);
        when(exchangeRatesClient.getLatestRates()).thenReturn(response2);
        when(currencyApiClient.getLatestRates()).thenReturn(response3);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(EUR);
        assertThat(result.rates()).containsEntry(USD, new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();

        verify(mockClient, never()).getLatestRates();
    }

    @Test
    void getLatestRatesFromProviders_WithOneProviderFail_ShouldUseOthers() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.17")), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.19")), false
        );

        when(fixerClient.getLatestRates()).thenThrow(new RuntimeException("Fixer failed"));
        when(exchangeRatesClient.getLatestRates()).thenReturn(response1);
        when(currencyApiClient.getLatestRates()).thenReturn(response2);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(EUR);
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();

        verify(mockClient, never()).getLatestRates();
    }

    @Test
    void getLatestRatesFromProviders_WithUnsuccessfulResponse_ShouldIgnoreIt() {
        CurrencyExchangeResponse successResponse = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.18")), false
        );
        CurrencyExchangeResponse failureResponse = CurrencyExchangeResponse.failure();

        when(fixerClient.getLatestRates()).thenReturn(failureResponse);
        when(exchangeRatesClient.getLatestRates()).thenReturn(successResponse);
        when(currencyApiClient.getLatestRates()).thenReturn(successResponse);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void getLatestRatesFromProviders_WithAllProvidersFail_ShouldUseMock() {
        CurrencyExchangeResponse mockResponse = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.20")), false
        );

        when(fixerClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(exchangeRatesClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));

        when(mockClient.getLatestRates()).thenReturn(mockResponse);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, new BigDecimal("1.20"));
        assertThat(result.isMockData()).isTrue();

        verify(mockClient).getLatestRates();
    }

    @Test
    void getLatestRatesFromProviders_WithMixedFailures_ShouldReturnMedianFromSuccessful() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.17")), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.19")), false
        );

        when(fixerClient.getLatestRates()).thenReturn(response1);
        when(exchangeRatesClient.getLatestRates()).thenReturn(CurrencyExchangeResponse.failure());
        when(currencyApiClient.getLatestRates()).thenReturn(response2);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void getLatestRatesFromProviders_WithMultipleCurrencies_ShouldSelectMedianForEach() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, new BigDecimal("1.17"), GBP, new BigDecimal("0.86")), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, new BigDecimal("1.18"), GBP, new BigDecimal("0.87")), false
        );
        CurrencyExchangeResponse response3 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, new BigDecimal("1.19"), GBP, new BigDecimal("0.88")), false
        );

        when(fixerClient.getLatestRates()).thenReturn(response1);
        when(exchangeRatesClient.getLatestRates()).thenReturn(response2);
        when(currencyApiClient.getLatestRates()).thenReturn(response3);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsEntry(USD, new BigDecimal("1.18"));
        assertThat(result.rates()).containsEntry(GBP, new BigDecimal("0.87"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void getLatestRatesFromProviders_WithAllFailuresIncludingMock_ShouldThrowException() {
        when(fixerClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(exchangeRatesClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(mockClient.getLatestRates()).thenThrow(new RuntimeException("Mock failed"));

        assertThatThrownBy(() -> providerService.getLatestRatesFromProviders())
                .isInstanceOf(AllProvidersFailedException.class);

        verify(mockClient).getLatestRates();
    }

    @Test
    void getLatestRatesFromProviders_WithTwoProviders_ShouldCalculateMedianCorrectly() {
        CurrencyExchangeResponse response1 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.10")), false
        );
        CurrencyExchangeResponse response2 = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.20")), false
        );

        when(fixerClient.getLatestRates()).thenReturn(response1);
        when(exchangeRatesClient.getLatestRates()).thenReturn(response2);
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.15"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void getLatestRatesFromProviders_WithSingleProvider_ShouldReturnDirectly() {
        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.18")), false
        );

        when(fixerClient.getLatestRates()).thenReturn(response);
        when(exchangeRatesClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();
    }

    @Test
    void getLatestRatesFromProviders_WithEmptyRatesResponse_ShouldIgnoreIt() {
        CurrencyExchangeResponse validResponse = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.18")), false
        );
        CurrencyExchangeResponse emptyResponse = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(), false
        );

        when(fixerClient.getLatestRates()).thenReturn(emptyResponse);
        when(exchangeRatesClient.getLatestRates()).thenReturn(validResponse);
        when(currencyApiClient.getLatestRates()).thenReturn(validResponse);

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        assertThat(result.success()).isTrue();
        assertThat(result.rates().get(USD)).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(result.isMockData()).isFalse();
    }
}
