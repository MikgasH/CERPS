package com.example.cerpshashkin.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CurrencyNotSupportedException extends CurrencyServiceException {
    private static final String MESSAGE_TEMPLATE = "Currency '%s' is not supported. Supported currencies: %s";

    private final String invalidCurrency;
    private final List<String> supportedCurrencies;

    public CurrencyNotSupportedException(final String currencyCode, final List<String> availableCurrencies) {
        super(String.format(MESSAGE_TEMPLATE, currencyCode, String.join(", ", availableCurrencies)));
        this.invalidCurrency = currencyCode;
        this.supportedCurrencies = availableCurrencies;
    }
}
