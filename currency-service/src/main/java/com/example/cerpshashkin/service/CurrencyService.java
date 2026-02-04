package com.example.cerpshashkin.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.dto.CurrentRatesResponse;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyConversionService conversionService;
    private final ExchangeRateService exchangeRateService;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public List<String> getSupportedCurrencies() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .sorted()
                .toList();
    }

    public ConversionResponse convertCurrency(final ConversionRequest request) {
        return conversionService.convertCurrency(request);
    }

    public CurrentRatesResponse getCurrentRatesForBase(final String base) {
        return exchangeRateService.getAllRatesForBase(base);
    }
}
