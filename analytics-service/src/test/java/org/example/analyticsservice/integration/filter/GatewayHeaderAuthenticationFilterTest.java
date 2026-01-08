package org.example.analyticsservice.integration.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GatewayHeaderAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void filter_WithValidHeaders_ShouldAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "premium@example.com")
                        .header("X-User-Roles", "ROLE_PREMIUM_USER"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithMultipleRoles_ShouldParseAllRoles() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "admin@example.com")
                        .header("X-User-Roles", "ROLE_USER,ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void filter_WithoutHeaders_ShouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithEmptyEmail_ShouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "")
                        .header("X-User-Roles", "ROLE_PREMIUM_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filter_WithRolesWithSpaces_ShouldTrimAndParse() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "premium@example.com")
                        .header("X-User-Roles", " ROLE_PREMIUM_USER , ROLE_USER "))
                .andExpect(status().isOk());
    }

    @Test
    void filter_PublicEndpoint_ShouldAllowWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
