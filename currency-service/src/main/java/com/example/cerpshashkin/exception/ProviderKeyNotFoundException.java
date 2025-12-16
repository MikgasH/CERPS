package com.example.cerpshashkin.exception;

public class ProviderKeyNotFoundException extends RuntimeException {
    private static final String MESSAGE = "Provider key not found with id: %d";

    public ProviderKeyNotFoundException(final Long id) {
        super(String.format(MESSAGE, id));
    }
}
