package com.example.cerpshashkin.client;

import com.example.cerpshashkin.model.CurrencyExchangeResponse;

public interface ExchangeRateClient {

    CurrencyExchangeResponse getLatestRates();
    String getProviderName();
}
