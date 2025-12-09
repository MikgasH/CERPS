package com.example.cerpshashkin.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Currency;

@Converter(autoApply = true)
public class CurrencyAttributeConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(final Currency currency) {
        return currency != null ? currency.getCurrencyCode() : null;
    }

    @Override
    public Currency convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        return Currency.getInstance(dbData.trim());
    }
}
