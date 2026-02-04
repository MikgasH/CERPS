package com.example.cerpshashkin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CurrentRatesResponse(
        String baseCurrency,
        Instant timestamp,
        Map<String, BigDecimal> rates,
        int totalCurrencies
) {
    public static CurrentRatesResponse success(final String baseCurrency, final Map<String, BigDecimal> rates) {
        return new CurrentRatesResponse(
                baseCurrency,
                Instant.now(),
                rates,
                rates.size()
        );
    }
}
