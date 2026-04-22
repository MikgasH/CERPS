package com.example.cerpshashkin.client;

import com.example.cerpshashkin.config.GeminiProperties;
import com.example.cerpshashkin.dto.GeminiRequest;
import com.example.cerpshashkin.dto.GeminiResponse;
import com.example.cerpshashkin.exception.GeminiApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class GeminiClient {

    private static final String ENDPOINT_TEMPLATE = "v1beta/models/%s:generateContent";
    private static final String KEY_PARAM = "key";

    private final RestClient geminiRestClient;
    private final GeminiProperties properties;

    public GeminiClient(
            @Qualifier("geminiRestClient") final RestClient geminiRestClient,
            final GeminiProperties properties
    ) {
        this.geminiRestClient = geminiRestClient;
        this.properties = properties;
    }

    public String generate(final String systemPrompt, final String userPrompt) {
        final String endpoint = String.format(ENDPOINT_TEMPLATE, properties.model());
        final GeminiRequest requestBody = GeminiRequest.of(systemPrompt, userPrompt);

        log.debug("Calling Gemini: endpoint={}, model={}", endpoint, properties.model());

        try {
            final GeminiResponse response = geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder.path(endpoint)
                            .queryParam(KEY_PARAM, properties.apiKey())
                            .build())
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (request, httpResponse) -> {
                                throw new GeminiApiException(
                                        "Gemini returned HTTP " + httpResponse.getStatusCode());
                            })
                    .body(GeminiResponse.class);

            return extractText(response);
        } catch (final ResourceAccessException ex) {
            log.warn("Gemini network or timeout failure: {}", ex.getMessage());
            throw new GeminiApiException("Gemini unreachable or timed out", ex);
        } catch (final GeminiApiException ex) {
            throw ex;
        } catch (final RestClientException ex) {
            log.warn("Gemini REST client error: {}", ex.getMessage());
            throw new GeminiApiException("Gemini request failed", ex);
        }
    }

    private String extractText(final GeminiResponse response) {
        return Optional.ofNullable(response)
                .map(GeminiResponse::candidates)
                .filter(list -> !list.isEmpty())
                .map(List::getFirst)
                .map(GeminiResponse.Candidate::content)
                .map(GeminiResponse.Content::parts)
                .filter(list -> !list.isEmpty())
                .map(List::getFirst)
                .map(GeminiResponse.Part::text)
                .orElseThrow(() -> new GeminiApiException("Gemini response had no text candidates"));
    }
}
