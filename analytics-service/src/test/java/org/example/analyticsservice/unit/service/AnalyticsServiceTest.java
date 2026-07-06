package org.example.analyticsservice.unit.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import org.example.analyticsservice.service.AnalyticsService;
import org.example.analyticsservice.service.TrendsService;
import org.example.analyticsservice.service.cache.TrendsCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TrendsService trendsService;

    @Mock
    private TrendsCache trendsCache;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void calculateTrends_ShouldReturnCachedResponse_WhenCacheHit() {
        TrendsResponse cached = response();
        when(trendsCache.get("USD", "EUR", "7D")).thenReturn(Optional.of(cached));

        TrendsResponse result = analyticsService.calculateTrends(new TrendsRequest("USD", "EUR", "7D"));

        assertThat(result).isSameAs(cached);
        verifyNoInteractions(trendsService);
        verify(trendsCache, never()).put(anyString(), anyString(), anyString(), any());
        verify(trendsCache, never()).putShort(anyString(), anyString(), anyString(), any());
    }

    @Test
    void calculateTrends_ShouldStoreInLongTtlTier_WhenServedFromPrimarySource() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        TrendsResponse computed = response();
        when(trendsCache.get("USD", "EUR", "7D")).thenReturn(Optional.empty());
        when(trendsService.calculateTrends(request))
                .thenReturn(new TrendsService.TrendsResult(computed, false));

        TrendsResponse result = analyticsService.calculateTrends(request);

        assertThat(result).isSameAs(computed);
        verify(trendsCache).put("USD", "EUR", "7D", computed);
        verify(trendsCache, never()).putShort(anyString(), anyString(), anyString(), any());
    }

    @Test
    void calculateTrends_ShouldStoreInShortTtlTier_WhenServedFromFallback() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        TrendsResponse computed = response();
        when(trendsCache.get("USD", "EUR", "7D")).thenReturn(Optional.empty());
        when(trendsService.calculateTrends(request))
                .thenReturn(new TrendsService.TrendsResult(computed, true));

        TrendsResponse result = analyticsService.calculateTrends(request);

        // Fallback data is lower fidelity - caching it for the full TTL would
        // pin degraded responses long after the primary source recovers.
        assertThat(result).isSameAs(computed);
        verify(trendsCache).putShort("USD", "EUR", "7D", computed);
        verify(trendsCache, never()).put(anyString(), anyString(), anyString(), any());
    }

    @Test
    void calculateTrends_ShouldNormalizeCacheKey_WhenInputPaddedLowercase() {
        TrendsRequest request = new TrendsRequest(" usd", "eur ", "7d");
        when(trendsCache.get("USD", "EUR", "7D")).thenReturn(Optional.empty());
        when(trendsService.calculateTrends(request))
                .thenReturn(new TrendsService.TrendsResult(response(), false));

        analyticsService.calculateTrends(request);

        // Padded input must map to the canonical key, not create a duplicate entry.
        verify(trendsCache).get("USD", "EUR", "7D");
        verify(trendsCache).put(eq("USD"), eq("EUR"), eq("7D"), any(TrendsResponse.class));
    }

    @Test
    void invalidateCache_ShouldDelegateToTrendsCache() {
        analyticsService.invalidateCache();

        verify(trendsCache).invalidateAll();
    }

    private TrendsResponse response() {
        Instant now = Instant.parse("2026-07-01T00:00:00Z");
        return TrendsResponse.success("USD", "EUR", "7D", List.of(),
                new BigDecimal("1.10"), new BigDecimal("1.18"), new BigDecimal("7.27"),
                now.minusSeconds(604_800L), now, 7);
    }
}
