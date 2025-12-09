package com.example.cerps.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class PeriodValidator implements ConstraintValidator<ValidPeriod, String> {

    private static final Set<String> VALID_PERIODS = Set.of("WEEK", "MONTH", "YEAR");

    @Override
    public void initialize(ValidPeriod constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        String upperCaseValue = value.toUpperCase().trim();
        return VALID_PERIODS.contains(upperCaseValue);
    }
}
