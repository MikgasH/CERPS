package com.example.cerpshashkin.filter;

import com.example.cerps.common.filter.AbstractRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rate-limits all {@code /api/v1/admin/**} traffic per client IP before
 * {@link ApiKeyAuthFilter} authenticates it, throttling API-key guessing.
 * Client IPs come from {@link AbstractRateLimitFilter}'s trusted-proxy-aware
 * extraction, so clients behind Railway's proxy get separate buckets.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminEndpointRateLimitFilter extends AbstractRateLimitFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    @Override
    protected Integer resolveLimit(final HttpServletRequest request) {
        return request.getRequestURI().startsWith(ADMIN_PATH_PREFIX) ? MAX_REQUESTS_PER_MINUTE : null;
    }
}
