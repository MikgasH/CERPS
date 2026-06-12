package com.example.cerpshashkin.dto;

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
        Instant timestamp
) {
    public static final String SOURCE_DATABASE = "DATABASE";
    public static final String SOURCE_FRANKFURTER = "FRANKFURTER";
}
