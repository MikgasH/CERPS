package com.example.cerpshashkin.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.CurrencyNotSupportedException;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyService {

    private static final String LOG_GET_SUPPORTED = "Getting supported currencies from database";
    private static final String LOG_RETURN_CURRENCIES = "Returning {} supported currencies";
    private static final String LOG_ADD_CURRENCY = "Adding currency to database: {}";
    private static final String LOG_CURRENCY_ADDED = "Currency {} added successfully";
    private static final String LOG_CURRENCY_ALREADY_EXISTS = "Currency {} already exists";
    private static final String LOG_REFRESH_RATES = "Refreshing exchange rates";
    private static final String ERROR_CURRENCY_NULL_OR_EMPTY = "Currency code cannot be null or empty";

    private final CurrencyConversionService conversionService;
    private final ExchangeRateService exchangeRateService;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public List<String> getSupportedCurrencies() {
        log.info(LOG_GET_SUPPORTED);
        final List<String> currencies = supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .sorted()
                .toList();
        log.info(LOG_RETURN_CURRENCIES, currencies.size());
        return currencies;
    }

    @Transactional
    public void addCurrency(final String currencyCode) {
        log.info(LOG_ADD_CURRENCY, currencyCode);

        final String normalizedCode = validateAndNormalizeCurrency(currencyCode);

        if (supportedCurrencyRepository.existsByCurrencyCode(normalizedCode)) {
            log.info(LOG_CURRENCY_ALREADY_EXISTS, normalizedCode);
            return;
        }

        final SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .currencyCode(normalizedCode)
                .build();

        supportedCurrencyRepository.save(entity);
        log.info(LOG_CURRENCY_ADDED, normalizedCode);
    }

    public ConversionResponse convertCurrency(final ConversionRequest request) {
        log.info("Converting currency: {} {} to {}",
                request.amount(), request.from(), request.to());
        return conversionService.convertCurrency(request);
    }

    public void refreshExchangeRates() {
        log.info(LOG_REFRESH_RATES);
        exchangeRateService.refreshRates();
    }

    public void validateSupportedCurrencies(final String from, final String to) {
        final List<String> supported = getSupportedCurrencies();

        if (!supported.contains(from.toUpperCase())) {
            throw new CurrencyNotSupportedException(
                    from.toUpperCase(),
                    supported
            );
        }

        if (!supported.contains(to.toUpperCase())) {
            throw new CurrencyNotSupportedException(
                    to.toUpperCase(),
                    supported
            );
        }
    }

    private String validateAndNormalizeCurrency(final String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new InvalidCurrencyException(ERROR_CURRENCY_NULL_OR_EMPTY);
        }

        final String normalized = currencyCode.trim().toUpperCase();

        try {
            Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currencyCode);
        }
    }
}
