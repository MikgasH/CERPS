package com.example.cerpshashkin.unit.config;

import com.example.cerpshashkin.config.CacheConfig;
import com.example.cerpshashkin.config.NativeImageConfig;
import com.example.cerpshashkin.dto.HistoricalRatesResponse;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration DAY = Duration.ofHours(24);
    private static final Duration PARTIAL = Duration.ofMinutes(5);

    private CaffeineCache historicalRatesCache;

    @BeforeEach
    void setUp() {
        final CacheManager manager = new CacheConfig().cacheManager();
        ((SimpleCacheManager) manager).afterPropertiesSet();
        historicalRatesCache = (CaffeineCache) manager.getCache("historicalRates");
    }

    @Test
    void historicalRatesCache_ShouldUseShortTtl_WhenSnapshotPartial() {
        // Partial past-date snapshot: the short TTL must win over the
        // past-date TTL so the entry retries soon after Frankfurter recovers.
        final Duration ttl = ttlOf("partial", response(LocalDate.now().minusDays(30), false));

        assertThat(ttl).isLessThanOrEqualTo(PARTIAL);
        assertThat(ttl).isGreaterThan(PARTIAL.minusMinutes(1));
    }

    @Test
    void historicalRatesCache_ShouldUseDayTtl_WhenPastDateSnapshotComplete() {
        final Duration ttl = ttlOf("past", response(LocalDate.now().minusDays(30), true));

        assertThat(ttl).isGreaterThan(HOUR);
        assertThat(ttl).isLessThanOrEqualTo(DAY);
    }

    @Test
    void historicalRatesCache_ShouldUseHourTtl_WhenSameDaySnapshotComplete() {
        final Duration ttl = ttlOf("today", response(LocalDate.now(), true));

        assertThat(ttl).isLessThanOrEqualTo(HOUR);
        assertThat(ttl).isGreaterThan(HOUR.minusMinutes(1));
    }

    @Test
    void historicalRatesCache_ShouldUseCaffeineClassesRegisteredForNativeImage() throws Exception {
        // Caffeine picks its internal cache/node classes reflectively from
        // the builder options (maximumSize + variable Expiry -> SSMSA/PSWMS).
        // NativeImageConfig registers exactly these names for GraalVM; if a
        // Caffeine upgrade or a CacheConfig builder change alters the chosen
        // classes, this test fails on the JVM instead of the native image
        // failing at startup with ClassNotFoundException.
        Object localCache = readField(historicalRatesCache.getNativeCache(), "cache");
        assertThat(localCache.getClass().getName())
                .isEqualTo(NativeImageConfig.CAFFEINE_CACHE_CLASS);

        Object nodeFactory = readField(localCache, "nodeFactory");
        assertThat(nodeFactory.getClass().getName())
                .isEqualTo(NativeImageConfig.CAFFEINE_NODE_CLASS);
    }

    private static Object readField(final Object target, final String fieldName) throws ReflectiveOperationException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException next) {
                continue;
            }
        }
        throw new NoSuchFieldException(fieldName + " not found on " + target.getClass());
    }

    private Duration ttlOf(final String key, final HistoricalRatesResponse value) {
        historicalRatesCache.put(key, value);
        @SuppressWarnings("unchecked")
        final Cache<Object, Object> nativeCache = (Cache<Object, Object>) historicalRatesCache.getNativeCache();
        return nativeCache.policy().expireVariably().orElseThrow()
                .getExpiresAfter(key).orElseThrow();
    }

    private static HistoricalRatesResponse response(final LocalDate date, final boolean complete) {
        return new HistoricalRatesResponse(
                "EUR", date, Map.of("USD", new BigDecimal("1.08")), "DATABASE", Instant.now(), complete);
    }
}
