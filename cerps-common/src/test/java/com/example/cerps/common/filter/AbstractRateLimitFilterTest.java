package com.example.cerps.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRateLimitFilterTest {

    private static final int LIMIT = 2;
    private static final String CLIENT_IP = "10.0.0.1";
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long STALE_ENTRY_MILLIS = 120_000L;

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

    @Test
    void doFilter_ShouldAllowExactlyLimitRequests_AndRejectTheNextWithLimitMessage() throws Exception {
        // Boundary: the request that lands exactly ON the limit must pass;
        // only the first one OVER it may be rejected.
        assertThat(performRequest().getStatus()).isEqualTo(200);
        assertThat(performRequest().getStatus()).isEqualTo(200);

        MockHttpServletResponse rejected = performRequest();

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getContentType()).isEqualTo("application/json");
        assertThat(rejected.getContentAsString())
                .contains("Maximum " + LIMIT + " requests per minute");
    }

    @Test
    void doFilter_ShouldNotLimit_WhenResolveLimitReturnsNull() throws Exception {
        AbstractRateLimitFilter unlimitedFilter = new AbstractRateLimitFilter() {
            @Override
            protected Integer resolveLimit(final HttpServletRequest request) {
                return null;
            }
        };

        for (int i = 0; i < LIMIT * 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            unlimitedFilter.doFilter(newRequest(), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void doFilter_ShouldAllowRequestsAgain_WhenWindowExpired() throws Exception {
        exhaustLimit();
        assertThat(performRequest().getStatus()).isEqualTo(429);

        // Age the bucket past the 60s window; the next request must open a
        // fresh window instead of counting against the exhausted one.
        backdateBucket(CLIENT_IP, WINDOW_MILLIS + 1_000L);

        assertThat(performRequest().getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_ShouldKeepRejecting_WhenWindowNotYetExpired() throws Exception {
        exhaustLimit();

        // Just short of the window boundary: still the same window, still blocked.
        backdateBucket(CLIENT_IP, WINDOW_MILLIS - 1_000L);

        assertThat(performRequest().getStatus()).isEqualTo(429);
    }

    @Test
    void cleanupStaleEntries_ShouldEvictBucket_WhenOlderThanStaleThreshold() throws Exception {
        exhaustLimit();
        backdateBucket(CLIENT_IP, STALE_ENTRY_MILLIS + 1_000L);

        filter.cleanupStaleEntries();

        assertThat(rateLimitMap()).isEmpty();
    }

    @Test
    void cleanupStaleEntries_ShouldRetainBucket_WhenStillWithinStaleThreshold() throws Exception {
        exhaustLimit();

        filter.cleanupStaleEntries();

        // The scheduled sweep must never wipe live windows - the exhausted
        // bucket keeps rejecting until its window actually expires.
        assertThat(rateLimitMap()).containsKey(CLIENT_IP);
        assertThat(performRequest().getStatus()).isEqualTo(429);
    }

    private void exhaustLimit() throws Exception {
        for (int i = 0; i < LIMIT + 1; i++) {
            performRequest();
        }
    }

    private MockHttpServletResponse performRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(newRequest(), response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest newRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rateLimitMap() {
        return (Map<String, Object>) ReflectionTestUtils.getField(filter, "rateLimitMap");
    }

    /**
     * Rewinds a bucket's window start by replacing the private map entry -
     * the filter reads {@code System.currentTimeMillis()} directly, so time
     * itself cannot be faked without touching production code.
     */
    private void backdateBucket(final String key, final long ageMillis) throws Exception {
        Map<String, Object> map = rateLimitMap();
        Object entry = map.get(key);
        Constructor<?> constructor = entry.getClass()
                .getDeclaredConstructor(long.class, AtomicInteger.class);
        constructor.setAccessible(true);
        map.put(key, constructor.newInstance(
                System.currentTimeMillis() - ageMillis, new AtomicInteger(LIMIT + 1)));
    }
}
