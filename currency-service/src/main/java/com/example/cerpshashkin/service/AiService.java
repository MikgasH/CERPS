package com.example.cerpshashkin.service;

import com.example.cerpshashkin.client.GeminiClient;
import com.example.cerpshashkin.config.GeminiProperties;
import com.example.cerpshashkin.exception.GeminiApiException;
import com.example.cerpshashkin.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    private static final int MAX_BANK_NAME_LENGTH = 100;
    private static final Pattern VALID_BANK_NAME = Pattern.compile("^[\\p{L}\\p{N} '\\-]+$");
    private static final Pattern NUMBER = Pattern.compile("(\\d+\\.?\\d*)");
    private static final String NOT_FOUND_MARKER = "NOT_FOUND";

    private static final String PROMPT_SYSTEM = "system";
    private static final String PROMPT_BANK_COMMISSION = "bank-commission";
    private static final String BANK_NAME_PLACEHOLDER = "{bankName}";

    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;

    public void validateBankName(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("bankName must not be blank");
        }
        if (name.length() > MAX_BANK_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "bankName must be at most " + MAX_BANK_NAME_LENGTH + " characters");
        }
        if (!VALID_BANK_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "bankName may only contain letters, digits, spaces, hyphens, and apostrophes");
        }
    }

    public Double getBankCommission(final String bankName) {
        final String systemPrompt = requirePrompt(PROMPT_SYSTEM);
        final String userPrompt = requirePrompt(PROMPT_BANK_COMMISSION)
                .replace(BANK_NAME_PLACEHOLDER, bankName);

        final String raw;
        try {
            raw = geminiClient.generate(systemPrompt, userPrompt);
        } catch (final GeminiApiException ex) {
            log.warn("Gemini lookup failed for bankName='{}': {}", bankName, ex.getMessage());
            throw new ServiceUnavailableException("Gemini AI service unavailable", ex);
        }

        final String trimmed = raw == null ? "" : raw.trim();
        log.debug("Gemini response for bankName='{}': {}", bankName, trimmed);

        if (trimmed.contains(NOT_FOUND_MARKER)) {
            return null;
        }

        final Matcher matcher = NUMBER.matcher(trimmed);
        if (!matcher.find()) {
            log.info("Gemini returned no parseable number for bankName='{}': {}", bankName, trimmed);
            return null;
        }

        try {
            return Double.parseDouble(matcher.group(1));
        } catch (final NumberFormatException ex) {
            log.warn("Could not parse Gemini number '{}' for bankName='{}'", matcher.group(1), bankName);
            return null;
        }
    }

    private String requirePrompt(final String key) {
        if (geminiProperties.prompts() == null || !geminiProperties.prompts().containsKey(key)) {
            throw new IllegalStateException("Missing gemini.prompts." + key + " in configuration");
        }
        return geminiProperties.prompts().get(key);
    }
}
