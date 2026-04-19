package com.example.cerps.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class PeriodValidator implements ConstraintValidator<ValidPeriod, String> {

    private static final Set<String> ALLOWED_PERIODS = Set.of("1D", "7D", "30D", "90D", "180D", "1Y");

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        final String normalized = value.trim().toUpperCase();
        return ALLOWED_PERIODS.contains(normalized);
    }
}
