package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.service.ExchangeRateProviderService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsValidRateTest {

    private ExchangeRateProviderService providerService;

    @BeforeEach
    void setUp() {
        ExchangeRateClient client = mock(ExchangeRateClient.class);
        when(client.getProviderName()).thenReturn("TestProvider");
        providerService = new ExchangeRateProviderService(List.of(client), new SimpleMeterRegistry());
        ReflectionTestUtils.setField(providerService, "baseCurrencyCode", "EUR");
    }

    private boolean invokeIsValidRate(BigDecimal rate) {
        return ReflectionTestUtils.invokeMethod(providerService, "isValidRate", rate);
    }

    // === Invalid rates ===

    @Test
    void isValidRate_WithZero_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(BigDecimal.ZERO)).isFalse();
    }

    @Test
    void isValidRate_WithNegative_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(-1))).isFalse();
    }

    @Test
    void isValidRate_WithNull_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(null)).isFalse();
    }

    @Test
    void isValidRate_WithVerySmallPositive_ShouldReturnTrue() {
        // BigDecimal cannot represent NaN, so test a near-zero value instead
        assertThat(invokeIsValidRate(new BigDecimal("0.0000000001"))).isTrue();
    }

    @Test
    void isValidRate_WithPositiveInfinity_ShouldReturnFalse() {
        // BigDecimal can't represent infinity directly, but a value >= MAX_RATE should be invalid
        assertThat(invokeIsValidRate(BigDecimal.valueOf(1_000_000))).isFalse();
    }

    @Test
    void isValidRate_WithValueAboveMaxRate_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(1_000_001))).isFalse();
    }

    @Test
    void isValidRate_WithExactlyMaxRate_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(1_000_000))).isFalse();
    }

    @Test
    void isValidRate_WithVeryLargeNegative_ShouldReturnFalse() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(-999999))).isFalse();
    }

    // === Valid rates ===

    @Test
    void isValidRate_WithHalf_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(0.5))).isTrue();
    }

    @Test
    void isValidRate_With1000_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(BigDecimal.valueOf(1000))).isTrue();
    }

    @Test
    void isValidRate_WithOne_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(BigDecimal.ONE)).isTrue();
    }

    @Test
    void isValidRate_WithSmallPositive_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(new BigDecimal("0.000001"))).isTrue();
    }

    @Test
    void isValidRate_WithTypicalEurUsdRate_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(new BigDecimal("1.18"))).isTrue();
    }

    @Test
    void isValidRate_WithTypicalEurJpyRate_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(new BigDecimal("130.5"))).isTrue();
    }

    @Test
    void isValidRate_JustBelowMaxRate_ShouldReturnTrue() {
        assertThat(invokeIsValidRate(new BigDecimal("999999.99"))).isTrue();
    }
}
