/*package com.example.cerpshashkin.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PeriodValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private PeriodValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PeriodValidator();
    }

    @Test
    void isValid_WithValidHoursPeriod_ShouldReturnTrue() {
        assertThat(validator.isValid("12H", context)).isTrue();
        assertThat(validator.isValid("24H", context)).isTrue();
        assertThat(validator.isValid("168H", context)).isTrue();
        assertThat(validator.isValid("720H", context)).isTrue();
        assertThat(validator.isValid("8760H", context)).isTrue();
    }

    @Test
    void isValid_WithValidDaysPeriod_ShouldReturnTrue() {
        assertThat(validator.isValid("1D", context)).isTrue();
        assertThat(validator.isValid("7D", context)).isTrue();
        assertThat(validator.isValid("30D", context)).isTrue();
        assertThat(validator.isValid("90D", context)).isTrue();
        assertThat(validator.isValid("365D", context)).isTrue();
    }

    @Test
    void isValid_WithValidMonthsPeriod_ShouldReturnTrue() {
        assertThat(validator.isValid("1M", context)).isTrue();
        assertThat(validator.isValid("3M", context)).isTrue();
        assertThat(validator.isValid("6M", context)).isTrue();
        assertThat(validator.isValid("12M", context)).isTrue();
    }

    @Test
    void isValid_WithValidYearPeriod_ShouldReturnTrue() {
        assertThat(validator.isValid("1Y", context)).isTrue();
    }

    @Test
    void isValid_WithLowercaseUnits_ShouldReturnTrue() {
        assertThat(validator.isValid("24h", context)).isTrue();
        assertThat(validator.isValid("7d", context)).isTrue();
        assertThat(validator.isValid("3m", context)).isTrue();
        assertThat(validator.isValid("1y", context)).isTrue();
    }

    @Test
    void isValid_WithWhitespace_ShouldReturnTrue() {
        assertThat(validator.isValid("  7D  ", context)).isTrue();
        assertThat(validator.isValid(" 30D ", context)).isTrue();
    }

    @Test
    void isValid_WithTooFewHours_ShouldReturnFalse() {
        assertThat(validator.isValid("1H", context)).isFalse();
        assertThat(validator.isValid("11H", context)).isFalse();
    }

    @Test
    void isValid_WithTooManyHours_ShouldReturnFalse() {
        assertThat(validator.isValid("8761H", context)).isFalse();
        assertThat(validator.isValid("10000H", context)).isFalse();
    }

    @Test
    void isValid_WithTooManyDays_ShouldReturnFalse() {
        assertThat(validator.isValid("366D", context)).isFalse();
        assertThat(validator.isValid("1000D", context)).isFalse();
    }

    @Test
    void isValid_WithTooManyMonths_ShouldReturnFalse() {
        assertThat(validator.isValid("13M", context)).isFalse();
        assertThat(validator.isValid("24M", context)).isFalse();
    }

    @Test
    void isValid_WithTooManyYears_ShouldReturnFalse() {
        assertThat(validator.isValid("2Y", context)).isFalse();
        assertThat(validator.isValid("5Y", context)).isFalse();
    }

    @Test
    void isValid_WithZeroPeriod_ShouldReturnFalse() {
        assertThat(validator.isValid("0H", context)).isFalse();
        assertThat(validator.isValid("0D", context)).isFalse();
        assertThat(validator.isValid("0M", context)).isFalse();
        assertThat(validator.isValid("0Y", context)).isFalse();
    }

    @Test
    void isValid_WithNegativePeriod_ShouldReturnFalse() {
        assertThat(validator.isValid("-1H", context)).isFalse();
        assertThat(validator.isValid("-7D", context)).isFalse();
    }

    @Test
    void isValid_WithInvalidFormat_ShouldReturnFalse() {
        assertThat(validator.isValid("7", context)).isFalse();
        assertThat(validator.isValid("D7", context)).isFalse();
        assertThat(validator.isValid("7DD", context)).isFalse();
        assertThat(validator.isValid("7 D", context)).isFalse();
    }

    @Test
    void isValid_WithInvalidUnit_ShouldReturnFalse() {
        assertThat(validator.isValid("7W", context)).isFalse();
        assertThat(validator.isValid("7S", context)).isFalse();
        assertThat(validator.isValid("7X", context)).isFalse();
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
    }

    @Test
    void isValid_WithOnlyUnit_ShouldReturnFalse() {
        assertThat(validator.isValid("D", context)).isFalse();
        assertThat(validator.isValid("H", context)).isFalse();
    }

    @Test
    void isValid_WithDecimalNumber_ShouldReturnFalse() {
        assertThat(validator.isValid("7.5D", context)).isFalse();
        assertThat(validator.isValid("1.5M", context)).isFalse();
    }

    @Test
    void isValid_WithTooLargeNumber_ShouldReturnFalse() {
        assertThat(validator.isValid("9999999999D", context)).isFalse();
    }

    @Test
    void isValid_WithSpecialCharacters_ShouldReturnFalse() {
        assertThat(validator.isValid("7@D", context)).isFalse();
        assertThat(validator.isValid("7#H", context)).isFalse();
    }
}*/
