package com.example.cerpshashkin.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic eviction for the {@code supportedCurrencies} ConcurrentMapCache,
 * which has no built-in TTL. Lives here (not in CacheConfig) so it is gated
 * like every other scheduler and stays off in tests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true")
public class CacheEvictionScheduler {

    private static final String LOG_EVICTED = "Evicted supportedCurrencies cache";
    private static final String SUPPORTED_CURRENCIES_CACHE = "supportedCurrencies";

    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 900_000) // 15 minutes
    public void evictSupportedCurrenciesCache() {
        final Cache cache = cacheManager.getCache(SUPPORTED_CURRENCIES_CACHE);
        if (cache != null) {
            cache.clear();
            log.debug(LOG_EVICTED);
        }
    }
}
