package com.example.cerpshashkin.exception;

public class CurrencyServiceException extends RuntimeException {
    public CurrencyServiceException(final String message) {
        super(message);
    }

    public CurrencyServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
