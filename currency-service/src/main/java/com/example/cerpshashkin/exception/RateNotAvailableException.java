package com.example.cerpshashkin.exception;

public class RateNotAvailableException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Exchange rate not available for %s -> %s";

    public RateNotAvailableException(final String fromCurrency, final String toCurrency) {
        super(String.format(MESSAGE_TEMPLATE, fromCurrency, toCurrency));
    }
}
