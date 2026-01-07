package com.example.cerpshashkin.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface RateQueryResult {

    BigDecimal getRate();

    String getRateType();

    Instant getTimestamp();
}
