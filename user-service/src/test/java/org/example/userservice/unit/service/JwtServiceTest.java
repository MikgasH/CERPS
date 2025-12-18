package org.example.userservice.unit.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.userservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    private static final String TEST_JWT_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW11c3QtYmUtbG9uZy1lbm91Z2gtZm9yLWhzMjU2LWFsZ29yaXRobQ==";
    private static final long TEST_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);

        testUser = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                ))
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void generateToken_WithUserDetails_ShouldReturnValidToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should include username in token subject")
    void generateToken_ShouldIncludeUsername() {
        String token = jwtService.generateToken(testUser);
        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should include roles in token claims")
    void generateToken_ShouldIncludeRoles() {
        String token = jwtService.generateToken(testUser);
        Claims claims = extractAllClaims(token);

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should include multiple roles in token")
    void generateToken_WithMultipleRoles_ShouldIncludeAllRoles() {
        UserDetails multiRoleUser = User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .build();

        String token = jwtService.generateToken(multiRoleUser);
        Claims claims = extractAllClaims(token);

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should set expiration time correctly")
    void generateToken_ShouldSetCorrectExpiration() {
        String token = jwtService.generateToken(testUser);
        Claims claims = extractAllClaims(token);

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();

        long tokenLifetime = expiration.getTime() - issuedAt.getTime();
        assertThat(tokenLifetime).isEqualTo(TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate token with custom claims")
    void generateToken_WithExtraClaims_ShouldIncludeThem() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");
        extraClaims.put("roles", List.of("ROLE_USER"));

        String token = jwtService.generateToken(extraClaims, testUser);
        Claims claims = extractAllClaims(token);

        assertThat(claims.get("customClaim")).isEqualTo("customValue");
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        String token = jwtService.generateToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should extract custom claims from token")
    void extractClaim_ShouldReturnCorrectValue() {
        String token = jwtService.generateToken(testUser);

        String subject = jwtService.extractClaim(token, Claims::getSubject);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(subject).isEqualTo("test@example.com");
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate token successfully for correct user")
    void isTokenValid_WithCorrectUser_ShouldReturnTrue() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for different user")
    void isTokenValid_WithDifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);

        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for invalid token format")
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token.format";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for tampered token")
    void extractUsername_WithTamperedToken_ShouldThrowException() {
        String token = jwtService.generateToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for token with wrong signature")
    void isTokenValid_WithWrongSignature_ShouldThrowException() {
        JwtService differentSecretService = new JwtService();
        ReflectionTestUtils.setField(differentSecretService, "jwtSecret",
                "ZGlmZmVyZW50LXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtbXVzdC1iZS1sb25nLWVub3VnaA==");
        ReflectionTestUtils.setField(differentSecretService, "jwtExpiration", TEST_EXPIRATION);

        String tokenWithDifferentSecret = differentSecretService.generateToken(testUser);

        assertThatThrownBy(() -> jwtService.isTokenValid(tokenWithDifferentSecret, testUser))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle empty roles list")
    void generateToken_WithNoRoles_ShouldGenerateToken() {
        UserDetails noRolesUser = User.builder()
                .username("noroles@example.com")
                .password("password")
                .authorities(List.of())
                .build();

        String token = jwtService.generateToken(noRolesUser);
        Claims claims = extractAllClaims(token);

        assertThat(token).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).isEmpty();
    }

    private Claims extractAllClaims(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_JWT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
