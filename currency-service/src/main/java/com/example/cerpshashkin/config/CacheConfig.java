package com.example.cerpshashkin.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
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

    private CacheManager cacheManager;

    @Bean
    public CacheManager cacheManager() {
        final SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new ConcurrentMapCache(DECRYPTED_API_KEYS_CACHE),
                new ConcurrentMapCache(SUPPORTED_CURRENCIES_CACHE),
                new ConcurrentMapCache(CURRENT_RATES_CACHE),
                new CaffeineCache(HISTORICAL_RATES_CACHE, Caffeine.newBuilder()
                        .maximumSize(HISTORICAL_RATES_MAX_SIZE)
                        .expireAfterWrite(HISTORICAL_RATES_TTL)
                        .build())
        ));
        this.cacheManager = manager;
        return manager;
    }

    @Scheduled(fixedRate = 900_000) // 15 minutes
    public void evictSupportedCurrenciesCache() {
        if (cacheManager != null) {
            final var cache = cacheManager.getCache(SUPPORTED_CURRENCIES_CACHE);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
