package com.example.cerpshashkin.client.integration;

import com.example.cerpshashkin.BaseWireMockTest;
import com.example.cerpshashkin.client.impl.CurrencyApiClient;
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

class CurrencyApiClientIntegrationTest extends BaseWireMockTest {

    @Autowired
    private CurrencyApiClient currencyApiClient;

    @Test
    void getLatestRates_ShouldReturnSuccessfulResponse() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("currencyapi-success-response.json"))));

        CurrencyExchangeResponse result = currencyApiClient.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("USD"));
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("EUR"),
                Currency.getInstance("GBP"),
                Currency.getInstance("JPY")
        );
    }

    @Test
    void getLatestRates_WhenServerReturns500_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Failed to fetch latest exchange rates from CurrencyAPI");
    }

    @Test
    void getLatestRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Not Found\"}")));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getLatestRates_WhenServerReturns401_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Unauthorized\"}")));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 401");
    }

    @Test
    void getLatestRates_WhenEmptyData_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("currencyapi-empty-response.json"))));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Empty data received");
    }

    @Test
    void getLatestRates_WhenNullData_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"meta\": {\"last_updated_at\": \"2023-06-23T10:04:00Z\"}, \"data\": null}")));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Response data is null");
    }

    @Test
    void getLatestRates_WhenInvalidMetaInfo_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"meta\": null, \"data\": {\"EUR\": {\"code\": \"EUR\", \"value\": 0.85}}}")));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Invalid meta information");
    }

    @Test
    void getLatestRates_WhenInvalidJson_ShouldThrowException() {
        stubFor(get(urlEqualTo("/latest?apikey=test-currencyapi-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json")));

        assertThatThrownBy(() -> currencyApiClient.getLatestRates())
                .isInstanceOf(Exception.class);
    }

    @Test
    void getProviderName_ShouldReturnCurrencyAPI() {
        assertThat(currencyApiClient.getProviderName()).isEqualTo("CurrencyAPI");
    }
}
