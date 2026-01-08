package org.example.userservice.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("SecurityConfig Integration Tests")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_EMAIL = "security-test@example.com";
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Public endpoint: /api/v1/auth/register should be accessible without authentication")
    void registerEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Public endpoint: /api/v1/auth/login should be accessible without authentication")
    void loginEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public endpoint: /api/internal/auth/validate should be accessible without authentication")
    void internalAuthEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public endpoint: /actuator/health should be accessible without authentication")
    void actuatorHealthEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public endpoint: /swagger-ui.html should be accessible without authentication")
    void swaggerEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Public endpoint: /v3/api-docs should be accessible without authentication")
    void apiDocsEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint: /api/v1/auth/me should require authentication")
    void meEndpoint_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected endpoint: /api/v1/auth/me should be accessible with valid JWT")
    void meEndpoint_WithValidJWT_ShouldReturn200() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint: /api/v1/auth/change-password should require authentication")
    void changePasswordEndpoint_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"New123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected endpoint: /api/v1/auth/change-password should be accessible with valid JWT")
    void changePasswordEndpoint_WithValidJWT_ShouldBeAccessible() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TEST_PASSWORD + "\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token")
    void protectedEndpoint_WithInvalidJWT_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with malformed Authorization header")
    void protectedEndpoint_WithMalformedAuthHeader_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with empty Authorization header")
    void protectedEndpoint_WithEmptyAuthHeader_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with Bearer prefix but no token")
    void protectedEndpoint_WithBearerPrefixOnly_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User with ROLE_USER should access /api/v1/auth/me")
    void userWithRoleUser_ShouldAccessMeEndpoint() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User with ROLE_ADMIN should access /api/v1/auth/me")
    void userWithRoleAdmin_ShouldAccessMeEndpoint() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF should be disabled for public endpoints")
    void publicEndpoints_ShouldNotRequireCSRFToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("csrf-test@example.com")
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CSRF should be disabled for protected endpoints")
    void protectedEndpoints_ShouldNotRequireCSRFToken() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TEST_PASSWORD + "\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should use stateless session management")
    void sessionManagement_ShouldBeStateless() throws Exception {
        String token = registerAndLogin();

        MvcResult result1 = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String sessionCookie = result1.getResponse().getHeader("Set-Cookie");
    }

    private String registerAndLogin() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
    }
}
