package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.dto.CurrentRatesResponse;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.model.CachedRate;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.repository.RateQueryResult;
import com.example.cerpshashkin.service.ExchangeRateProviderService;
import com.example.cerpshashkin.service.ExchangeRateService;
import com.example.cerpshashkin.service.SupportedCurrenciesService;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceUnitTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final Currency UZS = Currency.getInstance("UZS");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 1);
    private static final int SCALE = 6;
    private static final int INTERMEDIATE_SCALE = 12;

    @Mock
    private ExchangeRateProviderService providerService;

    @Mock
    private CurrencyRateCache cache;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private SupportedCurrenciesService supportedCurrenciesService;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    // Real template over a mocked manager so executeWithoutResult still runs
    // the callback (a plain mock would silently skip the saveAll blocks).
    @Spy
    private TransactionTemplate transactionTemplate =
            new TransactionTemplate(mock(PlatformTransactionManager.class));

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "baseCurrencyCode", "EUR");
        exchangeRateService.initMetrics();
    }

    @Test
    void getExchangeRate_WithCachedRate_ShouldReturnCachedValue() {
        BigDecimal cachedRate = BigDecimal.valueOf(0.847);
        CachedRate cachedRateObj = new CachedRate(cachedRate, Instant.now());

        when(cache.getRate(USD, EUR)).thenReturn(Optional.of(cachedRateObj));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, EUR);

        assertThat(result).contains(cachedRate);
        verify(cache).getRate(USD, EUR);
        verifyNoInteractions(providerService);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getExchangeRate_WithoutCacheButInDatabase_ShouldReturnFromDb() {
        BigDecimal dbRate = BigDecimal.valueOf(1.18);
        RateQueryResult queryResult = createRateQueryResult(dbRate, "DIRECT");

        when(cache.getRate(any(), any())).thenReturn(Optional.empty());
        when(exchangeRateRepository.findBestRate(eq("EUR"), eq("USD"), eq("EUR"), any(Instant.class)))
                .thenReturn(Optional.of(queryResult));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(EUR, USD);

        assertThat(result).contains(dbRate);
        verify(cache).putRate(EUR, USD, dbRate);
        verifyNoInteractions(providerService);
    }

    @Test
    void getExchangeRate_WithNoDatabaseRate_ShouldFetchFromProvider() {
        BigDecimal newRate = BigDecimal.valueOf(1.18);

        when(cache.getRate(any(), any())).thenReturn(Optional.empty());
        when(exchangeRateRepository.findBestRate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, newRate), false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(EUR, USD);

        assertThat(result).contains(newRate);
        verify(providerService).getLatestRatesFromProviders();
    }

    @Test
    void getExchangeRate_WithProviderFailure_ShouldReturnEmpty() {
        when(cache.getRate(any(), any())).thenReturn(Optional.empty());
        when(exchangeRateRepository.findBestRate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(providerService.getLatestRatesFromProviders()).thenThrow(new RuntimeException("Provider failed"));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, EUR);

        assertThat(result).isEmpty();
        verify(providerService).getLatestRatesFromProviders();
    }

    @Test
    void refreshRates_WithRealData_ShouldClearCacheSaveToDbAndCache() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "GBP", "JPY"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(
                        USD, BigDecimal.valueOf(1.18),
                        GBP, BigDecimal.valueOf(0.87),
                        JPY, BigDecimal.valueOf(130.0)
                ),
                false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);

        exchangeRateService.refreshRates();

        verify(cache).clearCache();
        verify(providerService).getLatestRatesFromProviders();

        ArgumentCaptor<List<ExchangeRateEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());

        List<ExchangeRateEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(3);

        verify(cache).putRate(EUR, USD, BigDecimal.valueOf(1.18));
        verify(cache).putRate(EUR, GBP, BigDecimal.valueOf(0.87));
        verify(cache).putRate(EUR, JPY, BigDecimal.valueOf(130.0));
    }

    @Test
    void refreshRates_WithUnsuccessfulResponse_ShouldNotSaveOrCache() {
        CurrencyExchangeResponse unsuccessfulResponse = new CurrencyExchangeResponse(
                false, Instant.now(), EUR, TEST_DATE, null, false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(unsuccessfulResponse);

        exchangeRateService.refreshRates();

        verify(cache, never()).clearCache();
        verify(cache, never()).putRate(any(), any(), any());
        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void refreshRates_ShouldOnlySaveSupportedCurrencies() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "GBP"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(
                        USD, BigDecimal.valueOf(1.18),
                        GBP, BigDecimal.valueOf(0.87),
                        JPY, BigDecimal.valueOf(130.0)
                ),
                false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);

        exchangeRateService.refreshRates();

        ArgumentCaptor<List<ExchangeRateEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());

        List<ExchangeRateEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities)
                .extracting(entity -> entity.getTargetCurrency().getCurrencyCode())
                .containsExactlyInAnyOrder("USD", "GBP")
                .doesNotContain("JPY");
    }

    @Test
    void refreshRates_ShouldNotSaveBaseCurrency() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("EUR", "USD"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(
                        EUR, BigDecimal.valueOf(1.0),
                        USD, BigDecimal.valueOf(1.18)
                ),
                false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);

        exchangeRateService.refreshRates();

        ArgumentCaptor<List<ExchangeRateEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());

        List<ExchangeRateEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(1);
        assertThat(savedEntities.get(0).getTargetCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void getExchangeRate_WithInverseRateInCache_ShouldCalculateInverse() {
        BigDecimal eurToUsd = BigDecimal.valueOf(1.18);
        CachedRate cachedRate = new CachedRate(eurToUsd, Instant.now());

        when(cache.getRate(USD, EUR)).thenReturn(Optional.empty());
        when(cache.getRate(EUR, USD)).thenReturn(Optional.of(cachedRate));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, EUR);

        BigDecimal expectedInverse = BigDecimal.ONE.divide(eurToUsd, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(expectedInverse);
        verifyNoInteractions(providerService);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getExchangeRate_WithCrossRateInCache_ShouldCalculateCrossRate() {
        CachedRate eurToUsd = new CachedRate(BigDecimal.valueOf(1.18), Instant.now());
        CachedRate eurToGbp = new CachedRate(BigDecimal.valueOf(0.87), Instant.now());

        when(cache.getRate(USD, GBP)).thenReturn(Optional.empty());
        when(cache.getRate(GBP, USD)).thenReturn(Optional.empty());
        when(cache.getRate(EUR, USD)).thenReturn(Optional.of(eurToUsd));
        when(cache.getRate(EUR, GBP)).thenReturn(Optional.of(eurToGbp));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, GBP);

        BigDecimal expectedCrossRate = BigDecimal.valueOf(0.87)
                .divide(BigDecimal.valueOf(1.18), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(expectedCrossRate);
        verifyNoInteractions(providerService);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getExchangeRate_WithHighDenominationInverseInCache_ShouldRetainSignificantDigits() {
        // At the old CALCULATION_SCALE (6), 1/13800 collapsed to 0.000072 - two
        // significant digits, a ~0.64% error. Manually computed at scale 12:
        // 1/13800 = 0.0000724637681159... -> 0.000072463768
        CachedRate eurToUzs = new CachedRate(new BigDecimal("13800"), Instant.now());

        when(cache.getRate(UZS, EUR)).thenReturn(Optional.empty());
        when(cache.getRate(EUR, UZS)).thenReturn(Optional.of(eurToUzs));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(UZS, EUR);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("0.000072463768"));
    }

    @Test
    void getExchangeRate_WithHighDenominationCrossRateInCache_ShouldRetainSignificantDigits() {
        CachedRate eurToUzs = new CachedRate(new BigDecimal("13800"), Instant.now());
        CachedRate eurToUsd = new CachedRate(new BigDecimal("1.08"), Instant.now());

        when(cache.getRate(UZS, USD)).thenReturn(Optional.empty());
        when(cache.getRate(USD, UZS)).thenReturn(Optional.empty());
        when(cache.getRate(EUR, UZS)).thenReturn(Optional.of(eurToUzs));
        when(cache.getRate(EUR, USD)).thenReturn(Optional.of(eurToUsd));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(UZS, USD);

        // 1.08/13800 = 0.0000782608695652... -> 0.000078260870 at scale 12
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("0.000078260870"));
    }

    @Test
    void getAllRatesForBase_WithHighDenominationBase_ShouldRoundOnlyFinalResultToOutputScale() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("EUR", "USD", "UZS"));
        when(exchangeRateRepository.findAllLatestByBaseCurrency(eq("EUR"), any(Instant.class)))
                .thenReturn(List.of(
                        rateEntity(USD, new BigDecimal("1.08")),
                        rateEntity(UZS, new BigDecimal("13800"))
                ));

        CurrentRatesResponse response = exchangeRateService.getAllRatesForBase("UZS");

        // Published rates keep the 6-decimal output contract
        assertThat(response.rates().get("EUR")).isEqualByComparingTo(new BigDecimal("0.000072"));
        assertThat(response.rates().get("USD")).isEqualByComparingTo(new BigDecimal("0.000078"));
        assertThat(response.rates().get("EUR").scale()).isEqualTo(SCALE);
        assertThat(response.rates().get("USD").scale()).isEqualTo(SCALE);
    }

    @Test
    void refreshRates_WithAllProvidersFailed_ShouldNotPropagateException() {
        when(providerService.getLatestRatesFromProviders())
                .thenThrow(new AllProvidersFailedException(List.of("Fixer.io", "ExchangeRatesAPI")));

        exchangeRateService.refreshRates();

        verify(cache, never()).putRate(any(), any(), any());
        verify(exchangeRateRepository, never()).saveAll(any());
    }

    @Test
    void refreshRates_WithUnexpectedException_ShouldPropagate() {
        when(providerService.getLatestRatesFromProviders())
                .thenThrow(new RuntimeException("Service error"));

        assertThatThrownBy(() -> exchangeRateService.refreshRates())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");
    }

    @Test
    void getExchangeRate_WithDatabaseRate_ShouldReturnFromDb() {
        BigDecimal dbRate = BigDecimal.valueOf(0.847);
        RateQueryResult queryResult = createRateQueryResult(dbRate, "INVERSE");

        when(cache.getRate(any(), any())).thenReturn(Optional.empty());
        when(exchangeRateRepository.findBestRate(eq("USD"), eq("EUR"), eq("EUR"), any(Instant.class)))
                .thenReturn(Optional.of(queryResult));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, EUR);

        assertThat(result).contains(dbRate);
        verify(cache).putRate(USD, EUR, dbRate);
        verifyNoInteractions(providerService);
    }

    @Test
    void getExchangeRate_WithDatabaseCrossRate_ShouldReturnFromDb() {
        BigDecimal crossRate = BigDecimal.valueOf(0.737288);
        RateQueryResult queryResult = createRateQueryResult(crossRate, "CROSS");

        when(cache.getRate(any(), any())).thenReturn(Optional.empty());
        when(exchangeRateRepository.findBestRate(eq("USD"), eq("GBP"), eq("EUR"), any(Instant.class)))
                .thenReturn(Optional.of(queryResult));

        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(USD, GBP);

        assertThat(result).contains(crossRate);
        verify(cache).putRate(USD, GBP, crossRate);
        verifyNoInteractions(providerService);
    }

    @Test
    void refreshRates_WithMissingCurrencies_ShouldGapFillFromFallback() {
        Currency byn = Currency.getInstance("BYN");
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "GBP", "BYN"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, BigDecimal.valueOf(1.18), GBP, BigDecimal.valueOf(0.87)),
                false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);
        when(providerService.getFallbackRates(Set.of("BYN")))
                .thenReturn(Map.of("BYN", BigDecimal.valueOf(3.25)));

        exchangeRateService.refreshRates();

        ArgumentCaptor<List<ExchangeRateEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository, times(2)).saveAll(captor.capture());

        List<ExchangeRateEntity> aggregated = captor.getAllValues().get(0);
        List<ExchangeRateEntity> fallback = captor.getAllValues().get(1);

        assertThat(aggregated).hasSize(2);
        assertThat(fallback).hasSize(1);
        assertThat(fallback.get(0).getTargetCurrency()).isEqualTo(byn);
        assertThat(fallback.get(0).getSource()).isEqualTo("FRANKFURTER");
        // Same instant as phase 1 — required for the same-hour bucket cross-rate join
        assertThat(fallback.get(0).getTimestamp()).isEqualTo(aggregated.get(0).getTimestamp());

        verify(cache).putRate(EUR, byn, BigDecimal.valueOf(3.25));
    }

    @Test
    void refreshRates_WithoutMissingCurrencies_ShouldNotCallFallback() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "GBP"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE,
                Map.of(USD, BigDecimal.valueOf(1.18), GBP, BigDecimal.valueOf(0.87)),
                false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);

        exchangeRateService.refreshRates();

        verify(providerService, never()).getFallbackRates(any());
    }

    @Test
    void refreshRates_WhenFallbackFails_ShouldKeepPhaseOneResults() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "BYN"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);
        when(providerService.getFallbackRates(Set.of("BYN")))
                .thenThrow(new RuntimeException("Frankfurter down"));

        exchangeRateService.refreshRates();

        ArgumentCaptor<List<ExchangeRateEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(entity -> entity.getTargetCurrency().getCurrencyCode())
                .containsExactly("USD");
        verify(cache).putRate(EUR, USD, BigDecimal.valueOf(1.18));
    }

    @Test
    void refreshRates_WhenFallbackReturnsEmpty_ShouldSaveOnlyAggregatedRates() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "BYN"));

        CurrencyExchangeResponse response = CurrencyExchangeResponse.success(
                EUR, TEST_DATE, Map.of(USD, BigDecimal.valueOf(1.18)), false
        );
        when(providerService.getLatestRatesFromProviders()).thenReturn(response);
        when(providerService.getFallbackRates(Set.of("BYN"))).thenReturn(Map.of());

        exchangeRateService.refreshRates();

        verify(exchangeRateRepository, times(1)).saveAll(any());
    }

    @Test
    void getAllRatesForBase_ShouldOmitMissingCurrency_WhenEurBaseSnapshotIsPartial() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("USD", "GBP", "BYN"));
        when(exchangeRateRepository.findAllLatestByBaseCurrency(eq("EUR"), any(Instant.class)))
                .thenReturn(List.of(
                        rateEntity(USD, BigDecimal.valueOf(1.18)),
                        rateEntity(GBP, BigDecimal.valueOf(0.87))
                ));

        CurrentRatesResponse response = exchangeRateService.getAllRatesForBase("EUR");

        assertThat(response.rates()).containsOnlyKeys("USD", "GBP");
        assertThat(response.rates().get("USD")).isEqualByComparingTo(BigDecimal.valueOf(1.18));
        assertThat(response.rates().get("GBP")).isEqualByComparingTo(BigDecimal.valueOf(0.87));
        assertThat(response.totalCurrencies()).isEqualTo(2);
    }

    @Test
    void getAllRatesForBase_ShouldOmitMissingCurrency_WhenNonEurBaseSnapshotIsPartial() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet())
                .thenReturn(Set.of("EUR", "USD", "GBP", "BYN"));
        when(exchangeRateRepository.findAllLatestByBaseCurrency(eq("EUR"), any(Instant.class)))
                .thenReturn(List.of(
                        rateEntity(USD, BigDecimal.valueOf(1.18)),
                        rateEntity(GBP, BigDecimal.valueOf(0.87))
                ));

        CurrentRatesResponse response = exchangeRateService.getAllRatesForBase("USD");

        assertThat(response.rates()).containsOnlyKeys("EUR", "GBP");
        assertThat(response.rates().get("EUR")).isEqualByComparingTo(
                BigDecimal.ONE.divide(BigDecimal.valueOf(1.18), SCALE, RoundingMode.HALF_UP));
        assertThat(response.rates().get("GBP")).isEqualByComparingTo(
                BigDecimal.valueOf(0.87).divide(BigDecimal.valueOf(1.18), SCALE, RoundingMode.HALF_UP));
    }

    private ExchangeRateEntity rateEntity(Currency target, BigDecimal rate) {
        return ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(EUR)
                .targetCurrency(target)
                .rate(rate)
                .source("AGGREGATED")
                .timestamp(Instant.now())
                .build();
    }

    private RateQueryResult createRateQueryResult(BigDecimal rate, String rateType) {
        return new RateQueryResult() {
            @Override
            public BigDecimal getRate() {
                return rate;
            }

            @Override
            public String getRateType() {
                return rateType;
            }

            @Override
            public Instant getTimestamp() {
                return Instant.now();
            }
        };
    }

}
