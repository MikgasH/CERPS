package org.example.analyticsservice.integration.controller;

import org.example.analyticsservice.integration.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTrends_WithValidParams_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "premium@example.com")
                        .header("X-User-Roles", "ROLE_PREMIUM_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.changePercentage").value(notNullValue()));
    }

    @Test
    void getTrends_WithoutAuth_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTrends_WithUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "user@example.com")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTrends_WithAdminRole_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D")
                        .header("X-User-Email", "admin@example.com")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"));
    }

    @Test
    void getTrends_WithInsufficientData_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "CHF")
                        .param("to", "CAD")
                        .param("period", "7D")
                        .header("X-User-Email", "premium@example.com")
                        .header("X-User-Roles", "ROLE_PREMIUM_USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Insufficient Data"));
    }

    @Test
    void actuatorEndpoint_ShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
