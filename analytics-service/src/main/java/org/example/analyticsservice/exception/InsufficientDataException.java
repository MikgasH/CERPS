package org.example.analyticsservice.exception;

public class InsufficientDataException extends RuntimeException {
    public InsufficientDataException(final String message) {
        super(message);
    }
}
