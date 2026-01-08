package com.example.cerps.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PeriodValidator implements ConstraintValidator<ValidPeriod, String> {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{1,9}[HDMY]$");

    private static final int MIN_HOURS = 12;
    private static final int MAX_HOURS = 8760;
    private static final int MAX_DAYS = 365;
    private static final int MAX_MONTHS = 12;
    private static final int MAX_YEARS = 1;

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        final String trimmed = value.trim().toUpperCase();

        if (!PERIOD_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        try {
            final int amount = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));

            if (amount <= 0) {
                return false;
            }

            return switch (trimmed.charAt(trimmed.length() - 1)) {
                case 'H' -> amount >= MIN_HOURS && amount <= MAX_HOURS;
                case 'D' -> amount <= MAX_DAYS;
                case 'M' -> amount <= MAX_MONTHS;
                case 'Y' -> amount == MAX_YEARS;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
