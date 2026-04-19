package org.example.analyticsservice.service.cache;

import com.example.cerps.common.dto.TrendsResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public final class TrendsCache {

    private static final String KEY_SEPARATOR = "_";
    private static final int MAX_CACHE_SIZE = 1000;

    @Value("${cache.trends.ttl:28800}")
    private long cacheTtlSeconds;

    @Value("${cache.trends.short-ttl:300}")
    private long shortTtlSeconds;

    private Cache<String, TrendsResponse> cache;
    private Cache<String, TrendsResponse> shortCache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .build();
        shortCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(Duration.ofSeconds(shortTtlSeconds))
                .build();
    }

    public Optional<TrendsResponse> get(final String from, final String to, final String period) {
        final String key = createKey(from, to, period);
        TrendsResponse response = cache.getIfPresent(key);
        if (response == null) {
            response = shortCache.getIfPresent(key);
        }
        if (response != null) {
            log.debug("Cache hit for trends {}", key);
        }
        return Optional.ofNullable(response);
    }

    public void put(final String from, final String to, final String period, final TrendsResponse response) {
        final String key = createKey(from, to, period);
        cache.put(key, response);
        log.debug("Cached trends for {}", key);
    }

    public void putShort(final String from, final String to, final String period, final TrendsResponse response) {
        final String key = createKey(from, to, period);
        shortCache.put(key, response);
        log.debug("Cached trends (short TTL) for {}", key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
        shortCache.invalidateAll();
        log.info("Trends cache invalidated");
    }

    private String createKey(final String from, final String to, final String period) {
        return from + KEY_SEPARATOR + to + KEY_SEPARATOR + period.toUpperCase();
    }
}
