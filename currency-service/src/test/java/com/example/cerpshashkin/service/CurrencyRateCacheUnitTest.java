package com.example.cerpshashkin.service;

import com.example.cerpshashkin.model.CachedRate;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRateCacheUnitTest {

    private CurrencyRateCache cache;
    private Currency usd;
    private Currency eur;

    @BeforeEach
    void setUp() {
        cache = new CurrencyRateCache();
        ReflectionTestUtils.setField(cache, "cacheTtlSeconds", 3600L);
        usd = Currency.getInstance("USD");
        eur = Currency.getInstance("EUR");
    }

    @Test
    void putRate_ShouldStoreRateSuccessfully() {
        BigDecimal rate = BigDecimal.valueOf(0.85);

        cache.putRate(usd, eur, rate);

        Optional<CachedRate> result = cache.getRate(usd, eur);
        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualTo(rate);
        assertThat(result.get().timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void getRate_WithNonExistentPair_ShouldReturnEmpty() {
        Currency gbp = Currency.getInstance("GBP");

        Optional<CachedRate> result = cache.getRate(usd, gbp);

        assertThat(result).isEmpty();
    }

    @Test
    void getRate_WithValidRate_ShouldReturnCachedRate() {
        BigDecimal rate = BigDecimal.valueOf(0.85);
        cache.putRate(usd, eur, rate);

        Optional<CachedRate> result = cache.getRate(usd, eur);

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualTo(rate);
    }

    @Test
    void getRate_WithExpiredRate_ShouldReturnEmpty() {
        ReflectionTestUtils.setField(cache, "cacheTtlSeconds", -1L);

        BigDecimal rate = BigDecimal.valueOf(0.85);
        cache.putRate(usd, eur, rate);

        Optional<CachedRate> result = cache.getRate(usd, eur);

        assertThat(result).isEmpty();
    }

    @Test
    void clearCache_ShouldRemoveAllRates() {
        cache.putRate(usd, eur, BigDecimal.valueOf(0.85));
        cache.putRate(eur, usd, BigDecimal.valueOf(1.18));

        assertThat(cache.getRate(usd, eur)).isPresent();
        assertThat(cache.getRate(eur, usd)).isPresent();

        cache.clearCache();

        assertThat(cache.getRate(usd, eur)).isEmpty();
        assertThat(cache.getRate(eur, usd)).isEmpty();
    }

    @Test
    void putRate_WithDifferentPairs_ShouldStoreSeparately() {
        BigDecimal usdToEur = BigDecimal.valueOf(0.85);
        BigDecimal eurToUsd = BigDecimal.valueOf(1.18);

        cache.putRate(usd, eur, usdToEur);
        cache.putRate(eur, usd, eurToUsd);

        Optional<CachedRate> result1 = cache.getRate(usd, eur);
        Optional<CachedRate> result2 = cache.getRate(eur, usd);

        assertThat(result1).isPresent();
        assertThat(result1.get().rate()).isEqualTo(usdToEur);
        assertThat(result2).isPresent();
        assertThat(result2.get().rate()).isEqualTo(eurToUsd);
    }

    @Test
    void putRate_WithSamePairTwice_ShouldOverwritePrevious() {
        BigDecimal oldRate = BigDecimal.valueOf(0.85);
        BigDecimal newRate = BigDecimal.valueOf(0.87);

        cache.putRate(usd, eur, oldRate);
        cache.putRate(usd, eur, newRate);

        Optional<CachedRate> result = cache.getRate(usd, eur);

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualTo(newRate);
    }

    @Test
    void getRate_WithMultipleCurrencyPairs_ShouldReturnCorrectRates() {
        Currency gbp = Currency.getInstance("GBP");
        Currency jpy = Currency.getInstance("JPY");

        cache.putRate(usd, eur, BigDecimal.valueOf(0.85));
        cache.putRate(usd, gbp, BigDecimal.valueOf(0.75));
        cache.putRate(usd, jpy, BigDecimal.valueOf(110.0));

        assertThat(cache.getRate(usd, eur).get().rate()).isEqualTo(BigDecimal.valueOf(0.85));
        assertThat(cache.getRate(usd, gbp).get().rate()).isEqualTo(BigDecimal.valueOf(0.75));
        assertThat(cache.getRate(usd, jpy).get().rate()).isEqualTo(BigDecimal.valueOf(110.0));
    }

    @Test
    void cache_WithNegativeTtl_ShouldExpireImmediately() {
        ReflectionTestUtils.setField(cache, "cacheTtlSeconds", -1L);

        cache.putRate(usd, eur, BigDecimal.valueOf(0.85));

        assertThat(cache.getRate(usd, eur)).isEmpty();
    }

    @Test
    void getRate_WithExpiredDirectRate_ShouldRemoveAndReturnEmpty() {
        cache.putRate(usd, eur, BigDecimal.valueOf(0.85));
        cache.putRate(eur, usd, BigDecimal.valueOf(1.18));

        ReflectionTestUtils.setField(cache, "cacheTtlSeconds", -1L);

        Optional<CachedRate> result = cache.getRate(usd, eur);

        assertThat(result).isEmpty();
    }

    @Test
    void putRate_WithMultipleCalls_ShouldOverwritePrevious() {
        cache.putRate(usd, eur, BigDecimal.valueOf(0.85));
        cache.putRate(usd, eur, BigDecimal.valueOf(0.87));

        Optional<CachedRate> result = cache.getRate(usd, eur);

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualByComparingTo("0.87");
    }
}
