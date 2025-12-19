package com.example.cerpshashkin.integration.security;

import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        when(exchangeRateService.getExchangeRate(any(Currency.class), any(Currency.class)))
                .thenReturn(Optional.of(BigDecimal.valueOf(1.18)));
    }

    @Test
    void actuatorEndpoints_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrencies_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCurrencies_WithUserRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PREMIUM_USER")
    void getCurrencies_WithPremiumUserRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCurrencies_WithAdminRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk());
    }

    @Test
    void addCurrency_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType("application/json")
                        .content("{\"code\": \"NOK\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void addCurrency_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType("application/json")
                        .content("{\"code\": \"NOK\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PREMIUM_USER")
    void addCurrency_WithPremiumUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType("application/json")
                        .content("{\"code\": \"NOK\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshCurrencies_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void refreshCurrencies_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void convertCurrency_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/convert")
                        .contentType("application/json")
                        .content("{\"amount\": 100, \"from\": \"USD\", \"to\": \"EUR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void convertCurrency_WithUserRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/convert")
                        .contentType("application/json")
                        .content("{\"amount\": 100, \"from\": \"USD\", \"to\": \"EUR\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PREMIUM_USER")
    void convertCurrency_WithPremiumUserRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/convert")
                        .contentType("application/json")
                        .content("{\"amount\": 100, \"from\": \"USD\", \"to\": \"EUR\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void convertCurrency_WithAdminRole_ShouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/currencies/convert")
                        .contentType("application/json")
                        .content("{\"amount\": 100, \"from\": \"USD\", \"to\": \"EUR\"}"))
                .andExpect(status().isOk());
    }
}
