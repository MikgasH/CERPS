/*package com.example.cerpshashkin.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyCodeValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private CurrencyCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CurrencyCodeValidator();
    }

    @Test
    void isValid_WithValidCurrencyCode_ShouldReturnTrue() {
        assertThat(validator.isValid("USD", context)).isTrue();
        assertThat(validator.isValid("EUR", context)).isTrue();
        assertThat(validator.isValid("GBP", context)).isTrue();
        assertThat(validator.isValid("JPY", context)).isTrue();
        assertThat(validator.isValid("CHF", context)).isTrue();
        assertThat(validator.isValid("CAD", context)).isTrue();
    }

    @Test
    void isValid_WithLowercaseCurrency_ShouldReturnTrue() {
        assertThat(validator.isValid("usd", context)).isTrue();
        assertThat(validator.isValid("eur", context)).isTrue();
        assertThat(validator.isValid("gbp", context)).isTrue();
    }

    @Test
    void isValid_WithMixedCaseCurrency_ShouldReturnTrue() {
        assertThat(validator.isValid("Usd", context)).isTrue();
        assertThat(validator.isValid("EuR", context)).isTrue();
        assertThat(validator.isValid("GbP", context)).isTrue();
    }

    @Test
    void isValid_WithCurrencyAndWhitespace_ShouldReturnTrue() {
        assertThat(validator.isValid("  USD  ", context)).isTrue();
        assertThat(validator.isValid("EUR ", context)).isTrue();
        assertThat(validator.isValid(" GBP", context)).isTrue();
    }

    @Test
    void isValid_WithInvalidCurrencyCode_ShouldReturnFalse() {
        assertThat(validator.isValid("INVALID", context)).isFalse();
        assertThat(validator.isValid("ZZZ", context)).isFalse();
        assertThat(validator.isValid("123", context)).isFalse();
        assertThat(validator.isValid("FAKE", context)).isFalse();
    }

    @Test
    void isValid_WithNullValue_ShouldReturnFalse() {
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    void isValid_WithEmptyString_ShouldReturnFalse() {
        assertThat(validator.isValid("", context)).isFalse();
    }

    @Test
    void isValid_WithBlankString_ShouldReturnFalse() {
        assertThat(validator.isValid("   ", context)).isFalse();
        assertThat(validator.isValid("\t", context)).isFalse();
        assertThat(validator.isValid("\n", context)).isFalse();
    }

    @Test
    void isValid_WithTooShortCode_ShouldReturnFalse() {
        assertThat(validator.isValid("US", context)).isFalse();
        assertThat(validator.isValid("E", context)).isFalse();
        assertThat(validator.isValid("A", context)).isFalse();
    }

    @Test
    void isValid_WithTooLongCode_ShouldReturnFalse() {
        assertThat(validator.isValid("USDD", context)).isFalse();
        assertThat(validator.isValid("EURO", context)).isFalse();
        assertThat(validator.isValid("DOLLAR", context)).isFalse();
    }

    @Test
    void isValid_WithSpecialCharacters_ShouldReturnFalse() {
        assertThat(validator.isValid("US$", context)).isFalse();
        assertThat(validator.isValid("EU@", context)).isFalse();
        assertThat(validator.isValid("GB#", context)).isFalse();
    }

    @Test
    void isValid_WithNumbers_ShouldReturnFalse() {
        assertThat(validator.isValid("US1", context)).isFalse();
        assertThat(validator.isValid("123", context)).isFalse();
        assertThat(validator.isValid("1USD", context)).isFalse();
    }
}*/
