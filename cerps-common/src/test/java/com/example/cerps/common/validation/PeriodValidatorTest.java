package com.example.cerps.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodValidatorTest {

    private PeriodValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PeriodValidator();
    }

    // === Valid periods (documented allow-list only) ===

    @ParameterizedTest
    @ValueSource(strings = {"1D", "7D", "30D", "90D", "180D", "1Y"})
    void isValid_WithAllowedPeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1d", "7d", "30d", "90d", "180d", "1y"})
    void isValid_WithLowercasePeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  1D  ", " 7D", "30D ", "  1Y  "})
    void isValid_WithWhitespacePadding_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    // === Invalid periods ===

    @ParameterizedTest
    @NullAndEmptySource
    void isValid_WithNullOrEmpty_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @Test
    void isValid_WithWhitespaceOnly_ShouldReturnFalse() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12H", "24H", "168H", "8760H"})
    void isValid_WithHourPeriods_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1M", "6M", "12M"})
    void isValid_WithMonthPeriods_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2D", "14D", "60D", "365D", "2Y", "10Y"})
    void isValid_WithUndocumentedPeriods_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0D", "0M", "0H", "-1D"})
    void isValid_WithZeroOrNegative_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "7X", "D7", "30", "D", "1.5D"})
    void isValid_WithInvalidFormat_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }
}
