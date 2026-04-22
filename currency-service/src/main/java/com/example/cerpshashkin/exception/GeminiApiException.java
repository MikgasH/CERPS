package com.example.cerpshashkin.exception;

public class GeminiApiException extends RuntimeException {

    public GeminiApiException(final String message) {
        super(message);
    }

    public GeminiApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
