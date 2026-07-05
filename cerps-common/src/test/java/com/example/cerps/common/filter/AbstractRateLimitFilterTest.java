package com.example.cerps.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRateLimitFilterTest {

    private static final int LIMIT = 2;

    private final AbstractRateLimitFilter filter = new AbstractRateLimitFilter() {
        @Override
        protected Integer resolveLimit(final HttpServletRequest request) {
            return LIMIT;
        }
    };

    @Test
    void extractClientIp_ShouldReturnRemoteAddr_WhenNoForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(filter.extractClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void extractClientIp_ShouldReturnRightMostValue_WhenForwardedHeaderHasMultipleHops() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        // Left-hand values are client-supplied; only the right-most one was
        // appended by the trusted edge proxy.
        request.addHeader("X-Forwarded-For", "6.6.6.6, 7.7.7.7, 203.0.113.9");

        assertThat(filter.extractClientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void extractClientIp_ShouldNotTrustSpoofedFirstValue_WhenSingleForwardedValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(filter.extractClientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void extractClientIp_ShouldFallBackToRemoteAddr_WhenForwardedHeaderBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "  ");

        assertThat(filter.extractClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void doFilter_ShouldReturn429_WhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < LIMIT; i++) {
            MockHttpServletResponse okResponse = new MockHttpServletResponse();
            filter.doFilter(request, okResponse, new MockFilterChain());
            assertThat(okResponse.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        filter.doFilter(request, limitedResponse, new MockFilterChain());

        assertThat(limitedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_ShouldUseSeparateBuckets_WhenClientsShareProxyButDifferInForwardedFor() throws Exception {
        for (int i = 0; i <= LIMIT; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
