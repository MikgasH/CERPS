package com.example.cerpshashkin.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

public record CurrencyExchangeResponse(
        boolean success,
        Instant lastUpdated,
        Currency base,
        LocalDate rateDate,
        Map<Currency, BigDecimal> rates,
        boolean isMockData
) {

    public static CurrencyExchangeResponse success(final Currency base,
                                                   final LocalDate rateDate,
                                                   final Map<Currency, BigDecimal> rates,
                                                   final boolean isMockData) {
        return new CurrencyExchangeResponse(true, Instant.now(), base,
                rateDate, rates, isMockData);
    }

    public static CurrencyExchangeResponse failure() {
        return new CurrencyExchangeResponse(false, Instant.now(), null, null, Map.of(), false);
    }
}
