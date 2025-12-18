package com.example.apigateway.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("API Gateway Integration Tests")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final String TEST_USER = "test@example.com";
    private static final List<String> TEST_ROLES = List.of("ROLE_USER");

    @Test
    @DisplayName("Public endpoint - actuator health should be accessible")
    void publicEndpoint_ActuatorHealth_ShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Public endpoint - Swagger UI should redirect")
    void publicEndpoint_SwaggerUI_ShouldBeAccessible() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    @DisplayName("Protected endpoint without auth should return 401")
    void protectedEndpoint_WithoutAuth_ShouldReturn401() {
        webTestClient.get()
                .uri("/api/v1/currencies")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    @DisplayName("Protected endpoint with invalid token should return 401")
    void protectedEndpoint_WithInvalidToken_ShouldReturn401() {
        webTestClient.get()
                .uri("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Invalid JWT token");
    }

    @Test
    @DisplayName("Protected endpoint with valid token should pass authentication")
    void protectedEndpoint_WithValidToken_ShouldPassAuthentication() {
        String token = generateValidToken(TEST_USER, TEST_ROLES);

        webTestClient.get()
                .uri("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Multiple protected endpoints should require authentication")
    void multipleProtectedEndpoints_ShouldRequireAuth() {
        webTestClient.get().uri("/api/v1/currencies").exchange().expectStatus().isUnauthorized();
        webTestClient.get().uri("/api/v1/analytics/trends").exchange().expectStatus().isUnauthorized();
        webTestClient.get().uri("/api/v1/admin/provider-keys").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Token with multiple roles should pass authentication")
    void tokenWithMultipleRoles_ShouldPassAuthentication() {
        List<String> multipleRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = generateValidToken(TEST_USER, multipleRoles);

        webTestClient.get()
                .uri("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Token without Bearer prefix should be rejected")
    void tokenWithoutBearerPrefix_ShouldBeRejected() {
        String token = generateValidToken(TEST_USER, TEST_ROLES);

        webTestClient.get()
                .uri("/api/v1/currencies")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Public endpoint - login should be accessible without auth")
    void publicEndpoint_Login_ShouldBeAccessible() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Public endpoint - register should be accessible without auth")
    void publicEndpoint_Register_ShouldBeAccessible() {
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    private String generateValidToken(String username, List<String> roles) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .claims(Map.of("roles", roles))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
