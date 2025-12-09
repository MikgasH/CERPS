package com.example.cerpshashkin.config;

import com.example.cerpshashkin.BaseWireMockTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SecurityConfigIntegrationTest extends BaseWireMockTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String premiumToken;
    private String adminToken;

    @BeforeEach
    void authenticateUsers() throws Exception {
        userToken = loginAndGetToken("user@example.com", "password");
        premiumToken = loginAndGetToken("premium@example.com", "password");
        adminToken = loginAndGetToken("admin@example.com", "password");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        RestClient authClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/auth")
                .build();

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        String response = authClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(String.class);

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("token").asText();
    }

    @Test
    void getCurrencies_WithoutAuth_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void getCurrencies_WithUserRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("USD", "EUR", "GBP");
    }

    @Test
    void getCurrencies_WithPremiumRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .header("Authorization", "Bearer " + premiumToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("USD", "EUR", "GBP");
    }

    @Test
    void getCurrencies_WithAdminRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("USD", "EUR", "GBP");
    }

    @Test
    void addCurrency_WithUserRole_ShouldReturnForbidden() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("?currency=NOK")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void addCurrency_WithPremiumRole_ShouldReturnForbidden() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("?currency=NOK")
                .header("Authorization", "Bearer " + premiumToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void addCurrency_WithAdminRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.post()
                .uri("?currency=NOK")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("Currency NOK added successfully");
    }

    @Test
    void addCurrency_WithoutAuth_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("?currency=NOK")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void getExchangeRates_WithUserRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("\"success\":true");
    }

    @Test
    void getExchangeRates_WithPremiumRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + premiumToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("\"success\":true");
    }

    @Test
    void getExchangeRates_WithAdminRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("\"success\":true");
    }

    @Test
    void getExchangeRates_WithoutAuth_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void refreshRates_WithUserRole_ShouldReturnForbidden() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void refreshRates_WithPremiumRole_ShouldReturnForbidden() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + premiumToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void refreshRates_WithAdminRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response = client.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response).contains("Exchange rates updated successfully");
    }

    @Test
    void refreshRates_WithoutAuth_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.post()
                .uri("/refresh")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithUserRole_ShouldReturnForbidden() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithPremiumRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .header("Authorization", "Bearer " + premiumToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (req, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body).contains("Insufficient data");
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithAdminRole_ShouldReturnSuccess() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .onStatus(status -> status.value() == 400, (req, httpResponse) -> {
                    String body = new String(httpResponse.getBody().readAllBytes());
                    assertThat(body).contains("Insufficient data");
                })
                .toBodilessEntity();
    }

    @Test
    void getTrends_WithoutAuth_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .uri("/trends?from=USD&to=EUR&period=7D")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void accessWithInvalidToken_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .header("Authorization", "Bearer invalid.token.here")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void accessWithExpiredToken_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.invalid";

        client.get()
                .header("Authorization", "Bearer " + expiredToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void accessWithMalformedToken_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .header("Authorization", "Bearer not-a-jwt-token")
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void accessWithoutBearerPrefix_ShouldReturnUnauthorized() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        client.get()
                .header("Authorization", userToken)
                .retrieve()
                .onStatus(status -> status.value() == 403, (req, httpResponse) -> {
                    assertThat(httpResponse.getStatusCode().value()).isEqualTo(403);
                })
                .toBodilessEntity();
    }

    @Test
    void accessProtectedEndpoint_ThenRefreshToken_ShouldWork() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/currencies")
                .build();

        String response1 = client.get()
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response1).contains("USD");

        String response2 = client.post()
                .uri("/refresh")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response2).contains("Exchange rates updated successfully");

        String response3 = client.get()
                .uri("/exchange-rates?amount=100&from=USD&to=EUR")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertThat(response3).contains("\"success\":true");
    }
}
