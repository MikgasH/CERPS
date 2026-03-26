package org.example.analyticsservice.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.example.analyticsservice.integration.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "currency-service.url=http://localhost:9561")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(9561);
        wireMockServer.start();
        WireMock.configureFor("localhost", 9561);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getTrends_WithValidParams_ShouldReturn200() throws Exception {
        stubCurrencies();
        stubRateHistory("USD", "EUR", List.of(
                ratePoint(Instant.now().minus(7, ChronoUnit.DAYS), "0.909091"),
                ratePoint(Instant.now().minus(3, ChronoUnit.DAYS), "0.892857"),
                ratePoint(Instant.now(), "0.847458")
        ));

        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.changePercentage").value(notNullValue()));
    }

    @Test
    void getTrends_WithInsufficientData_ShouldReturn404() throws Exception {
        stubCurrencies();
        stubRateHistory("CHF", "CAD", List.of());

        mockMvc.perform(get("/api/v1/analytics/trends")
                        .param("from", "CHF")
                        .param("to", "CAD")
                        .param("period", "7D"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Insufficient Data"));
    }

    @Test
    void actuatorEndpoint_ShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private void stubCurrencies() throws Exception {
        List<String> currencies = List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD");
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/currencies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(currencies))));
    }

    private void stubRateHistory(String from, String to, List<Map<String, Object>> points) throws Exception {
        Map<String, Object> response = Map.of(
                "from", from,
                "to", to,
                "points", points
        );
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/rates/history"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    private Map<String, Object> ratePoint(Instant timestamp, String rate) {
        return Map.of(
                "timestamp", timestamp.toString(),
                "rate", new BigDecimal(rate)
        );
    }
}
