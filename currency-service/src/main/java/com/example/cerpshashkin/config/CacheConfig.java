package com.example.cerpshashkin.config;

import com.example.cerpshashkin.dto.HistoricalRatesResponse;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final String SUPPORTED_CURRENCIES_CACHE = "supportedCurrencies";
    private static final String DECRYPTED_API_KEYS_CACHE = "decryptedApiKeys";
    private static final String CURRENT_RATES_CACHE = "currentRates";
    private static final String HISTORICAL_RATES_CACHE = "historicalRates";

    // Keyed by base + date — an unbounded key space, so the cache must be
    // bounded (unlike the small fixed-key ConcurrentMapCaches above).
    private static final long HISTORICAL_RATES_MAX_SIZE = 1_000;
    private static final Duration HISTORICAL_RATES_TTL = Duration.ofHours(1);
    // Past-date snapshots are immutable once the ECB fixing has happened, so
    // a popular old date must not re-fetch from Frankfurter every hour.
    private static final Duration HISTORICAL_RATES_PAST_DATE_TTL = Duration.ofHours(24);
    // A partial snapshot (the Frankfurter gap-fill failed or covered only
    // some currencies) must not be served for the full TTL after the
    // provider recovers; the short TTL retries soon while still shielding
    // Frankfurter from a retry per request.
    private static final Duration HISTORICAL_RATES_PARTIAL_TTL = Duration.ofMinutes(5);

    @Bean
    public CacheManager cacheManager() {
        final SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new ConcurrentMapCache(DECRYPTED_API_KEYS_CACHE),
                new ConcurrentMapCache(SUPPORTED_CURRENCIES_CACHE),
                new ConcurrentMapCache(CURRENT_RATES_CACHE),
                new CaffeineCache(HISTORICAL_RATES_CACHE, Caffeine.newBuilder()
                        .maximumSize(HISTORICAL_RATES_MAX_SIZE)
                        .expireAfter(historicalRatesExpiry())
                        .build())
        ));
        return manager;
    }

    /**
     * Variable expiration for the historical-rates cache: a partial snapshot
     * expires within minutes so it heals once the gap-fill provider recovers,
     * a same-day snapshot can still change until the ECB fixing and keeps the
     * hourly TTL, and a complete past-date snapshot cannot change and stays
     * cached for a day (bounded by {@code maximumSize} LRU either way).
     */
    private static Expiry<Object, Object> historicalRatesExpiry() {
        return new Expiry<>() {
            @Override
            public long expireAfterCreate(final Object key, final Object value, final long currentTime) {
                return ttlFor(value);
            }

            @Override
            public long expireAfterUpdate(final Object key, final Object value,
                                          final long currentTime, final long currentDuration) {
                return ttlFor(value);
            }

            @Override
            public long expireAfterRead(final Object key, final Object value,
                                        final long currentTime, final long currentDuration) {
                return currentDuration;
            }

            private long ttlFor(final Object value) {
                if (value instanceof HistoricalRatesResponse response) {
                    if (!response.complete()) {
                        return HISTORICAL_RATES_PARTIAL_TTL.toNanos();
                    }
                    if (response.date().isBefore(LocalDate.now())) {
                        return HISTORICAL_RATES_PAST_DATE_TTL.toNanos();
                    }
                }
                return HISTORICAL_RATES_TTL.toNanos();
            }
        };
    }
}
