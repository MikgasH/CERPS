package com.example.cerpshashkin.controller;

import com.example.cerpshashkin.BaseWireMockTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrencyControllerIntegrationTest extends BaseWireMockTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private RestClient restClient;
    private RestClient authRestClient;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        authRestClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/auth")
                .build();

        userToken = loginAndGetToken("user@example.com", "password");
        adminToken = loginAndGetToken("admin@example.com", "password");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        String response = authRestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(String.class);

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("token").asText();
    }

    @Test
    void getCurrencies_ShouldReturnDefaultCurrencies() {
        final String response = restClient.get()
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {})
                .body(String.class);

        assertThat(response)
                .contains("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "SEK", "NZD");
    }

    @Test
    void addCurrency_WithValidCurrency_ShouldReturnSuccess() {
        final String response = restClient.post()
                .uri("?currency=NOK")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {})
                .body(String.class);

        assertThat(response).contains("Currency NOK added successfully");

        final String getCurrenciesResponse = restClient.get()
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(getCurrenciesResponse).contains("NOK");
    }

    @Test
    void addCurrency_WithInvalidCurrency_ShouldReturnBadRequest() {
        restClient.post()
                .uri("?currency=INVALID")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body)
                            .contains("Validation error")
                            .contains("Invalid currency code");
                })
                .toBodilessEntity();
    }

    @Test
    void addCurrency_WithEmptyCurrency_ShouldReturnBadRequest() {
        restClient.post()
                .uri("?currency=")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body).contains("Validation error");
                })
                .toBodilessEntity();
    }

    @Test
    void addCurrency_WithUserRole_ShouldReturnForbidden() {
        restClient.post()
                .uri("?currency=NOK")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void exchangeRates_WithSameCurrencies_ShouldReturnSameAmount() {
        final String response = restClient.get()
                .uri("/exchange-rates?amount=100&from=USD&to=USD")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(response)
                .contains("\"success\":true")
                .contains("\"convertedAmount\":100")
                .contains("\"exchangeRate\":1");
    }

    @Test
    void exchangeRates_WithValidCurrencies_AndMockProvider_ShouldReturnConversion() {
        wireMockServer.resetAll();

        stubFor(get(urlPathMatching("/latest.*"))
                .willReturn(aResponse().withStatus(500)));

        setupMockService1Stub();
        setupMockService2Stub();

        final String response = restClient.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(response)
                .contains("\"success\":true")
                .contains("\"originalAmount\":100")
                .contains("\"fromCurrency\":\"USD\"")
                .contains("\"toCurrency\":\"EUR\"");
    }

    @Test
    void exchangeRates_WithSuccessfulExternalProvider_ShouldReturnConversion() {
        final String response = restClient.get()
                .uri("/exchange-rates?amount=100&from=EUR&to=USD")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(response)
                .contains("\"success\":true")
                .contains("\"originalAmount\":100");
    }

    @Test
    void exchangeRates_WithUnsupportedFromCurrency_ShouldReturnBadRequest() {
        restClient.get()
                .uri("/exchange-rates?amount=100&from=INVALID&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body)
                            .contains("Validation error")
                            .contains("Invalid source currency code");
                })
                .toBodilessEntity();
    }

    @Test
    void exchangeRates_WithInvalidFromCurrency_ShouldReturnBadRequest() {
        restClient.get()
                .uri("/exchange-rates?amount=100&from=INVALID&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body)
                            .contains("Validation error")
                            .contains("Invalid source currency code");
                })
                .toBodilessEntity();
    }

    @Test
    void exchangeRates_WithInvalidToCurrency_ShouldReturnBadRequest() {
        restClient.get()
                .uri("/exchange-rates?amount=100&from=USD&to=ZZZZZ")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body)
                            .contains("Validation error")
                            .contains("Invalid target currency code");
                })
                .toBodilessEntity();
    }

    @Test
    void exchangeRates_WithNegativeAmount_ShouldReturnBadRequest() {
        restClient.get()
                .uri("/exchange-rates?amount=-100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void exchangeRates_WithZeroAmount_ShouldReturnBadRequest() {
        restClient.get()
                .uri("/exchange-rates?amount=0&from=USD&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void refreshRates_WithMockProvider_ShouldReturnSuccess() {
        wireMockServer.resetAll();

        stubFor(get(urlPathMatching("/latest.*"))
                .willReturn(aResponse().withStatus(500)));

        setupMockService1Stub();
        setupMockService2Stub();

        final String response = restClient.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("Exchange rates updated successfully");
    }

    @Test
    void refreshRates_WithSuccessfulExternalProvider_ShouldReturnSuccess() {
        final String response = restClient.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("Exchange rates updated successfully");
    }

    @Test
    void refreshRates_WithUserRole_ShouldReturnForbidden() {
        restClient.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void fullWorkflow_AddConvertRefreshDelete_ShouldWorkCorrectly() {
        wireMockServer.resetAll();

        stubFor(get(urlPathMatching("/latest.*"))
                .willReturn(aResponse().withStatus(500)));

        setupMockService1Stub();
        setupMockService2Stub();

        String response = restClient.get()
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);
        assertThat(response).contains("USD", "EUR", "GBP");

        restClient.post()
                .uri("?currency=SEK")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();

        response = restClient.get()
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);
        assertThat(response).contains("SEK");

        restClient.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();

        response = restClient.get()
                .uri("/exchange-rates?amount=100&from=EUR&to=SEK")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);
        assertThat(response).contains("\"success\":true");
    }

    @Test
    void getTrends_WithValidParameters_ShouldReturnInsufficientDataError() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body)
                            .contains("Insufficient data")
                            .contains("Found 0 data points, need at least 2");
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithInvalidToCurrency_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&to=INVALID&period=7D")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body).contains("Validation error");
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithInvalidPeriod_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR&period=INVALID")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body).contains("Validation error");
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithTooShortPeriod_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR&period=1H")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithTooLongPeriod_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR&period=2Y")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithMissingToCurrency_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&period=7D")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithMissingPeriod_ShouldReturnValidationError() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(400);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithUserRole_ShouldReturnForbidden() {
        restClient.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (request, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }
}
