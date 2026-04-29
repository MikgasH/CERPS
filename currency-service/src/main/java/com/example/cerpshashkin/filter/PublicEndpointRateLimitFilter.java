package com.example.cerpshashkin.filter;

import com.example.cerps.common.filter.AbstractRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PublicEndpointRateLimitFilter extends AbstractRateLimitFilter {

    private static final Map<String, Integer> ENDPOINT_LIMITS = Map.of(
            "/api/v1/currencies", 100,
            "/api/v1/rates/current", 60,
            "/api/v1/currencies/convert", 60,
            "/api/v1/ai/bank-commission", 10
    );

    @Override
    protected Integer resolveLimit(final HttpServletRequest request) {
        return ENDPOINT_LIMITS.get(request.getRequestURI());
    }

    @Override
    protected String resolveBucketKey(final HttpServletRequest request, final String clientIp) {
        return clientIp + ":" + request.getRequestURI();
    }
}
