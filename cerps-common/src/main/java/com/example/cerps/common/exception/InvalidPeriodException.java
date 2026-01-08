package com.example.cerps.common.exception;

public class InvalidPeriodException extends RuntimeException {

    public InvalidPeriodException(String message) {
        super(message);
    }

    public InvalidPeriodException(String message, Throwable cause) {
        super(message, cause);
    }
}
