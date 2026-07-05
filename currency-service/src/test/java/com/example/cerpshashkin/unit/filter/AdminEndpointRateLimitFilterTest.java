package com.example.cerpshashkin.unit.filter;

import com.example.cerpshashkin.filter.AdminEndpointRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

class AdminEndpointRateLimitFilterTest {

    private final AdminEndpointRateLimitFilter filter = new AdminEndpointRateLimitFilter();

    @Test
    void resolveLimit_ShouldReturnLimit_WhenAdminPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/provider-keys");
        request.setRequestURI("/api/v1/admin/provider-keys");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isEqualTo(10);
    }

    @Test
    void resolveLimit_ShouldReturnNull_WhenNonAdminPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/currencies");
        request.setRequestURI("/api/v1/currencies");

        Integer limit = invokeMethod(filter, "resolveLimit", request);

        assertThat(limit).isNull();
    }
}
