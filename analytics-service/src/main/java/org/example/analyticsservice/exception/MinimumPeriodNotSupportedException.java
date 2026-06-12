package org.example.analyticsservice.exception;

import lombok.Getter;

@Getter
public class MinimumPeriodNotSupportedException extends RuntimeException {

    private final String minimumPeriod;

    public MinimumPeriodNotSupportedException(final String message, final String minimumPeriod) {
        super(message);
        this.minimumPeriod = minimumPeriod;
    }
}
