package com.example.cerps.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RatePoint(
        Instant timestamp,
        BigDecimal rate
) {
}
