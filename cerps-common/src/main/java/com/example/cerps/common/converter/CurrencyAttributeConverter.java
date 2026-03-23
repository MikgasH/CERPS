package com.example.cerps.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;


@Converter(autoApply = true)
public class CurrencyAttributeConverter implements AttributeConverter<Currency, String> {

    private static final Logger log = LoggerFactory.getLogger(CurrencyAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(final Currency currency) {
        return currency != null ? currency.getCurrencyCode() : null;
    }

    @Override
    public Currency convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return Currency.getInstance(dbData.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid currency code '{}' found in database — returning null fallback", dbData);
            return null;
        }
    }
}
