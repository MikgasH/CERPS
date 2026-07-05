package com.example.cerpshashkin.integration.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.service.ExchangeRateService;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit finding A2: the application-side inverse rate (cache path) used to be
 * rounded to CALCULATION_SCALE mid-calculation, while FIND_BEST_RATE_QUERY
 * computes 1.0/rate at full numeric precision — so the same conversion
 * returned different results depending on which path served it. Both paths
 * must agree within rounding at the 6-decimal output scale.
 */
class InverseRatePathConsistencyIntegrationTest extends BaseWireMockTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency UZS = Currency.getInstance("UZS");
    private static final BigDecimal EUR_TO_UZS = new BigDecimal("13800.000000");

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRateCache rateCache;

    private ExchangeRateEntity seededRate;

    @BeforeEach
    void seedHighDenominationRate() {
        seededRate = exchangeRateRepository.save(ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(EUR)
                .targetCurrency(UZS)
                .rate(EUR_TO_UZS)
                .source("AGGREGATED")
                .timestamp(Instant.now())
                .build());
    }

    @AfterEach
    void cleanUp() {
        exchangeRateRepository.delete(seededRate);
        rateCache.clearCache();
    }

    @Test
    void getExchangeRate_ShouldAgreeAtOutputScale_WhenServedFromSqlInverseAndCacheInverse() {
        rateCache.clearCache();
        BigDecimal sqlInverse = exchangeRateService.getExchangeRate(UZS, EUR).orElseThrow();

        rateCache.clearCache();
        rateCache.putRate(EUR, UZS, EUR_TO_UZS);
        BigDecimal cacheInverse = exchangeRateService.getExchangeRate(UZS, EUR).orElseThrow();

        // 1/13800 = 0.0000724637681159... manually computed at the intermediate scale
        assertThat(cacheInverse).isEqualByComparingTo(new BigDecimal("0.000072463768"));

        assertThat(cacheInverse.setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP))
                .isEqualByComparingTo(sqlInverse.setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP));

        // The SQL-side 1.0/rate keeps at least the intermediate precision as well
        assertThat(sqlInverse.setScale(CerpsConstants.INTERMEDIATE_CALCULATION_SCALE, RoundingMode.HALF_UP))
                .isEqualByComparingTo(cacheInverse);
    }
}
