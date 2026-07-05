package org.example.analyticsservice.integration.client;

import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerps.common.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.analyticsservice.client.FrankfurterClient;
import org.example.analyticsservice.integration.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "resilience4j.retry.instances.frankfurter.wait-duration=50ms")
@ActiveProfiles("test")
@Import(TestConfig.class)
class FrankfurterClientIntegrationTest {

    private static final int WIREMOCK_PORT = 9562;

    @Autowired
    private FrankfurterClient frankfurterClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.start();
        // Reset breaker state so failure-driven tests don't leave it OPEN for
        // the next test method (the context, hence the breaker, is shared).
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getRates_WithValidRange_ShouldReturnAllEntries() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("PLN,USD"))
                .withQueryParam("from", equalTo("2026-03-02"))
                .withQueryParam("to", equalTo("2026-03-04"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("rates-range-response.json"))));

        List<FrankfurterRateEntry> entries = frankfurterClient.getRates(
                Set.of("USD", "PLN"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));

        assertThat(entries).hasSize(6);
        assertThat(entries.getFirst().date()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(entries.getFirst().base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(entries.getFirst().quote()).isEqualTo("PLN");
        assertThat(entries.getFirst().rate()).isEqualByComparingTo(new BigDecimal("4.3105"));
        assertThat(entries)
                .extracting(FrankfurterRateEntry::quote)
                .containsOnly("PLN", "USD");
    }

    @Test
    void getRates_WithEmptyArray_ShouldReturnEmptyList() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("rates-empty-response.json"))));

        List<FrankfurterRateEntry> entries = frankfurterClient.getRates(
                Set.of("BYN"), LocalDate.of(2005, 6, 1), LocalDate.of(2005, 6, 30));

        assertThat(entries).isEmpty();
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/rates")));
    }

    @Test
    void getRates_WhenBodyIsNull_ShouldThrowExternalServiceException() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        assertThatThrownBy(() -> frankfurterClient.getRates(
                Set.of("USD"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Null response");
    }

    @Test
    void getRates_WhenServerReturns500_ShouldThrowAfterRetries() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-500-response.json"))));

        assertThatThrownBy(() -> frankfurterClient.getRates(
                Set.of("USD"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to fetch rates from Frankfurter");

        wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/rates")));
    }

    @Test
    void getRates_WhenFirstAttemptFails_ShouldRetryAndSucceed() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("recovered")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-500-response.json"))));
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .inScenario("retry")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("rates-range-response.json"))));

        List<FrankfurterRateEntry> entries = frankfurterClient.getRates(
                Set.of("USD", "PLN"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));

        assertThat(entries).hasSize(6);
        wireMockServer.verify(2, getRequestedFor(urlPathEqualTo("/rates")));
    }

    @Test
    void getRates_ShouldShortCircuitWithoutHttpCall_WhenCircuitBreakerOpens() {
        wireMockServer.stubFor(get(urlPathEqualTo("/rates"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-500-response.json"))));

        final CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("frankfurter");

        // Retry wraps the breaker, so every attempt (3 per call) is recorded;
        // 5 recorded failures reach minimum-number-of-calls at 100% failure rate.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> frankfurterClient.getRates(
                    Set.of("USD"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4)))
                    .isInstanceOf(Exception.class);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        wireMockServer.resetRequests();

        assertThatThrownBy(() -> frankfurterClient.getRates(
                Set.of("USD"), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4)))
                .isInstanceOf(CallNotPermittedException.class);

        wireMockServer.verify(0, getRequestedFor(urlPathEqualTo("/rates")));
    }

    private String readJsonFile(final String fileName) {
        try (InputStream stream = getClass().getResourceAsStream("/test-data/frankfurter/" + fileName)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read test fixture " + fileName, e);
        }
    }
}
