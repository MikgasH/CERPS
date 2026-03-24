package org.example.analyticsservice.service.cache;

import com.example.cerps.common.dto.TrendsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public final class TrendsCache {

    private static final String KEY_SEPARATOR = "_";
    private static final int MAX_CACHE_SIZE = 1000;

    private final Map<String, CachedTrends> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, CachedTrends> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    @Value("${cache.trends.ttl:28800}")
    private long cacheTtlSeconds; // Default matches CerpsConstants.CACHE_TTL_SECONDS

    public Optional<TrendsResponse> get(final String from, final String to, final String period) {
        final String key = createKey(from, to, period);

        return Optional.ofNullable(cache.get(key))
                .flatMap(cached -> {
                    if (isExpired(cached)) {
                        cache.remove(key);
                        log.debug("Removed expired trends cache for {}", key);
                        return Optional.empty();
                    }
                    log.debug("Cache hit for trends {}", key);
                    return Optional.of(cached.response());
                });
    }

    public void put(final String from, final String to, final String period, final TrendsResponse response) {
        final String key = createKey(from, to, period);
        cache.put(key, new CachedTrends(response, Instant.now()));
        log.debug("Cached trends for {}", key);
    }

    private String createKey(final String from, final String to, final String period) {
        return from + KEY_SEPARATOR + to + KEY_SEPARATOR + period.toUpperCase();
    }

    private boolean isExpired(final CachedTrends cached) {
        return cached.cachedAt().plusSeconds(cacheTtlSeconds).isBefore(Instant.now());
    }
}
