package com.example.cerpshashkin.unit.filter;

import com.example.cerpshashkin.filter.ApiKeyAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyAuthFilterTest {

    private static final String VALID_KEY = "test-admin-api-key";
    private static final String ADMIN_PATH = "/api/v1/admin/currencies";

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "adminApiKey", VALID_KEY);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_ShouldPassThroughWithoutAuthentication_WhenNonAdminPath() throws Exception {
        MockHttpServletRequest request = adminRequest("/api/v1/currencies");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_ShouldReturn401_WhenApiKeyMissing() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing API key");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_ShouldReturn401_WhenApiKeyBlank() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing API key");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_ShouldReturn401_WhenApiKeyInvalid() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API key");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_ShouldReject_WhenApiKeyMatchesOnlyAsPrefix() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", VALID_KEY + "-suffix");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doFilter_ShouldGrantRoleAdminDuringChain_WhenApiKeyValid() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // The context is cleared in a finally block, so the authentication
        // must be observed from inside the chain, not after doFilter returns.
        AtomicReference<Authentication> seenByChain = new AtomicReference<>();
        FilterChain capturingChain = (req, res) ->
                seenByChain.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, capturingChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seenByChain.get()).isNotNull();
        assertThat(seenByChain.get().getPrincipal()).isEqualTo("admin");
        assertThat(seenByChain.get().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void doFilter_ShouldClearSecurityContext_AfterChainCompletes() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", VALID_KEY);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_ShouldClearSecurityContext_WhenChainThrows() throws Exception {
        MockHttpServletRequest request = adminRequest(ADMIN_PATH);
        request.addHeader("X-API-Key", VALID_KEY);
        FilterChain failingChain = (req, res) -> {
            throw new ServletException("downstream failure");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), failingChain))
                .isInstanceOf(ServletException.class);

        // A leaked ROLE_ADMIN context would authorize the next request on
        // this thread - the finally block must clear it even on failure.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest adminRequest(final String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
