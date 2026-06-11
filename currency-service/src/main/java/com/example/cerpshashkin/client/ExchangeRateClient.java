package com.example.cerpshashkin.client;

import com.example.cerpshashkin.model.CurrencyExchangeResponse;

import java.util.Set;

public interface ExchangeRateClient {

    CurrencyExchangeResponse getLatestRates();
    String getProviderName();

    /**
     * Fallback clients are excluded from the median aggregation and are only
     * queried for currencies the primary providers did not return.
     */
    default boolean isFallback() {
        return false;
    }

    /**
     * Fetches latest rates restricted to the given currency codes. Primary
     * providers ignore the restriction and return their full rate set.
     */
    default CurrencyExchangeResponse getLatestRates(final Set<String> symbols) {
        return getLatestRates();
    }
}
