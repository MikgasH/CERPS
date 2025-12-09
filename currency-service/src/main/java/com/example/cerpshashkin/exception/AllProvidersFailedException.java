package com.example.cerpshashkin.exception;

import java.util.List;

public class AllProvidersFailedException extends CurrencyServiceException {
    private static final String MESSAGE = "All exchange rate providers failed: %s";

    public AllProvidersFailedException(final List<String> failedProviders) {
        super(String.format(MESSAGE, String.join(", ", failedProviders)));
    }
}
