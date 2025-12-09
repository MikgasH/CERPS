package com.example.cerpshashkin.client.integration;

import com.example.cerpshashkin.BaseWireMockTest;
import com.example.cerpshashkin.client.impl.ExchangeRatesClient;
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

class ExchangeRatesClientIntegrationTest extends BaseWireMockTest {

    @Autowired
    private ExchangeRatesClient exchangeRatesClient;

    @Test
    void getLatestRates_ShouldReturnSuccessfulResponse() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));

        CurrencyExchangeResponse result = exchangeRatesClient.getLatestRates();

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
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch latest exchange rates from ExchangeRatesAPI");
    }

    @Test
    void getLatestRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Not Found\"}")));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getLatestRates_WhenServerReturns401_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Unauthorized\"}")));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 401");
    }

    @Test
    void getLatestRates_WhenSuccessFalse_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": false, \"error\": {\"code\": 101, \"type\": \"invalid_access_key\"}}")));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("API returned success=false");
    }

    @Test
    void getLatestRates_WhenNullRates_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"timestamp\": 1725459054, \"base\": \"EUR\", \"date\": \"2025-09-04\", \"rates\": null}")));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Empty rates received");
    }

    @Test
    void getLatestRates_WhenInvalidJson_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json")));

        assertThatThrownBy(() -> exchangeRatesClient.getLatestRates())
                .isInstanceOf(Exception.class);
    }

    @Test
    void getLatestRates_WhenEmptyRates_ShouldReturnEmptyMap() {
        stubFor(get(urlEqualTo("/latest?access_key=test-exchangerates-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"timestamp\": 1725459054, \"base\": \"EUR\", \"date\": \"2025-09-04\", \"rates\": {}}")));

        CurrencyExchangeResponse result = exchangeRatesClient.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getProviderName_ShouldReturnExchangeRatesAPI() {
        assertThat(exchangeRatesClient.getProviderName()).isEqualTo("ExchangeRatesAPI");
    }
}
