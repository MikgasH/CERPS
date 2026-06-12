package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.dto.FrankfurterRateEntry;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
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
    private static final String PROVIDER_NAME_FRANKFURTER = "Frankfurter";

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

    /**
     * Frankfurter v2 returns a flat array of per-pair entries instead of a
     * rates object. All entries of one response share the same base and date;
     * unknown quote currencies are skipped like in the other converters.
     */
    public CurrencyExchangeResponse convertFromFrankfurter(final List<FrankfurterRateEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException(ERROR_NULL_RESPONSE.replace("{}", FrankfurterRateEntry.class.getSimpleName()));
        }

        final Map<Currency, BigDecimal> currencyRates = new HashMap<>();
        Currency base = null;
        LocalDate rateDate = null;

        for (final FrankfurterRateEntry entry : entries) {
            if (entry == null || entry.quote() == null || entry.rate() == null) {
                continue;
            }
            try {
                currencyRates.put(Currency.getInstance(entry.quote()), entry.rate());
            } catch (IllegalArgumentException e) {
                log.debug(DEBUG_UNKNOWN_CURRENCY, PROVIDER_NAME_FRANKFURTER, entry.quote());
                continue;
            }
            if (base == null) {
                base = entry.base();
            }
            if (rateDate == null || (entry.date() != null && entry.date().isAfter(rateDate))) {
                rateDate = entry.date();
            }
        }

        // Frankfurter has no success flag or update timestamp — a parsed response is a successful one.
        return new CurrencyExchangeResponse(true, Instant.now(), base, rateDate, currencyRates, false);
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
