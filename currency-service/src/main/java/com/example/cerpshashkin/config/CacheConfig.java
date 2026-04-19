package com.example.cerpshashkin.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final String SUPPORTED_CURRENCIES_CACHE = "supportedCurrencies";
    private static final String DECRYPTED_API_KEYS_CACHE = "decryptedApiKeys";
    private static final String CURRENT_RATES_CACHE = "currentRates";

    private CacheManager cacheManager;

    @Bean
    public CacheManager cacheManager() {
        final SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new ConcurrentMapCache(DECRYPTED_API_KEYS_CACHE),
                new ConcurrentMapCache(SUPPORTED_CURRENCIES_CACHE),
                new ConcurrentMapCache(CURRENT_RATES_CACHE)
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
