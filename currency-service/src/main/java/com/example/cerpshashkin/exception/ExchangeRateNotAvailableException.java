package com.example.cerpshashkin.exception;

public class ExchangeRateNotAvailableException extends CurrencyServiceException {
    private static final String MESSAGE_FULL = "Exchange rate not available for %s -> %s";

    public ExchangeRateNotAvailableException(final String fromCurrency, final String toCurrency) {
        super(String.format(MESSAGE_FULL, fromCurrency, toCurrency));
    }

    public ExchangeRateNotAvailableException(final String message) {
        super(message);
    }
}
