package com.example.cerpshashkin.service;

import com.example.cerpshashkin.BaseWireMockTest;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

class CacheExpirationIntegrationTest extends BaseWireMockTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private CurrencyRateCache cache;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @BeforeEach
    void cleanUpBeforeTest() {
        cache.clearCache();
        exchangeRateRepository.deleteAll();
        wireMockServer.resetRequests();
    }

    @Test
    void freshCache_ShouldNotCallProviders() {
        exchangeRateService.refreshRates();
        wireMockServer.resetRequests();

        Optional<BigDecimal> rate1 = exchangeRateService.getExchangeRate(EUR, USD);
        Optional<BigDecimal> rate2 = exchangeRateService.getExchangeRate(EUR, USD);
        Optional<BigDecimal> rate3 = exchangeRateService.getExchangeRate(EUR, USD);

        assertThat(rate1).isPresent();
        assertThat(rate2).isPresent();
        assertThat(rate3).isPresent();
        assertThat(rate1.get()).isEqualByComparingTo(rate2.get());
        assertThat(rate2.get()).isEqualByComparingTo(rate3.get());

        verify(0, getRequestedFor(urlPathMatching("/latest.*")));
        verify(0, getRequestedFor(urlPathEqualTo("/api/rates/latest")));
    }

    @Test
    void clearCache_ShouldForceApiCall() {
        BigDecimal initialRate = new BigDecimal("1.250000");
        cache.putRate(EUR, USD, initialRate);

        Optional<BigDecimal> cachedRate = exchangeRateService.getExchangeRate(EUR, USD);
        assertThat(cachedRate).isPresent();
        assertThat(cachedRate.get()).isEqualByComparingTo(initialRate);

        wireMockServer.resetRequests();
        cache.clearCache();

        Optional<BigDecimal> newRate = exchangeRateService.getExchangeRate(EUR, USD);

        assertThat(wireMockServer.getAllServeEvents()).isNotEmpty();
        assertThat(newRate).isPresent();
        assertThat(newRate.get()).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(newRate.get()).isNotEqualByComparingTo(initialRate);
    }

    @Test
    void cache_ShouldReduceApiCalls() {
        Optional<BigDecimal> firstRate = exchangeRateService.getExchangeRate(EUR, USD);

        assertThat(firstRate).isPresent();
        assertThat(wireMockServer.getAllServeEvents()).isNotEmpty();

        wireMockServer.resetRequests();

        Optional<BigDecimal> secondRate = exchangeRateService.getExchangeRate(EUR, USD);
        Optional<BigDecimal> thirdRate = exchangeRateService.getExchangeRate(EUR, USD);
        Optional<BigDecimal> fourthRate = exchangeRateService.getExchangeRate(EUR, USD);

        int requestsAfterCached = wireMockServer.getAllServeEvents().size();
        assertThat(requestsAfterCached).isZero();

        assertThat(firstRate.get()).isEqualByComparingTo(secondRate.get());
        assertThat(secondRate.get()).isEqualByComparingTo(thirdRate.get());
        assertThat(thirdRate.get()).isEqualByComparingTo(fourthRate.get());
    }

    @Test
    void cache_ShouldWorkForInverseRates() {
        Optional<BigDecimal> eurToUsd = exchangeRateService.getExchangeRate(EUR, USD);
        assertThat(eurToUsd).isPresent();

        assertThat(wireMockServer.getAllServeEvents()).isNotEmpty();

        wireMockServer.resetRequests();

        Optional<BigDecimal> usdToEur = exchangeRateService.getExchangeRate(USD, EUR);
        assertThat(usdToEur).isPresent();

        int requestsInverse = wireMockServer.getAllServeEvents().size();
        assertThat(requestsInverse).isZero();

        BigDecimal product = eurToUsd.get().multiply(usdToEur.get());
        assertThat(product).isCloseTo(BigDecimal.ONE,
                org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    void refreshRates_ShouldUpdateCache() {
        BigDecimal oldRate = new BigDecimal("999.999999");
        cache.putRate(EUR, USD, oldRate);

        Optional<BigDecimal> beforeRefresh = exchangeRateService.getExchangeRate(EUR, USD);
        assertThat(beforeRefresh).isPresent();
        assertThat(beforeRefresh.get()).isEqualByComparingTo(oldRate);

        exchangeRateService.refreshRates();

        Optional<BigDecimal> afterRefresh = exchangeRateService.getExchangeRate(EUR, USD);
        assertThat(afterRefresh).isPresent();
        assertThat(afterRefresh.get()).isEqualByComparingTo(new BigDecimal("1.18"));
        assertThat(afterRefresh.get()).isNotEqualByComparingTo(oldRate);
    }
}
