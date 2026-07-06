package com.example.cerpshashkin.unit.filter;

import com.example.cerpshashkin.filter.PublicEndpointRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

class PublicEndpointRateLimitFilterTest {

    private final PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter();

    @ParameterizedTest
    @CsvSource({
            "/api/v1/currencies, 100",
            "/api/v1/rates/current, 60",
            "/api/v1/rates/historical, 60",
            "/api/v1/rates/history, 60",
            "/api/v1/currencies/convert, 60",
            "/api/v1/ai/bank-commission, 10"
    })
    void resolveLimit_ShouldReturnEndpointLimit_WhenPublicEndpoint(final String path, final int expectedLimit) {
        Integer limit = invokeMethod(filter, "resolveLimit", requestFor(path));

        assertThat(limit).isEqualTo(expectedLimit);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator/health",
            "/api/v1/admin/currencies",
            // Matching is exact: a sub-path of a limited endpoint is not limited.
            "/api/v1/rates/history/extra"
    })
    void resolveLimit_ShouldReturnNull_WhenPathNotInLimitMap(final String path) {
        Integer limit = invokeMethod(filter, "resolveLimit", requestFor(path));

        assertThat(limit).isNull();
    }

    @Test
    void resolveBucketKey_ShouldIncludeEndpoint_SoLimitsApplyPerEndpoint() {
        String key = invokeMethod(filter, "resolveBucketKey", requestFor("/api/v1/currencies"), "203.0.113.9");

        assertThat(key).isEqualTo("203.0.113.9:/api/v1/currencies");
    }

    @Test
    void doFilter_ShouldNotShareBucketAcrossEndpoints_WhenSameClientIp() throws Exception {
        // Exhaust the strictest endpoint (10/min) from one IP...
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(requestFor("/api/v1/ai/bank-commission"), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(requestFor("/api/v1/ai/bank-commission"), rejected, new MockFilterChain());
        assertThat(rejected.getStatus()).isEqualTo(429);

        // ...and the same IP must still be able to call other endpoints.
        MockHttpServletResponse otherEndpoint = new MockHttpServletResponse();
        filter.doFilter(requestFor("/api/v1/currencies"), otherEndpoint, new MockFilterChain());
        assertThat(otherEndpoint.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest requestFor(final String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
