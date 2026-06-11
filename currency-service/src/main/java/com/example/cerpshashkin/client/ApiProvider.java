package com.example.cerpshashkin.client;

import lombok.Getter;

@Getter
public enum ApiProvider {
    FIXER("Fixer.io"),
    EXCHANGE_RATES("ExchangeRatesAPI"),
    CURRENCY_API("CurrencyAPI"),
    FRANKFURTER("Frankfurter");

    private final String displayName;

    ApiProvider(final String displayName) {
        this.displayName = displayName;
    }
}
