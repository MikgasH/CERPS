package org.example.analyticsservice.unit.service.cache;

import com.example.cerps.common.dto.TrendsResponse;
import org.example.analyticsservice.service.cache.TrendsCache;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrendsCacheTest {

    private final TrendsCache cache = cacheWithTtls(3600L, 3600L);

    @Test
    void get_ShouldReturnEmpty_WhenNothingCached() {
        assertThat(cache.get("USD", "EUR", "7D")).isEmpty();
    }

    @Test
    void get_ShouldReturnResponse_WhenStoredInLongTtlTier() {
        TrendsResponse response = response("7D");

        cache.put("USD", "EUR", "7D", response);

        assertThat(cache.get("USD", "EUR", "7D")).contains(response);
    }

    @Test
    void get_ShouldFallThroughToShortTtlTier_WhenNotInLongTtlTier() {
        TrendsResponse response = response("7D");

        cache.putShort("USD", "EUR", "7D", response);

        // Fallback-sourced entries live only in the short tier; get() must
        // consult it after missing the long tier, not report a miss.
        assertThat(cache.get("USD", "EUR", "7D")).contains(response);
    }

    @Test
    void get_ShouldHitSameEntry_WhenPeriodCaseDiffers() {
        TrendsResponse response = response("7D");

        cache.put("USD", "EUR", "7d", response);

        assertThat(cache.get("USD", "EUR", "7D")).contains(response);
    }

    @Test
    void get_ShouldMissAcrossTiers_WhenKeysDiffer() {
        cache.put("USD", "EUR", "7D", response("7D"));
        cache.putShort("USD", "EUR", "30D", response("30D"));

        assertThat(cache.get("USD", "GBP", "7D")).isEmpty();
        assertThat(cache.get("USD", "EUR", "90D")).isEmpty();
    }

    @Test
    void get_ShouldNotServeShortTierEntry_WhenShortTtlElapsed() {
        // Short TTL of zero expires short-tier entries immediately while the
        // long tier keeps its own TTL - the tiers must age independently.
        TrendsCache expiringCache = cacheWithTtls(3600L, 0L);
        expiringCache.putShort("USD", "EUR", "7D", response("7D"));
        expiringCache.put("USD", "GBP", "7D", response("7D"));

        assertThat(expiringCache.get("USD", "EUR", "7D")).isEmpty();
        assertThat(expiringCache.get("USD", "GBP", "7D")).isPresent();
    }

    @Test
    void invalidateAll_ShouldClearBothTiers() {
        cache.put("USD", "EUR", "7D", response("7D"));
        cache.putShort("USD", "EUR", "30D", response("30D"));

        cache.invalidateAll();

        assertThat(cache.get("USD", "EUR", "7D")).isEmpty();
        assertThat(cache.get("USD", "EUR", "30D")).isEmpty();
    }

    @Test
    void put_ShouldOverwriteExistingEntry_WhenSameKey() {
        TrendsResponse stale = response("7D");
        TrendsResponse fresh = response("7D");

        cache.put("USD", "EUR", "7D", stale);
        cache.put("USD", "EUR", "7D", fresh);

        Optional<TrendsResponse> result = cache.get("USD", "EUR", "7D");
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(fresh);
    }

    private static TrendsCache cacheWithTtls(final long longTtlSeconds, final long shortTtlSeconds) {
        TrendsCache cache = new TrendsCache();
        ReflectionTestUtils.setField(cache, "cacheTtlSeconds", longTtlSeconds);
        ReflectionTestUtils.setField(cache, "shortTtlSeconds", shortTtlSeconds);
        ReflectionTestUtils.invokeMethod(cache, "init");
        return cache;
    }

    private static TrendsResponse response(final String period) {
        Instant now = Instant.parse("2026-07-01T00:00:00Z");
        return TrendsResponse.success("USD", "EUR", period, List.of(),
                new BigDecimal("1.10"), new BigDecimal("1.18"), new BigDecimal("7.27"),
                now.minusSeconds(604_800L), now, 7);
    }
}
