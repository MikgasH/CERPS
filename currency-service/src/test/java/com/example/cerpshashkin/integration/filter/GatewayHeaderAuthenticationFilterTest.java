package com.example.cerpshashkin.integration.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayHeaderAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void filter_WithValidUserHeaders_ShouldAuthenticateAndAllowAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithAdminHeaders_ShouldAuthenticateAndAllowAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "admin@example.com")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithMultipleRoles_ShouldAuthenticateAndAllowAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", "ROLE_USER,ROLE_PREMIUM_USER"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithPremiumUserRole_ShouldAuthenticateAndAllowAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "premium@example.com")
                        .header("X-User-Roles", "ROLE_PREMIUM_USER"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithRolesContainingSpaces_ShouldParseAndAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", " ROLE_USER , ROLE_ADMIN "))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithNoRoles_ShouldAuthenticateButDenyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithoutUserEmail_ShouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithEmptyUserEmail_ShouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithBlankUserEmail_ShouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "   ")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithoutHeaders_ShouldDenyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithInvalidRole_ShouldAuthenticateButDenyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", "ROLE_INVALID"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_PublicEndpoint_ShouldAllowWithoutHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_PublicEndpoint_ShouldAllowWithHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk());
    }
}
