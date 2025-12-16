package org.example.userservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
    }

    @Test
    void testPasswordEncoding() {
        String rawPassword = "MySecurePassword123!";

        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void testDifferentSalts() {
        // КРИТИЧНО! Доказывает защиту от rainbow tables
        String password = "SamePassword123!";

        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        assertNotEquals(hash1, hash2); // Different salts
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
    }

    @Test
    void testWrongPasswordFails() {
        String correctPassword = "Correct123!";
        String wrongPassword = "Wrong456!";

        String hash = passwordEncoder.encode(correctPassword);

        assertFalse(passwordEncoder.matches(wrongPassword, hash));
    }

    @Test
    void testPerformanceBenchmark() {
        String password = "TestPassword123!";
        int iterations = 10;

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            passwordEncoder.encode(password);
        }
        long end = System.currentTimeMillis();

        long avgTime = (end - start) / iterations;

        assertTrue(avgTime > 100,
                "BCrypt should take > 100ms (cost=12), but took " + avgTime + "ms");
        assertTrue(avgTime < 500,
                "BCrypt should take < 500ms for UX, but took " + avgTime + "ms");
    }
}
