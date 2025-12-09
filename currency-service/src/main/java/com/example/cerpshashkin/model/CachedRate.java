package com.example.cerpshashkin.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CachedRate(
        BigDecimal rate,
        Instant timestamp
) {}
