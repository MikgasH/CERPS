package com.example.cerpshashkin.integration.client;

import com.example.cerpshashkin.client.impl.FrankfurterClient;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrankfurterClientIntegrationTest extends BaseWireMockTest {

    @Autowired
    private FrankfurterClient frankfurterClient;

    @Test
    void getLatestRates_WithSymbols_ShouldReturnRequestedRates() {
        stubFor(get(urlPathEqualTo("/latest"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN,RUB"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\": \"EUR\", \"date\": \"" + LocalDate.now()
                                + "\", \"rates\": {\"BYN\": 3.25, \"RUB\": 95.5}}")));

        CurrencyExchangeResponse result = frankfurterClient.getLatestRates(Set.of("RUB", "BYN"));

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("BYN"),
                Currency.getInstance("RUB")
        );
    }

    @Test
    void getLatestRates_WithoutSymbols_ShouldReturnAllRates() {
        CurrencyExchangeResponse result = frankfurterClient.getLatestRates();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsKey(Currency.getInstance("USD"));
    }

    @Test
    void getLatestRates_WhenResponseDateIsStale_ShouldReturnEmptyRates() {
        stubFor(get(urlPathEqualTo("/latest"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\": \"EUR\", \"date\": \"" + LocalDate.now().minusDays(14)
                                + "\", \"rates\": {\"BYN\": 3.25}}")));

        CurrencyExchangeResponse result = frankfurterClient.getLatestRates(Set.of("BYN"));

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getHistoricalRates_ShouldReturnRatesForDate() {
        LocalDate date = LocalDate.of(2024, 3, 15);

        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("date", equalTo("2024-03-15"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("PLN,USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\": \"EUR\", \"date\": \"2024-03-15\","
                                + " \"rates\": {\"USD\": 1.09, \"PLN\": 4.29}}")));

        CurrencyExchangeResponse result = frankfurterClient.getHistoricalRates(date, Set.of("USD", "PLN"));

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rateDate()).isEqualTo(date);
        assertThat(result.rates()).containsKeys(
                Currency.getInstance("USD"),
                Currency.getInstance("PLN")
        );
    }

    @Test
    void getLatestRates_WhenServerReturns500_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/latest"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("error-response.json"))));

        assertThatThrownBy(() -> frankfurterClient.getLatestRates(Set.of("BYN")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 500");
    }

    @Test
    void getLatestRates_WhenNullRates_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/latest"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"base\": \"EUR\", \"date\": \"" + LocalDate.now() + "\", \"rates\": null}")));

        assertThatThrownBy(() -> frankfurterClient.getLatestRates(Set.of("BYN")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Empty rates received");
    }

    @Test
    void getHistoricalRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Not Found\"}")));

        assertThatThrownBy(() -> frankfurterClient.getHistoricalRates(LocalDate.of(2024, 3, 15), Set.of("USD")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getProviderName_ShouldReturnFrankfurter() {
        assertThat(frankfurterClient.getProviderName()).isEqualTo("Frankfurter");
        assertThat(frankfurterClient.isFallback()).isTrue();
    }
}
