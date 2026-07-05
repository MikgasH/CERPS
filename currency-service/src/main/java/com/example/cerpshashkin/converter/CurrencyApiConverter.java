package com.example.cerpshashkin.converter;

import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CurrencyApiConverter {

    private static final String MISSING_META_LOG =
            "CurrencyAPI response is missing meta.last_updated_at - treating response as invalid";

    public CurrencyExchangeResponse convertToCurrencyExchange(final CurrencyApiRawResponse raw) {
        if (raw == null || raw.data() == null) {
            return CurrencyExchangeResponse.failure();
        }

        // The rate date comes from meta; a malformed response without it must
        // surface as a validation failure, not an NPE logged as a generic error.
        if (raw.meta() == null || raw.meta().lastUpdatedAt() == null) {
            log.warn(MISSING_META_LOG);
            return CurrencyExchangeResponse.failure();
        }

        Map<Currency, BigDecimal> rates = new HashMap<>();

        raw.data().forEach((currencyCode, currencyData) -> {
            try {
                Currency currency = Currency.getInstance(currencyCode);
                rates.put(currency, currencyData.value());
            } catch (IllegalArgumentException e) {
                log.debug("Skipping unsupported currency: {}", currencyCode);
            }
        });

        return CurrencyExchangeResponse.success(
                Currency.getInstance("USD"),
                raw.meta().lastUpdatedAt().toLocalDate(),
                rates,
                false
        );
    }
}
