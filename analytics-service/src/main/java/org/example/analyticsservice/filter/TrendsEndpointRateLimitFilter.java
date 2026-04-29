package org.example.analyticsservice.filter;

import com.example.cerps.common.filter.AbstractRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrendsEndpointRateLimitFilter extends AbstractRateLimitFilter {

    private static final String TRENDS_PATH = "/api/v1/analytics/trends";
    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    @Override
    protected Integer resolveLimit(final HttpServletRequest request) {
        return TRENDS_PATH.equals(request.getRequestURI()) ? MAX_REQUESTS_PER_MINUTE : null;
    }
}
