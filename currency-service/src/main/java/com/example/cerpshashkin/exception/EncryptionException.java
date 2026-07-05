package com.example.cerpshashkin.exception;

/**
 * AES-GCM encryption/decryption of a provider API key failed — an internal
 * error (bad master key, corrupted ciphertext), never caused by client input.
 * Handled by the generic 500 handler.
 */
public class EncryptionException extends CurrencyServiceException {
    public EncryptionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
