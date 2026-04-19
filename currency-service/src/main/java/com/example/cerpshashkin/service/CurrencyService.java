package com.example.cerpshashkin.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.dto.CurrentRatesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyConversionService conversionService;
    private final ExchangeRateService exchangeRateService;
    private final SupportedCurrenciesService supportedCurrenciesService;

    public List<String> getSupportedCurrencies() {
        return supportedCurrenciesService.getSupportedCurrencyCodes();
    }

    public ConversionResponse convertCurrency(final ConversionRequest request) {
        return conversionService.convertCurrency(request);
    }

    @Cacheable(value = "currentRates", key = "#base")
    public CurrentRatesResponse getCurrentRatesForBase(final String base) {
        return exchangeRateService.getAllRatesForBase(base);
    }
}
