package com.example.cerpshashkin.client.integration;

import com.example.cerpshashkin.BaseWireMockTest;
import com.example.cerpshashkin.client.impl.MockService1Client;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Currency;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockService1ClientIntegrationTest extends BaseWireMockTest {

    @Autowired
    private MockService1Client mockService1Client;

    @Test
    void getLatestRates_ShouldReturnSuccessfulResponse() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("mock-service-success-response.json"))));

        CurrencyExchangeResponse result = mockService1Client.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).isNotEmpty();
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("GBP"),
                Currency.getInstance("JPY")
        );
    }

    @Test
    void getLatestRates_WhenServerReturns500_ShouldThrowException() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        assertThatThrownBy(() -> mockService1Client.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch latest exchange rates from MockService1");
    }

    @Test
    void getLatestRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Not Found\"}")));

        assertThatThrownBy(() -> mockService1Client.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getLatestRates_WhenServerReturns503_ShouldThrowException() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        assertThatThrownBy(() -> mockService1Client.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 503");
    }

    @Test
    void getLatestRates_WhenInvalidJson_ShouldThrowException() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json")));

        assertThatThrownBy(() -> mockService1Client.getLatestRates())
                .isInstanceOf(Exception.class);
    }

    @Test
    void getLatestRates_WhenEmptyRates_ShouldReturnEmptyMap() {
        stubFor(get(urlEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"timestamp\": 1729425600, \"base\": \"EUR\", \"date\": \"2025-10-20\", \"rates\": {}}")));

        CurrencyExchangeResponse result = mockService1Client.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getProviderName_ShouldReturnMockService1() {
        assertThat(mockService1Client.getProviderName()).isEqualTo("MockService1");
    }
}
