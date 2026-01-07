package org.example.analyticsservice.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CurrencyNotSupportedException extends RuntimeException {

    private final String invalidCurrency;
    private final List<String> supportedCurrencies;

    public CurrencyNotSupportedException(final String invalidCurrency, final List<String> supportedCurrencies) {
        super(String.format("Currency '%s' is not supported. Supported currencies: %s",
                invalidCurrency, String.join(", ", supportedCurrencies)));
        this.invalidCurrency = invalidCurrency;
        this.supportedCurrencies = supportedCurrencies;
    }
}
