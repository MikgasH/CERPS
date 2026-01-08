package com.example.cerps.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TrendsResponse(
        String from,
        String to,
        String period,
        BigDecimal oldRate,
        BigDecimal newRate,
        BigDecimal changePercentage,
        Instant startDate,
        Instant endDate,
        Integer dataPoints
) {
    public static TrendsResponse success(
            final String from,
            final String to,
            final String period,
            final BigDecimal oldRate,
            final BigDecimal newRate,
            final BigDecimal changePercentage,
            final Instant startDate,
            final Instant endDate,
            final Integer dataPoints
    ) {
        return new TrendsResponse(
                from,
                to,
                period,
                oldRate,
                newRate,
                changePercentage,
                startDate,
                endDate,
                dataPoints
        );
    }
}
