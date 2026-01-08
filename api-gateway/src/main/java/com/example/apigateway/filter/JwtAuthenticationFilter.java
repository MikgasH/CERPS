package com.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLES = "X-User-Roles";
    private static final String ROLES_CLAIM = "roles";

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/**",
            "/*/swagger-ui/**",
            "/*/swagger-ui.html",
            "/*/v3/api-docs/**",
            "/*/actuator/**",
            "/user-service/v3/api-docs/**",
            "/currency-service/v3/api-docs/**",
            "/analytics-service/v3/api-docs/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        final ServerHttpRequest request = exchange.getRequest();
        final String path = request.getPath().value();

        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        final String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            final List<String> roles = extractRoles(claims);

            log.debug("JWT validated for user: {} with roles: {}", username, roles);

            final ServerHttpRequest modifiedRequest = request.mutate()
                    .header(X_USER_EMAIL, username)
                    .header(X_USER_ROLES, String.join(",", roles))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (final JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
        } catch (final Exception e) {
            log.error("Error during JWT processing: {}", e.getMessage());
            return onError(exchange, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicEndpoint(final String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(final Claims claims) {
        final Object rolesObj = claims.get(ROLES_CLAIM);
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    private SecretKey getSignInKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> onError(final ServerWebExchange exchange, final String message, final HttpStatus status) {
        final ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        final String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());
        final byte[] bytes = body.getBytes();

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}

