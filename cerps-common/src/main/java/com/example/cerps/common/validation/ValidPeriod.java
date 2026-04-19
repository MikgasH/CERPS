package com.example.cerps.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PeriodValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPeriod {

    String message() default "Invalid period. Allowed values: 1D, 7D, 30D, 90D, 180D, 1Y";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
