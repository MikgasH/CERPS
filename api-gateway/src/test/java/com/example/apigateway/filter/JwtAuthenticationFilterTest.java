package com.example.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LW1pbi0yNTYtYml0cy10ZXN0LXNlY3JldC1rZXktbWluLTI1Ni1iaXRz";
    private static final String TEST_USER = "test@example.com";
    private static final List<String> TEST_ROLES = List.of("ROLE_USER");

    private JwtAuthenticationFilter filter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", TEST_SECRET);
    }

    @Test
    void filter_PublicEndpoint_ShouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();
    }

    @Test
    void filter_ValidToken_ShouldAddUserHeaders() {
        String token = generateToken(TEST_USER, TEST_ROLES);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any());
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertNotNull(capturedExchange);

        String userEmail = capturedExchange.getRequest().getHeaders().getFirst("X-User-Email");
        String userRoles = capturedExchange.getRequest().getHeaders().getFirst("X-User-Roles");

        assertEquals(TEST_USER, userEmail);
        assertNotNull(userRoles);
        assertTrue(userRoles.contains("ROLE_USER"));
    }

    @Test
    void filter_MissingToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_InvalidToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_TokenWithoutBearerPrefix_ShouldReturnUnauthorized() {
        String token = generateToken(TEST_USER, TEST_ROLES);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_MultipleRoles_ShouldAddAllRolesToHeader() {
        List<String> multipleRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = generateToken(TEST_USER, multipleRoles);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String userRoles = capturedExchange.getRequest().getHeaders().getFirst("X-User-Roles");

        assertNotNull(userRoles);
        assertTrue(userRoles.contains("ROLE_USER"));
        assertTrue(userRoles.contains("ROLE_ADMIN"));
    }

    @Test
    void filter_ExpiredToken_ShouldReturnUnauthorized() {
        String expiredToken = generateExpiredToken(TEST_USER, TEST_ROLES);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_ActuatorEndpoint_ShouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any());
    }

    @Test
    void getOrder_ShouldReturnNegative100() {
        assertEquals(-100, filter.getOrder());
    }

    private String generateToken(String username, List<String> roles) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .claims(Map.of("roles", roles))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private String generateExpiredToken(String username, List<String> roles) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .claims(Map.of("roles", roles))
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();
    }
}
