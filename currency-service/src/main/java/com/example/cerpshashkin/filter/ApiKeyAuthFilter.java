package com.example.cerpshashkin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long STALE_ENTRY_MILLIS = 120_000L;

    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestPath = request.getRequestURI();

        if (!requestPath.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp = request.getRemoteAddr();

        if (isRateLimited(clientIp)) {
            log.warn("AUDIT: Rate limit exceeded for admin endpoint. ip={}, path={}, timestamp={}",
                    clientIp, requestPath, Instant.now());
            sendTooManyRequests(response);
            return;
        }

        final String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AUDIT: Failed authentication attempt - missing API key. ip={}, path={}, timestamp={}",
                    clientIp, requestPath, Instant.now());
            sendUnauthorized(response, "Missing API key");
            return;
        }

        if (!constantTimeEquals(apiKey, adminApiKey)) {
            log.warn("AUDIT: Failed authentication attempt - invalid API key. ip={}, path={}, timestamp={}",
                    clientIp, requestPath, Instant.now());
            sendUnauthorized(response, "Invalid API key");
            return;
        }

        log.debug("Valid API key for admin endpoint: {}", requestPath);

        final var authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean constantTimeEquals(final String provided, final String expected) {
        final byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        final byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedBytes, expectedBytes);
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

    private void sendUnauthorized(final HttpServletResponse response, final String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\": \"%s\", \"message\": \"Please provide valid X-API-Key header\"}",
                message
        ));
    }

    private void sendTooManyRequests(final HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Maximum 10 requests per minute.\"}"
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
