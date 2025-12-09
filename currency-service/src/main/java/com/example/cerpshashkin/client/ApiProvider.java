package com.example.cerpshashkin.client;

import lombok.Getter;

@Getter
public enum ApiProvider {
    FIXER("Fixer.io"),
    EXCHANGE_RATES("ExchangeRatesAPI"),
    CURRENCY_API("CurrencyAPI"),
    MOCK_SERVICE_1("MockService1"),
    MOCK_SERVICE_2("MockService2");

    private final String displayName;

    ApiProvider(final String displayName) {
        this.displayName = displayName;
    }
}
