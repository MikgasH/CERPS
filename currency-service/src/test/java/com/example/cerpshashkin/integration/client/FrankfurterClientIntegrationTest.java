package com.example.cerpshashkin.integration.client;

import com.example.cerpshashkin.client.impl.FrankfurterClient;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
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
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN,RUB"))
                .withQueryParam("date", absent())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(frankfurterArrayBody(LocalDate.now(),
                                ratesOf("BYN", "3.25", "RUB", "95.5")))));

        CurrencyExchangeResponse result = frankfurterClient.getLatestRates(Set.of("RUB", "BYN"));

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.base()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(result.rateDate()).isEqualTo(LocalDate.now());
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
    void getLatestRates_WhenEntriesAreStale_ShouldReturnEmptyRates() {
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(frankfurterArrayBody(LocalDate.now().minusDays(14),
                                ratesOf("BYN", "3.25")))));

        CurrencyExchangeResponse result = frankfurterClient.getLatestRates(Set.of("BYN"));

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getLatestRates_WithMixedFreshAndStaleEntries_ShouldKeepOnlyFresh() {
        String body = "[{\"date\": \"" + LocalDate.now() + "\", \"base\": \"EUR\", \"quote\": \"BYN\", \"rate\": 3.25},"
                + " {\"date\": \"" + LocalDate.now().minusDays(14) + "\", \"base\": \"EUR\", \"quote\": \"RUB\", \"rate\": 95.5}]";
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN,RUB"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        CurrencyExchangeResponse result = frankfurterClient.getLatestRates(Set.of("BYN", "RUB"));

        assertThat(result.rates()).containsOnlyKeys(Currency.getInstance("BYN"));
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
                        .withBody(frankfurterArrayBody(date,
                                ratesOf("USD", "1.09", "PLN", "4.29")))));

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
    void getHistoricalRates_WithEmptyArray_ShouldReturnEmptyRates() {
        LocalDate date = LocalDate.of(2005, 6, 1);

        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("date", equalTo("2005-06-01"))
                .withQueryParam("base", equalTo("EUR"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        CurrencyExchangeResponse result = frankfurterClient.getHistoricalRates(date, Set.of("BYN"));

        assertThat(result.success()).isTrue();
        assertThat(result.rates()).isEmpty();
    }

    @Test
    void getHistoricalRates_ShouldSkipUnknownQuoteCurrencies() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        String body = "[{\"date\": \"2024-03-15\", \"base\": \"EUR\", \"quote\": \"USD\", \"rate\": 1.09},"
                + " {\"date\": \"2024-03-15\", \"base\": \"EUR\", \"quote\": \"XXY\", \"rate\": 2.0}]";

        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("date", equalTo("2024-03-15"))
                .withQueryParam("base", equalTo("EUR"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        CurrencyExchangeResponse result = frankfurterClient.getHistoricalRates(date, Set.of("USD", "XXY"));

        assertThat(result.rates()).containsOnlyKeys(Currency.getInstance("USD"));
    }

    @Test
    void getLatestRates_WhenServerReturns500_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/rates"))
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
    void getLatestRates_WhenBodyIsNull_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .withQueryParam("quotes", equalTo("BYN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        assertThatThrownBy(() -> frankfurterClient.getLatestRates(Set.of("BYN")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Null response received");
    }

    @Test
    void getHistoricalRates_WhenServerReturns404_ShouldThrowException() {
        stubFor(get(urlPathEqualTo("/rates"))
                .withQueryParam("base", equalTo("EUR"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":404,\"message\":\"not found\"}")));

        assertThatThrownBy(() -> frankfurterClient.getHistoricalRates(LocalDate.of(2024, 3, 15), Set.of("USD")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP error: 404");
    }

    @Test
    void getProviderName_ShouldReturnFrankfurter() {
        assertThat(frankfurterClient.getProviderName()).isEqualTo("Frankfurter");
        assertThat(frankfurterClient.isFallback()).isTrue();
    }

    private Map<String, String> ratesOf(final String... pairs) {
        Map<String, String> rates = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            rates.put(pairs[i], pairs[i + 1]);
        }
        return rates;
    }
}
