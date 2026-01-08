package com.example.cerpshashkin.exception;

public class CurrencyNotFoundException extends CurrencyServiceException {
    private static final String MESSAGE = "Currency not found: %s";

    public CurrencyNotFoundException(final String currency) {
        super(String.format(MESSAGE, currency));
    }
}
