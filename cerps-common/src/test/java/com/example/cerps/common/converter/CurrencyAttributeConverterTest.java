package com.example.cerps.common.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyAttributeConverterTest {

    private CurrencyAttributeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CurrencyAttributeConverter();
    }

    // === convertToDatabaseColumn ===

    @Test
    void convertToDatabaseColumn_WithValidCurrency_ShouldReturnCode() {
        Currency usd = Currency.getInstance("USD");
        assertThat(converter.convertToDatabaseColumn(usd)).isEqualTo("USD");
    }

    @Test
    void convertToDatabaseColumn_WithEur_ShouldReturnEUR() {
        Currency eur = Currency.getInstance("EUR");
        assertThat(converter.convertToDatabaseColumn(eur)).isEqualTo("EUR");
    }

    @Test
    void convertToDatabaseColumn_WithNull_ShouldReturnNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    // === convertToEntityAttribute ===

    @Test
    void convertToEntityAttribute_WithValidCode_ShouldReturnCurrency() {
        Currency result = converter.convertToEntityAttribute("USD");
        assertThat(result).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void convertToEntityAttribute_WithEur_ShouldReturnEurCurrency() {
        Currency result = converter.convertToEntityAttribute("EUR");
        assertThat(result).isEqualTo(Currency.getInstance("EUR"));
    }

    @Test
    void convertToEntityAttribute_WithNull_ShouldReturnNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_WithEmptyString_ShouldReturnNull() {
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    @Test
    void convertToEntityAttribute_WithBlankString_ShouldReturnNull() {
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void convertToEntityAttribute_WithInvalidCode_ShouldReturnNull() {
        assertThat(converter.convertToEntityAttribute("INVALID")).isNull();
    }

    @Test
    void convertToEntityAttribute_WithPaddedCode_ShouldReturnCurrency() {
        Currency result = converter.convertToEntityAttribute(" USD ");
        assertThat(result).isEqualTo(Currency.getInstance("USD"));
    }

    // === Roundtrip ===

    @Test
    void roundTrip_ShouldPreserveCurrency() {
        Currency original = Currency.getInstance("GBP");
        String dbValue = converter.convertToDatabaseColumn(original);
        Currency restored = converter.convertToEntityAttribute(dbValue);
        assertThat(restored).isEqualTo(original);
    }
}
