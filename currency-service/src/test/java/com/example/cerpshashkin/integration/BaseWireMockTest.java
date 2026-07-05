package com.example.cerpshashkin.integration;

import com.example.cerpshashkin.integration.config.TestConfig;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class BaseWireMockTest {

    protected static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        System.out.println("WireMock started successfully on port: " + wireMockServer.port());
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        String baseUrl = "http://localhost:" + wireMockServer.port();
        registry.add("api.fixer.url", () -> baseUrl);
        registry.add("api.exchangerates.url", () -> baseUrl);
        registry.add("api.currencyapi.url", () -> baseUrl);
        registry.add("api.frankfurter.url", () -> baseUrl);
        registry.add("api.mock1.url", () -> baseUrl);
        registry.add("api.mock2.url", () -> baseUrl);
    }

    @Autowired
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    private static boolean dataInitialized = false;

    @BeforeEach
    void setUp() {
        if (!dataInitialized && supportedCurrencyRepository.count() == 0) {
            setupCurrencies();
            dataInitialized = true;
        }
        // The Spring context (and its circuit breakers) is shared across all
        // BaseWireMockTest subclasses; reset breaker state so failure-driven
        // tests never leak an OPEN breaker into the next test or class.
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        resetWireMock();
    }

    private void setupCurrencies() {
        List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "SEK", "NZD")
                .forEach(code -> supportedCurrencyRepository.save(
                        SupportedCurrencyEntity.builder()
                                .currencyCode(code)
                                .build()
                ));
    }

    void resetWireMock() {
        if (wireMockServer.isRunning()) {
            wireMockServer.resetAll();
            WireMock.configureFor("localhost", wireMockServer.port());
            setupDefaultWireMockStubs();
        } else {
            System.err.println("WireMock server is NOT running before test execution. Skipping reset.");
        }
    }

    protected void setupDefaultWireMockStubs() {
        setupFixerStub();
        setupExchangeRatesStub();
        setupCurrencyApiStub();
        setupFrankfurterStub();
        setupMockService1Stub();
        setupMockService2Stub();
    }

    protected void setupFixerStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("access_key", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));
    }

    protected void setupExchangeRatesStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("access_key", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));
    }

    protected void setupCurrencyApiStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("apikey", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("currencyapi-success-response.json"))));
    }

    protected void setupFrankfurterStub() {
        // Frankfurter v2 serves a flat array from /rates (no /latest path) and
        // carries a "base" query param, so it never matches the keyed provider
        // stubs above. The body date must be dynamic — FrankfurterClient drops
        // entries older than 4 business days.
        stubFor(get(urlPathMatching("/rates"))
                .withQueryParam("base", matching("EUR"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(frankfurterResponseBody(java.time.LocalDate.now()))));
    }

    protected String frankfurterResponseBody(final java.time.LocalDate date) {
        final java.util.Map<String, String> rates = new java.util.LinkedHashMap<>();
        rates.put("USD", "1.18");
        rates.put("GBP", "0.87");
        rates.put("JPY", "130.5");
        rates.put("CHF", "1.08");
        rates.put("CAD", "1.45");
        rates.put("AUD", "1.55");
        rates.put("CNY", "7.65");
        rates.put("SEK", "10.15");
        rates.put("NZD", "1.65");
        return frankfurterArrayBody(date, rates);
    }

    protected String frankfurterArrayBody(final java.time.LocalDate date, final java.util.Map<String, String> rates) {
        return rates.entrySet().stream()
                .map(entry -> "{\"date\": \"" + date + "\", \"base\": \"EUR\", \"quote\": \""
                        + entry.getKey() + "\", \"rate\": " + entry.getValue() + "}")
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    protected void setupMockService1Stub() {
        stubFor(get(urlPathEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("mock-service-success-response.json"))));
    }

    protected void setupMockService2Stub() {
        stubFor(get(urlPathEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("mock-service-success-response.json"))));
    }

    protected String readJsonFile(final String fileName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource("test-data/" + fileName);

            if (resource == null) {
                throw new IllegalArgumentException("File not found: test-data/" + fileName);
            }

            return Files.readString(Paths.get(resource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to read test data file: " + fileName, e);
        }
    }
}
