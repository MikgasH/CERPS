package org.example.analyticsservice.filter;

import com.example.cerps.common.filter.AbstractRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rate-limits {@code /api/v1/admin/**} per client IP so the controller-level
 * X-API-Key check (see AdminController) cannot be brute-forced unthrottled.
 * Mirrors currency-service's AdminEndpointRateLimitFilter.
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
