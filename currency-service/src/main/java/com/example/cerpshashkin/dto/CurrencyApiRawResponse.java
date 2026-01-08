package com.example.cerpshashkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

public record CurrencyApiRawResponse(
        Meta meta,
        Map<String, CurrencyData> data
) {
    public record Meta(
            @JsonProperty("last_updated_at") ZonedDateTime lastUpdatedAt
    ) {}

    public record CurrencyData(
            String code,
            BigDecimal value
    ) {}
}
