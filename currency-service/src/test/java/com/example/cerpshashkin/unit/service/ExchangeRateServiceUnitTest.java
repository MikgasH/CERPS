package com.example.cerpshashkin.unit.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.model.CachedRate;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.repository.RateQueryResult;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.ExchangeRateProviderService;
import com.example.cerpshashkin.service.ExchangeRateService;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceUnitTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 1);
    private static final int SCALE = 6;

    @Mock
    private ExchangeRateProviderService providerService;

    @Mock
    private CurrencyRateCache cache;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "baseCurrencyCode", "EUR");
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
        List<SupportedCurrencyEntity> supported = List.of(
                createSupportedCurrency("USD"),
                createSupportedCurrency("GBP"),
                createSupportedCurrency("JPY")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(supported);

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
        List<SupportedCurrencyEntity> supported = List.of(
                createSupportedCurrency("USD"),
                createSupportedCurrency("GBP")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(supported);

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
        List<SupportedCurrencyEntity> supported = List.of(
                createSupportedCurrency("EUR"),
                createSupportedCurrency("USD")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(supported);

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

        BigDecimal expectedInverse = BigDecimal.ONE.divide(eurToUsd, SCALE, RoundingMode.HALF_UP);
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
                .divide(BigDecimal.valueOf(1.18), SCALE, RoundingMode.HALF_UP);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(expectedCrossRate);
        verifyNoInteractions(providerService);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void refreshRates_WithException_ShouldNotPropagateException() {
        when(providerService.getLatestRatesFromProviders())
                .thenThrow(new RuntimeException("Service error"));

        // refreshRates() catches all exceptions now - should not throw
        exchangeRateService.refreshRates();

        verify(cache, never()).putRate(any(), any(), any());
        verify(exchangeRateRepository, never()).saveAll(any());
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

    private SupportedCurrencyEntity createSupportedCurrency(String code) {
        return SupportedCurrencyEntity.builder()
                .id((long) code.hashCode())
                .currencyCode(code)
                .build();
    }
}
