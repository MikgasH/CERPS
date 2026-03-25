package com.example.cerpshashkin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final long STALE_ENTRY_MILLIS = 120_000L;

    private static final Map<String, Integer> ENDPOINT_LIMITS = Map.of(
            "/api/v1/currencies", 100,
            "/api/v1/rates/current", 60,
            "/api/v1/currencies/convert", 60
    );

    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getRequestURI();
        final Integer limit = resolveLimit(path);

        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp = getClientIp(request);
        final String key = clientIp + ":" + path;

        if (isRateLimited(key, limit)) {
            log.warn("AUDIT: Public rate limit exceeded. ip={}, path={}, limit={}/min, timestamp={}",
                    clientIp, path, limit, Instant.now());
            sendTooManyRequests(response, limit);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Integer resolveLimit(final String path) {
        for (final Map.Entry<String, Integer> entry : ENDPOINT_LIMITS.entrySet()) {
            if (path.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isRateLimited(final String key, final int maxRequests) {
        final long now = System.currentTimeMillis();
        rateLimitMap.compute(key, (k, entry) -> {
            if (entry == null || now - entry.windowStart > WINDOW_MILLIS) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            entry.counter.incrementAndGet();
            return entry;
        });

        final RateLimitEntry entry = rateLimitMap.get(key);
        return entry != null && entry.counter.get() > maxRequests;
    }

    private String getClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(final HttpServletResponse response, final int limit) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Maximum %d requests per minute.\"}",
                limit
        ));
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupStaleEntries() {
        final long now = System.currentTimeMillis();
        rateLimitMap.entrySet().removeIf(
                entry -> now - entry.getValue().windowStart > STALE_ENTRY_MILLIS
        );
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger counter;

        RateLimitEntry(final long windowStart, final AtomicInteger counter) {
            this.windowStart = windowStart;
            this.counter = counter;
        }
    }
}
