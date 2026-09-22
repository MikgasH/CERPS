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
    private static final String NOT_FOUND_MARKER = "NOT_FOUND";

    // The model is asked (see gemini.prompts in application.yml) for a bare
    // number, but it can still answer with surrounding prose that contains
    // other numbers ("as of 2024, 2.5%"). Candidates are therefore taken in
    // order of confidence -- the whole answer, then a percent-anchored number,
    // then any number -- and every candidate must fall inside a plausible
    // commission range before it is accepted.
    private static final double MAX_PLAUSIBLE_COMMISSION_PERCENT = 20.0;
    private static final Pattern BARE_NUMBER = Pattern.compile("^\\d+(?:\\.\\d+)?$");
    private static final Pattern PERCENT_NUMBER = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

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

    // Returns Double despite the repo's BigDecimal-for-money rule: this is an
    // LLM-estimated display percentage parsed from free text, not stored
    // financial data — no arithmetic is ever performed on it. Null means
    // "unknown".
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

        final Double commission = extractCommission(trimmed);
        if (commission == null) {
            log.info("Gemini returned no plausible commission for bankName='{}': {}", bankName, trimmed);
        }
        return commission;
    }

    private Double extractCommission(final String text) {
        if (BARE_NUMBER.matcher(text).matches()) {
            return plausibleOrNull(text);
        }

        final Double percentValue = firstPlausibleMatch(PERCENT_NUMBER.matcher(text), 1);
        if (percentValue != null) {
            return percentValue;
        }

        return firstPlausibleMatch(ANY_NUMBER.matcher(text), 0);
    }

    private Double firstPlausibleMatch(final Matcher matcher, final int group) {
        while (matcher.find()) {
            final Double value = plausibleOrNull(matcher.group(group));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    // The candidate always comes from a digits-only pattern, so parsing cannot
    // fail; the bounds check is what rejects stray numbers such as a year.
    private Double plausibleOrNull(final String candidate) {
        final double value = Double.parseDouble(candidate);
        return value <= MAX_PLAUSIBLE_COMMISSION_PERCENT ? value : null;
    }

    private String requirePrompt(final String key) {
        if (geminiProperties.prompts() == null || !geminiProperties.prompts().containsKey(key)) {
            throw new IllegalStateException("Missing gemini.prompts." + key + " in configuration");
        }
        return geminiProperties.prompts().get(key);
    }
}
