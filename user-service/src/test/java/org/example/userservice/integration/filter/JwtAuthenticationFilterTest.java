package org.example.userservice.integration.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.repository.UserRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("JwtAuthenticationFilter Integration Tests")
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "filter-test@example.com";
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should authenticate request with valid JWT token")
    void filter_WithValidJWT_ShouldAuthenticate() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should extract username from JWT token")
    void filter_WithValidJWT_ShouldSetSecurityContext() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("Should allow multiple requests with same valid token")
    void filter_WithSameToken_ShouldAuthenticateMultipleTimes() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject request without Authorization header")
    void filter_WithoutAuthHeader_ShouldRejectRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with empty Authorization header")
    void filter_WithEmptyAuthHeader_ShouldRejectRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request without Bearer prefix")
    void filter_WithoutBearerPrefix_ShouldRejectRequest() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with invalid Bearer format")
    void filter_WithInvalidBearerFormat_ShouldRejectRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token")
    void filter_WithInvalidJWT_ShouldRejectRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with malformed JWT token")
    void filter_WithMalformedJWT_ShouldRejectRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer notajwttoken"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject request with tampered JWT token")
    void filter_WithTamperedJWT_ShouldRejectRequest() throws Exception {
        String validToken = registerAndLogin();
        String tamperedToken = validToken.substring(0, validToken.length() - 10) + "TAMPERED12";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow public endpoints without JWT token")
    void filter_PublicEndpoint_ShouldNotRequireToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("public@example.com")
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should allow login endpoint without JWT token")
    void filter_LoginEndpoint_ShouldNotRequireToken() throws Exception {
        registerUser(TEST_EMAIL, TEST_PASSWORD);

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
    @DisplayName("Should allow actuator endpoints without JWT token")
    void filter_ActuatorEndpoint_ShouldNotRequireToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should extract roles from JWT token")
    void filter_WithValidJWT_ShouldExtractRoles() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Should handle JWT token with extra spaces")
    void filter_WithTokenWithSpaces_ShouldRejectRequest() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer  " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle case-sensitive Bearer prefix")
    void filter_WithLowercaseBearer_ShouldRejectRequest() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should not process filter twice for same request")
    void filter_OncePerRequestFilter_ShouldProcessOnce() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void registerUser(String email, String password) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
    }

    private String registerAndLogin() throws Exception {
        registerUser(TEST_EMAIL, TEST_PASSWORD);

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
