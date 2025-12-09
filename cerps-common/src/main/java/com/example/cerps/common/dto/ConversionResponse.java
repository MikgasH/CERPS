package com.example.cerps.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ConversionResponse(
        boolean success,
        BigDecimal originalAmount,
        String fromCurrency,
        String toCurrency,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        Instant timestamp
) {
    public static ConversionResponse success(
            final BigDecimal originalAmount,
            final String from,
            final String to,
            final BigDecimal convertedAmount,
            final BigDecimal rate
    ) {
        return new ConversionResponse(
                true,
                originalAmount,
                from,
                to,
                convertedAmount,
                rate,
                Instant.now()
        );
    }
}
