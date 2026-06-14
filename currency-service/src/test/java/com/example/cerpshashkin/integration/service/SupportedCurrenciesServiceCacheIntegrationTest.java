package com.example.cerpshashkin.integration.service;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.SupportedCurrenciesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the supported-currency cache on the real application context: both
 * {@code getSupportedCurrencyCodes()} and {@code getSupportedCurrencyCodesAsSet()}
 * delegate to the cached {@code CurrencyCodeCacheService} bean, so the repository
 * is queried at most once across repeated calls (it previously ran
 * {@code findAll()} on every Set-view invocation).
 */
@SpringBootTest
@ActiveProfiles("test")
class SupportedCurrenciesServiceCacheIntegrationTest {

    private static final String CACHE_NAME = "supportedCurrencies";

    @MockitoBean
    private SupportedCurrencyRepository repository;

    @Autowired
    private SupportedCurrenciesService service;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetCacheAndStub() {
        cacheManager.getCache(CACHE_NAME).clear();
        when(repository.findAll()).thenReturn(List.of(
                entity("USD"), entity("EUR"), entity("GBP")));
    }

    @Test
    void getSupportedCurrencyCodesAsSet_CalledRepeatedly_HitsRepositoryOnce() {
        final Set<String> first = service.getSupportedCurrencyCodesAsSet();
        final Set<String> second = service.getSupportedCurrencyCodesAsSet();
        service.getSupportedCurrencyCodesAsSet();

        assertThat(first).containsExactlyInAnyOrder("USD", "EUR", "GBP");
        assertThat(second).isEqualTo(first);
        // The whole point of the fix: no findAll() on every call.
        verify(repository, times(1)).findAll();
    }

    @Test
    void getSupportedCurrencyCodes_AndAsSet_ShareTheSameCacheEntry() {
        service.getSupportedCurrencyCodes();
        service.getSupportedCurrencyCodesAsSet();

        verify(repository, times(1)).findAll();
    }

    private static SupportedCurrencyEntity entity(final String code) {
        return SupportedCurrencyEntity.builder().currencyCode(code).build();
    }
}
