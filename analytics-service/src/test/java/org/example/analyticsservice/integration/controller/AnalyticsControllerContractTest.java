package org.example.analyticsservice.integration.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.example.analyticsservice.integration.config.TestConfig;
import org.example.analyticsservice.service.cache.TrendsCache;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Golden-file contract lock for {@code GET /api/v1/analytics/trends}.
 *
 * <p>Asserts the exact JSON shape the Android client depends on, for the four
 * externally observable outcomes (200 / 404 / 422 / 503). Golden files live in
 * {@code test-data/contract/}. This test is the regression safety net for the
 * Task 4 historical-store migration: it must pass unmodified before and after
 * any {@code TrendsService} refactoring.
 *
 * <p>Comparison is {@link JSONCompareMode#STRICT}: any added, removed or
 * renamed field fails the test. Only the {@code timestamp} value of error
 * responses is dynamic; it is still required to be present and parseable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class AnalyticsControllerContractTest {

    private static final int WIREMOCK_PORT = 9561;
    private static final String TRENDS_URL = "/api/v1/analytics/trends";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TIMESTAMP_FIELD = "timestamp";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TrendsCache trendsCache;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);
        clearCaches();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        clearCaches();
    }

    @Test
    void getTrends_ShouldMatchSuccessGoldenFile_WhenDataAvailable() throws Exception {
        stubCurrencies();
        stubRateHistory("USD", "EUR", """
                [
                  { "timestamp": "2026-05-13T00:00:00Z", "rate": 0.909091 },
                  { "timestamp": "2026-05-28T00:00:00Z", "rate": 0.892857 },
                  { "timestamp": "2026-06-12T00:00:00Z", "rate": 0.847458 }
                ]
                """);

        final MvcResult result = mockMvc.perform(get(TRENDS_URL)
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "30D"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        assertMatchesGolden("trends-success.json", result, false);
    }

    @Test
    void getTrends_ShouldMatchInsufficientDataGoldenFile_WhenNoRateHistory() throws Exception {
        stubCurrencies();
        stubRateHistory("CHF", "CAD", "[]");

        final MvcResult result = mockMvc.perform(get(TRENDS_URL)
                        .param("from", "CHF")
                        .param("to", "CAD")
                        .param("period", "7D"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andReturn();

        assertMatchesGolden("trends-insufficient-data-404.json", result, true);
    }

    @Test
    void getTrends_ShouldMatchMinimumPeriodGoldenFile_WhenFrankfurterOnlyPairWith1D() throws Exception {
        stubCurrencies();

        final MvcResult result = mockMvc.perform(get(TRENDS_URL)
                        .param("from", "BYN")
                        .param("to", "EUR")
                        .param("period", "1D"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andReturn();

        assertMatchesGolden("trends-minimum-period-422.json", result, true);
    }

    @Test
    void getTrends_ShouldMatchUpstreamUnavailableGoldenFile_WhenCurrencyServiceDown() throws Exception {
        stubCurrencies();
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/rates/history"))
                .willReturn(aResponse().withStatus(500)));

        final MvcResult result = mockMvc.perform(get(TRENDS_URL)
                        .param("from", "GBP")
                        .param("to", "JPY")
                        .param("period", "7D"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andReturn();

        assertMatchesGolden("trends-upstream-down-503.json", result, true);
    }

    private void assertMatchesGolden(final String goldenFile, final MvcResult result,
                                     final boolean hasDynamicTimestamp) throws Exception {
        final String actual = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        final String expected = readGolden(goldenFile);

        if (hasDynamicTimestamp) {
            JSONAssert.assertEquals(expected, actual, new CustomComparator(JSONCompareMode.STRICT,
                    new Customization(TIMESTAMP_FIELD, (o1, o2) -> true)));
            final Object timestamp = new JSONObject(actual).get(TIMESTAMP_FIELD);
            assertThat(Instant.parse(timestamp.toString())).isNotNull();
        } else {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
        }
    }

    private String readGolden(final String fileName) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/test-data/contract/" + fileName),
                "Missing golden file: " + fileName)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void stubCurrencies() {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/currencies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[\"EUR\",\"USD\",\"GBP\",\"JPY\",\"CHF\",\"CAD\",\"BYN\"]")));
    }

    private void stubRateHistory(final String from, final String to, final String pointsJson) {
        final String body = """
                { "from": "%s", "to": "%s", "points": %s }
                """.formatted(from, to, pointsJson);
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/rates/history"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(body)));
    }

    private void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            final var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        trendsCache.invalidateAll();
    }
}
