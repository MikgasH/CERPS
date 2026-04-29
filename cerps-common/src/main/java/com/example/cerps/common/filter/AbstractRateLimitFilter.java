package com.example.cerps.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final long STALE_ENTRY_MILLIS = 120_000L;
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-minute request limit for this request, or {@code null} if it should not be rate-limited.
     */
    protected abstract Integer resolveLimit(HttpServletRequest request);

    /**
     * Returns the bucket key used to group requests for limiting. Default: client IP only.
     * Override to scope buckets more narrowly (e.g. per-endpoint).
     */
    protected String resolveBucketKey(final HttpServletRequest request, final String clientIp) {
        return clientIp;
    }

    @Override
    protected final void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final Integer limit = resolveLimit(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp = extractClientIp(request);
        final String key = resolveBucketKey(request, clientIp);

        if (isRateLimited(key, limit)) {
            log.warn("AUDIT: Public rate limit exceeded. ip={}, path={}, limit={}/min, timestamp={}",
                    clientIp, request.getRequestURI(), limit, Instant.now());
            sendTooManyRequests(response, limit);
            return;
        }

        filterChain.doFilter(request, response);
    }

    protected String extractClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

    private static final class RateLimitEntry {
        private final long windowStart;
        private final AtomicInteger counter;

        private RateLimitEntry(final long windowStart, final AtomicInteger counter) {
            this.windowStart = windowStart;
            this.counter = counter;
        }
    }
}
