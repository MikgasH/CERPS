package com.example.cerpshashkin.exception;

import java.util.List;

public class CurrencyNotSupportedException extends CurrencyServiceException {
    private static final String MESSAGE_TEMPLATE = "Currency '%s' is not supported. Available currencies: %s";

    public CurrencyNotSupportedException(final String currencyCode, final List<String> availableCurrencies) {
        super(String.format(MESSAGE_TEMPLATE, currencyCode, String.join(", ", availableCurrencies)));
    }
}
