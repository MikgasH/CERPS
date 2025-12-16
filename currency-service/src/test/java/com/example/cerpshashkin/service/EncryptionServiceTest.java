package com.example.cerpshashkin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private static final String TEST_MASTER_KEY = "xtCPEriABFzNjq7KmLK5BmGt8vbWPq0PcB1C7Y8DxNo=";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_MASTER_KEY);
    }

    @Test
    void testEncryptDecrypt() {
        String plaintext = "my-secret-api-key-12345";

        String encrypted = encryptionService.encrypt(plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testDifferentIVs() {
        String plaintext = "same-text-for-encryption";

        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2);

        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testTamperedCiphertext() {
        String plaintext = "original-data";
        String encrypted = encryptionService.encrypt(plaintext);

        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[encryptedBytes.length - 1] ^= 0x01;
        String tamperedEncrypted = Base64.getEncoder().encodeToString(encryptedBytes);

        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(tamperedEncrypted));
    }

    @Test
    void testPerformanceBenchmark() {
        String plaintext = "performance-test-api-key";
        int iterations = 1000;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            encryptionService.encrypt(plaintext);
        }
        long endTime = System.nanoTime();

        long averageTimeNanos = (endTime - startTime) / iterations;
        long averageTimeMillis = averageTimeNanos / 1_000_000;

        assertTrue(averageTimeMillis < 1,
            "Average encryption time should be less than 1ms, but was " + averageTimeMillis + "ms");
    }

    @Test
    void testEmptyString() {
        String plaintext = "";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testLongString() {
        String plaintext = "a".repeat(10000);

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void testSpecialCharacters() {
        String plaintext = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~АБВГабвг中文";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }
}
