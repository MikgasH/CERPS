package com.example.cerpshashkin.exception;

public class ProviderKeyNotFoundByNameException extends RuntimeException {
    private static final String MESSAGE = "Active provider key not found for provider: %s";

    public ProviderKeyNotFoundByNameException(final String providerName) {
        super(String.format(MESSAGE, providerName));
    }
}
