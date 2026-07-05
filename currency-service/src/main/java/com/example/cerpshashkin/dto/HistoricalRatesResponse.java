package com.example.cerpshashkin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record HistoricalRatesResponse(
        @JsonProperty("base") String baseCurrency,
        LocalDate date,
        Map<String, BigDecimal> rates,
        String source,
        Instant timestamp,
        // Whether the snapshot covers every supported currency. Kept out of
        // the JSON contract: it only drives the per-entry cache TTL, so a
        // partial snapshot (failed gap-fill) is retried soon instead of being
        // served for the full TTL.
        @JsonIgnore boolean complete
) {
    public static final String SOURCE_DATABASE = "DATABASE";
    public static final String SOURCE_FRANKFURTER = "FRANKFURTER";
}
