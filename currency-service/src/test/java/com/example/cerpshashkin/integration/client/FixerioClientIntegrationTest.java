package com.example.cerpshashkin.integration.client;

import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.client.impl.FixerioClient;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Currency;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixerioClientIntegrationTest extends BaseWireMockTest {

    @Autowired
    private FixerioClient fixerioClient;

    @Test
    void getLatestRates_ShouldReturnSuccessfulResponse() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));

        CurrencyExchangeResponse result = fixerioClient.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("GBP"),
                Currency.getInstance("JPY")
        );
    }

    @Test
    void getLatestRates_WhenServerReturns500_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch latest exchange rates from Fixer.io");
    }

    @Test
    void getLatestRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Not Found\"}")));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getLatestRates_WhenServerReturns401_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Unauthorized\"}")));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 401");
    }

    @Test
    void getLatestRates_WhenSuccessFalse_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": false, \"error\": {\"code\": 101, \"type\": \"invalid_access_key\"}}")));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("API returned success=false");
    }

    @Test
    void getLatestRates_WhenNullRates_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"timestamp\": 1725459054, \"base\": \"EUR\", \"date\": \"2025-09-04\", \"rates\": null}")));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Empty rates received");
    }

    @Test
    void getLatestRates_WhenInvalidJson_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json")));

        assertThatThrownBy(() -> fixerioClient.getLatestRates())
                .isInstanceOf(Exception.class);
    }

    @Test
    void getLatestRates_WhenEmptyRates_ShouldReturnEmptyMap() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"timestamp\": 1725459054, \"base\": \"EUR\", \"date\": \"2025-09-04\", \"rates\": {}}")));

        CurrencyExchangeResponse result = fixerioClient.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getLatestRates_ShouldShortCircuitWithoutHttpCall_WhenCircuitBreakerOpens() {
        stubFor(get(urlEqualTo("/latest?access_key=test-fixer-key"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        final CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("fixerClient");

        // Retry wraps the breaker, so every attempt (2 per call) is recorded;
        // 5 recorded failures reach minimum-number-of-calls at 100% failure rate.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(fixerioClient::getLatestRates).isInstanceOf(Exception.class);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        wireMockServer.resetRequests();

        assertThatThrownBy(fixerioClient::getLatestRates)
                .isInstanceOf(CallNotPermittedException.class);

        verify(0, getRequestedFor(urlPathEqualTo("/latest")));
    }

    @Test
    void getProviderName_ShouldReturnFixerio() {
        assertThat(fixerioClient.getProviderName()).isEqualTo("Fixer.io");
    }
}
