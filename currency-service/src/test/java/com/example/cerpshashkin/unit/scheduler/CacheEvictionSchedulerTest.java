package com.example.cerpshashkin.unit.scheduler;

import com.example.cerpshashkin.scheduler.CacheEvictionScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheEvictionSchedulerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private CacheEvictionScheduler scheduler;

    @Test
    void evictSupportedCurrenciesCache_ShouldClearCache_WhenCacheExists() {
        when(cacheManager.getCache("supportedCurrencies")).thenReturn(cache);

        scheduler.evictSupportedCurrenciesCache();

        verify(cache).clear();
    }

    @Test
    void evictSupportedCurrenciesCache_ShouldNotThrow_WhenCacheMissing() {
        when(cacheManager.getCache("supportedCurrencies")).thenReturn(null);

        assertThatCode(() -> scheduler.evictSupportedCurrenciesCache())
                .doesNotThrowAnyException();
    }
}
