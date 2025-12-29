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

    String message() default "Invalid period format. Use: 12H-8760H, 1D-365D, 1M-12M, or 1Y";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
