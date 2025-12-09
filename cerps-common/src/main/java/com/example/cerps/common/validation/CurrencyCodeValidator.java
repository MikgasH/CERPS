package com.example.cerps.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrency, String> {

    private static final Set<String> VALID_CURRENCY_CODES = Currency.getAvailableCurrencies()
            .stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toSet());

    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        String upperCaseValue = value.toUpperCase().trim();
        return VALID_CURRENCY_CODES.contains(upperCaseValue);
    }
}
