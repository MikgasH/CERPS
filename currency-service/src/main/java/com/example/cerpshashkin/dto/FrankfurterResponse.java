package com.example.cerpshashkin.dto;

import com.example.cerps.common.converter.ResponseConverter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

public record FrankfurterResponse(
        @JsonDeserialize(using = ResponseConverter.CurrencyDeserializer.class)
        Currency base,

        LocalDate date,

        Map<String, BigDecimal> rates
) {}
