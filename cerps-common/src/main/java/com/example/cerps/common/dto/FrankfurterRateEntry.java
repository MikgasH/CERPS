package com.example.cerps.common.dto;

import com.example.cerps.common.converter.ResponseConverter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

/**
 * One element of the Frankfurter v2 {@code /rates} response, which is a flat
 * JSON array of per-pair objects:
 * {@code [{"date":"2026-01-15","base":"EUR","quote":"USD","rate":1.1645}, ...]}.
 *
 * <p>Lives in cerps-common because both currency-service (gap-fill provider)
 * and analytics-service (historical store backfill) parse this API shape.
 */
public record FrankfurterRateEntry(
        LocalDate date,

        @JsonDeserialize(using = ResponseConverter.CurrencyDeserializer.class)
        Currency base,

        String quote,

        BigDecimal rate
) {}
