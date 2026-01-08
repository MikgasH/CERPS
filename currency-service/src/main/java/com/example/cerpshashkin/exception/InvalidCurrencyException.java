package com.example.cerpshashkin.exception;

public class InvalidCurrencyException extends CurrencyServiceException {
    private static final String MESSAGE = "Invalid currency code: %s";

    public InvalidCurrencyException(final String currency) {
        super(String.format(MESSAGE, currency));
    }
}
