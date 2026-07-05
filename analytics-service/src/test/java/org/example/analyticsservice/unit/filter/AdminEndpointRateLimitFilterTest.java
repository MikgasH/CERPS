package org.example.analyticsservice.unit.filter;

import org.example.analyticsservice.filter.AdminEndpointRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

class AdminEndpointRateLimitFilterTest {

    private final AdminEndpointRateLimitFilter filter = new AdminEndpointRateLimitFilter();

    @Test
    void resolveLimit_ShouldReturnLimit_WhenAdminPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/cache/refresh");
        request.setRequestURI("/api/v1/admin/cache/refresh");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isEqualTo(10);
    }

    @Test
    void resolveLimit_ShouldReturnNull_WhenNonAdminPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/analytics/trends");
        request.setRequestURI("/api/v1/analytics/trends");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isNull();
    }
}
