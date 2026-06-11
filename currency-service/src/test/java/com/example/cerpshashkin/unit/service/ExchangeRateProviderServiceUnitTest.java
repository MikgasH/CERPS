package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.service.ExchangeRateProviderService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateProviderServiceUnitTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency BYN = Currency.getInstance("BYN");
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 1);

    @Mock
    private ExchangeRateClient fixerClient;

    @Mock
    private ExchangeRateClient exchangeRatesClient;

    @Mock
    private ExchangeRateClient currencyApiClient;

    @Mock
    private ExchangeRateClient frankfurterClient;

    private ExchangeRateProviderService providerService;

    @BeforeEach
    void setUp() {
        lenient().when(fixerClient.getProviderName()).thenReturn(ApiProvider.FIXER.getDisplayName());
        lenient().when(exchangeRatesClient.getProviderName()).thenReturn(ApiProvider.EXCHANGE_RATES.getDisplayName());
        lenient().when(currencyApiClient.getProviderName()).thenReturn(ApiProvider.CURRENCY_API.getDisplayName());
        lenient().when(frankfurterClient.getProviderName()).thenReturn(ApiProvider.FRANKFURTER.getDisplayName());
        lenient().when(frankfurterClient.isFallback()).thenReturn(true);

        List<ExchangeRateClient> clients =
                List.of(fixerClient, exchangeRatesClient, currencyApiClient, frankfurterClient);
        providerService = new ExchangeRateProviderService(clients, new SimpleMeterRegistry());
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
    }

    @Test
    void getLatestRatesFromProviders_WithAllProvidersFail_ShouldThrowException() {
        when(fixerClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(exchangeRatesClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));

        assertThatThrownBy(() -> providerService.getLatestRatesFromProviders())
                .isInstanceOf(AllProvidersFailedException.class);
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
    }

    @Test
    void getLatestRatesFromProviders_ShouldExcludeFallbackClientFromMedian() {
        when(fixerClient.getLatestRates()).thenReturn(CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.17")), false));
        when(exchangeRatesClient.getLatestRates()).thenReturn(CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.18")), false));
        when(currencyApiClient.getLatestRates()).thenReturn(CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, new BigDecimal("1.19")), false));

        CurrencyExchangeResponse result = providerService.getLatestRatesFromProviders();

        // Median of the three primary providers; Frankfurter is never queried
        assertThat(result.rates()).containsEntry(USD, new BigDecimal("1.18"));
        verify(frankfurterClient, never()).getLatestRates();
        verify(frankfurterClient, never()).getLatestRates(anySet());
    }

    @Test
    void getLatestRatesFromProviders_WithAllPrimariesFailed_ShouldNotFallBackToFrankfurter() {
        when(fixerClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(exchangeRatesClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));
        when(currencyApiClient.getLatestRates()).thenThrow(new RuntimeException("Failed"));

        assertThatThrownBy(() -> providerService.getLatestRatesFromProviders())
                .isInstanceOf(AllProvidersFailedException.class);

        verify(frankfurterClient, never()).getLatestRates();
        verify(frankfurterClient, never()).getLatestRates(anySet());
    }

    @Test
    void getFallbackRates_ShouldReturnRatesFromFallbackClient() {
        when(frankfurterClient.getLatestRates(Set.of("BYN", "RUB"))).thenReturn(
                CurrencyExchangeResponse.success(
                        EUR, TEST_DATE,
                        Map.of(BYN, new BigDecimal("3.25"), RUB, new BigDecimal("95.5")),
                        false));

        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of("BYN", "RUB"));

        assertThat(result)
                .containsEntry("BYN", new BigDecimal("3.25"))
                .containsEntry("RUB", new BigDecimal("95.5"));
        verify(fixerClient, never()).getLatestRates();
    }

    @Test
    void getFallbackRates_ShouldOnlyReturnRequestedSymbols() {
        when(frankfurterClient.getLatestRates(Set.of("BYN"))).thenReturn(
                CurrencyExchangeResponse.success(
                        EUR, TEST_DATE,
                        Map.of(BYN, new BigDecimal("3.25"), USD, new BigDecimal("1.18")),
                        false));

        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of("BYN"));

        assertThat(result).containsOnlyKeys("BYN");
    }

    @Test
    void getFallbackRates_ShouldFilterInvalidRates() {
        when(frankfurterClient.getLatestRates(Set.of("BYN", "RUB"))).thenReturn(
                CurrencyExchangeResponse.success(
                        EUR, TEST_DATE,
                        Map.of(BYN, new BigDecimal("-1"), RUB, new BigDecimal("95.5")),
                        false));

        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of("BYN", "RUB"));

        assertThat(result).containsOnlyKeys("RUB");
    }

    @Test
    void getFallbackRates_WithEmptySymbols_ShouldReturnEmptyMapWithoutQuerying() {
        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of());

        assertThat(result).isEmpty();
        verify(frankfurterClient, never()).getLatestRates(anySet());
    }

    @Test
    void getFallbackRates_WhenFallbackClientFails_ShouldReturnEmptyMap() {
        when(frankfurterClient.getLatestRates(anySet())).thenThrow(new RuntimeException("Frankfurter down"));

        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of("BYN"));

        assertThat(result).isEmpty();
    }

    @Test
    void getFallbackRates_WithUnsuccessfulResponse_ShouldReturnEmptyMap() {
        when(frankfurterClient.getLatestRates(anySet())).thenReturn(CurrencyExchangeResponse.failure());

        Map<String, BigDecimal> result = providerService.getFallbackRates(Set.of("BYN"));

        assertThat(result).isEmpty();
    }
}
