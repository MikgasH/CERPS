package com.example.cerpshashkin.dto;

import com.example.cerpshashkin.converter.ResponseConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

public record ExchangeRatesApiResponse(
        boolean success,

        @JsonProperty("timestamp")
        @JsonDeserialize(using = ResponseConverter.TimestampToInstantDeserializer.class)
        Instant lastUpdated,

        @JsonDeserialize(using = ResponseConverter.CurrencyDeserializer.class)
        Currency base,

        @JsonProperty("date")
        LocalDate rateDate,

        Map<String, BigDecimal> rates
) {}
