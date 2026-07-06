package org.example.analyticsservice.unit.filter;

import org.example.analyticsservice.filter.TrendsEndpointRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

class TrendsEndpointRateLimitFilterTest {

    private final TrendsEndpointRateLimitFilter filter = new TrendsEndpointRateLimitFilter();

    @Test
    void resolveLimit_ShouldReturnLimit_WhenTrendsPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/analytics/trends");
        request.setRequestURI("/api/v1/analytics/trends");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isEqualTo(30);
    }

    @Test
    void resolveLimit_ShouldReturnNull_WhenOtherPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRequestURI("/actuator/health");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isNull();
    }

    @Test
    void resolveLimit_ShouldReturnNull_WhenTrendsSubPath() {
        // Matching is exact, not prefix-based.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/analytics/trends/extra");
        request.setRequestURI("/api/v1/analytics/trends/extra");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isNull();
    }
}
