package com.example.cerps.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyCodeValidatorTest {

    private CurrencyCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CurrencyCodeValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF", "PLN", "CZK", "HUF", "BYN", "UAH"})
    void isValid_WithValidCurrencyCodes_ShouldReturnTrue(String code) {
        assertThat(validator.isValid(code, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", "eur", "gbp"})
    void isValid_WithLowercaseCodes_ShouldReturnTrue(String code) {
        assertThat(validator.isValid(code, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  USD  ", " EUR", "GBP "})
    void isValid_WithWhitespacePadding_ShouldReturnTrue(String code) {
        assertThat(validator.isValid(code, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isValid_WithNullOrEmpty_ShouldReturnFalse(String code) {
        assertThat(validator.isValid(code, null)).isFalse();
    }

    @Test
    void isValid_WithWhitespaceOnly_ShouldReturnFalse() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "ABC", "XYZ", "ABCDEF", "12", "U$D"})
    void isValid_WithInvalidCodes_ShouldReturnFalse(String code) {
        assertThat(validator.isValid(code, null)).isFalse();
    }
}
