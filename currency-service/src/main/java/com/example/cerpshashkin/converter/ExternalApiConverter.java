package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Slf4j
public class ExternalApiConverter {

    private static final String ERROR_NULL_RESPONSE = "{} cannot be null";
    private static final String WARN_API_UNSUCCESSFUL = "{} API returned unsuccessful response";
    private static final String DEBUG_UNKNOWN_CURRENCY = "Skipping unknown currency from {}: {}";
    private static final String PROVIDER_NAME_FIXER = "Fixer.io";
    private static final String PROVIDER_NAME_EXCHANGE_RATES = "ExchangeRatesAPI";

    public CurrencyExchangeResponse convertFromFixer(final FixerioResponse fixerResponse) {
        if (fixerResponse == null) {
            throw new IllegalArgumentException(ERROR_NULL_RESPONSE.replace("{}", FixerioResponse.class.getSimpleName()));
        }
        return convert(
                fixerResponse.success(),
                fixerResponse.lastUpdated(),
                fixerResponse.base(),
                fixerResponse.rateDate(),
                fixerResponse.rates(),
                PROVIDER_NAME_FIXER,
                false
        );
    }

    public CurrencyExchangeResponse convertFromExchangeRates(final ExchangeRatesApiResponse exchangeRatesResponse) {
        if (exchangeRatesResponse == null) {
            throw new IllegalArgumentException(ERROR_NULL_RESPONSE.replace("{}", ExchangeRatesApiResponse.class.getSimpleName()));
        }
        return convert(
                exchangeRatesResponse.success(),
                exchangeRatesResponse.lastUpdated(),
                exchangeRatesResponse.base(),
                exchangeRatesResponse.rateDate(),
                exchangeRatesResponse.rates(),
                PROVIDER_NAME_EXCHANGE_RATES,
                false
        );
    }

    private CurrencyExchangeResponse convert(
            final boolean success,
            final Instant lastUpdated,
            final Currency base,
            final LocalDate rateDate,
            final Map<String, BigDecimal> rawRates,
            final String providerName,
            final boolean isMockData
    ) {
        if (!success) {
            log.warn(WARN_API_UNSUCCESSFUL, providerName);
            return CurrencyExchangeResponse.failure();
        }

        final Map<Currency, BigDecimal> currencyRates = new HashMap<>();

        if (rawRates != null) {
            rawRates.forEach((currencyCode, rate) -> {
                try {
                    final Currency currency = Currency.getInstance(currencyCode);
                    currencyRates.put(currency, rate);
                } catch (IllegalArgumentException e) {
                    log.debug(DEBUG_UNKNOWN_CURRENCY, providerName, currencyCode);
                }
            });
        }

        return new CurrencyExchangeResponse(
                true,
                lastUpdated,
                base,
                rateDate,
                currencyRates,
                isMockData
        );
    }
}
