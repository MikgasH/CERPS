package com.example.cerpshashkin.exception;

import java.time.LocalDate;

public class HistoricalRatesNotFoundException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "No exchange rates available for %s on %s";

    public HistoricalRatesNotFoundException(final String baseCurrency, final LocalDate date) {
        super(String.format(MESSAGE_TEMPLATE, baseCurrency, date));
    }
}
