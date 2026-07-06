package com.example.cerpshashkin.integration.client;

import com.example.cerpshashkin.client.GeminiClient;
import com.example.cerpshashkin.exception.GeminiApiException;
import com.example.cerpshashkin.integration.BaseWireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiClientIntegrationTest extends BaseWireMockTest {

    private static final String GENERATE_PATH = "/v1beta/models/.*:generateContent";

    @Autowired
    private GeminiClient geminiClient;

    @Test
    void generate_ShouldReturnCandidateText_WhenGeminiResponds() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("gemini-success-response.json"))));

        String result = geminiClient.generate("system prompt", "user prompt");

        assertThat(result).isEqualTo("0.5");
        // Both prompts must land in their designated payload slots - a swap
        // would silently defeat the topic-restriction system prompt.
        verify(postRequestedFor(urlPathMatching(GENERATE_PATH))
                .withQueryParam("key", equalTo("test-gemini-key"))
                .withRequestBody(matchingJsonPath("$.system_instruction.parts[0].text", equalTo("system prompt")))
                .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text", equalTo("user prompt"))));
    }

    @Test
    void generate_ShouldThrowGeminiApiException_WhenServerReturns500() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": {\"message\": \"internal\"}}")));

        assertThatThrownBy(() -> geminiClient.generate("system", "user"))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("Gemini returned HTTP 500");
    }

    @Test
    void generate_ShouldThrowGeminiApiException_WhenRateLimited() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": {\"message\": \"quota exceeded\"}}")));

        assertThatThrownBy(() -> geminiClient.generate("system", "user"))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("429");
    }

    @Test
    void generate_ShouldThrowGeminiApiException_WhenNoCandidates() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"candidates\": []}")));

        assertThatThrownBy(() -> geminiClient.generate("system", "user"))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("no text candidates");
    }

    @Test
    void generate_ShouldThrowGeminiApiException_WhenCandidateHasNoParts() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"candidates\": [{\"content\": {\"parts\": []}}]}")));

        assertThatThrownBy(() -> geminiClient.generate("system", "user"))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("no text candidates");
    }

    @Test
    void generate_ShouldThrowGeminiApiException_WhenBodyMalformed() {
        stubFor(post(urlPathMatching(GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ not json")));

        // Deserialization failures surface as RestClientException and must be
        // wrapped in the client's own exception type, not leak Spring types.
        assertThatThrownBy(() -> geminiClient.generate("system", "user"))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("Gemini request failed");
    }
}
