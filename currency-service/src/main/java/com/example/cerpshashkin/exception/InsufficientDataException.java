package com.example.cerpshashkin.exception;

public class InsufficientDataException extends CurrencyServiceException {
    public InsufficientDataException(final String message) {
        super(message);
    }
}
