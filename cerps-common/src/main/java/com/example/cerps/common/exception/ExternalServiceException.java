package com.example.cerps.common.exception;

public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(final String message) {
        super(message);
    }

    public ExternalServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
