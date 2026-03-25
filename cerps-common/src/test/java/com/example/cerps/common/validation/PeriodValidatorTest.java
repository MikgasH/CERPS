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

    // === Valid periods ===

    @ParameterizedTest
    @ValueSource(strings = {"1D", "7D", "30D", "90D", "180D", "365D"})
    void isValid_WithValidDayPeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12H", "24H", "168H", "8760H"})
    void isValid_WithValidHourPeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1M", "6M", "12M"})
    void isValid_WithValidMonthPeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @Test
    void isValid_WithOneYear_ShouldReturnTrue() {
        assertThat(validator.isValid("1Y", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1d", "7d", "12h", "1m", "1y"})
    void isValid_WithLowercasePeriods_ShouldReturnTrue(String period) {
        assertThat(validator.isValid(period, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  1D  ", " 7D", "30D "})
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
    @ValueSource(strings = {"0D", "0M", "0H"})
    void isValid_WithZeroAmount_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }

    @Test
    void isValid_WithTooFewHours_ShouldReturnFalse() {
        assertThat(validator.isValid("11H", null)).isFalse();
    }

    @Test
    void isValid_WithTooManyHours_ShouldReturnFalse() {
        assertThat(validator.isValid("8761H", null)).isFalse();
    }

    @Test
    void isValid_WithTooManyDays_ShouldReturnFalse() {
        assertThat(validator.isValid("366D", null)).isFalse();
    }

    @Test
    void isValid_WithTooManyMonths_ShouldReturnFalse() {
        assertThat(validator.isValid("13M", null)).isFalse();
    }

    @Test
    void isValid_WithTwoYears_ShouldReturnFalse() {
        assertThat(validator.isValid("2Y", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "7X", "D7", "30", "D", "1.5D", "-1D"})
    void isValid_WithInvalidFormat_ShouldReturnFalse(String period) {
        assertThat(validator.isValid(period, null)).isFalse();
    }
}
