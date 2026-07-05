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

/**
 * Authenticates {@code /api/v1/admin/**} requests via the {@code X-API-Key}
 * header. Rate limiting for these paths lives in
 * {@link AdminEndpointRateLimitFilter}, which runs earlier in the chain so
 * failed authentication attempts are also throttled.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

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

    private void sendUnauthorized(final HttpServletResponse response, final String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\": \"%s\", \"message\": \"Please provide valid X-API-Key header\"}",
                message
        ));
    }
}
