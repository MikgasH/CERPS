package org.example.analyticsservice.filter;

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
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final String TRENDS_PATH = "/api/v1/analytics/trends";
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long STALE_ENTRY_MILLIS = 120_000L;

    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getRequestURI();

        if (!path.equals(TRENDS_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp = getClientIp(request);

        if (isRateLimited(clientIp)) {
            log.warn("AUDIT: Rate limit exceeded for trends endpoint. ip={}, timestamp={}",
                    clientIp, Instant.now());
            sendTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(final String clientIp) {
        final long now = System.currentTimeMillis();
        rateLimitMap.compute(clientIp, (key, entry) -> {
            if (entry == null || now - entry.windowStart > WINDOW_MILLIS) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            entry.counter.incrementAndGet();
            return entry;
        });

        final RateLimitEntry entry = rateLimitMap.get(clientIp);
        return entry != null && entry.counter.get() > MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(final HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\": \"Too many requests\","
                        + " \"message\": \"Rate limit exceeded. Maximum 30 requests per minute.\"}"
        );
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
