package org.example.analyticsservice.unit.entity;

import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateEntityTest {

    @Test
    void builder_ShouldCreateEntityWithAllFields() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BigDecimal rate = new BigDecimal("1.23");

        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(rate)
                .source("TEST")
                .timestamp(timestamp)
                .build();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getBaseCurrency()).isEqualTo("USD");
        assertThat(entity.getTargetCurrency()).isEqualTo("EUR");
        assertThat(entity.getRate()).isEqualTo(rate);
        assertThat(entity.getSource()).isEqualTo("TEST");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyEntity() {
        ExchangeRateEntity entity = new ExchangeRateEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getBaseCurrency()).isNull();
        assertThat(entity.getTargetCurrency()).isNull();
        assertThat(entity.getRate()).isNull();
        assertThat(entity.getSource()).isNull();
        assertThat(entity.getTimestamp()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldSetAllFields() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BigDecimal rate = new BigDecimal("1.23");

        ExchangeRateEntity entity = new ExchangeRateEntity(
                id, "USD", "EUR", rate, "TEST", timestamp
        );

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getBaseCurrency()).isEqualTo("USD");
        assertThat(entity.getTargetCurrency()).isEqualTo("EUR");
        assertThat(entity.getRate()).isEqualTo(rate);
        assertThat(entity.getSource()).isEqualTo("TEST");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BigDecimal rate = new BigDecimal("1.23");

        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(id);
        entity.setBaseCurrency("GBP");
        entity.setTargetCurrency("JPY");
        entity.setRate(rate);
        entity.setSource("API");
        entity.setTimestamp(timestamp);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getBaseCurrency()).isEqualTo("GBP");
        assertThat(entity.getTargetCurrency()).isEqualTo("JPY");
        assertThat(entity.getRate()).isEqualTo(rate);
        assertThat(entity.getSource()).isEqualTo("API");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void equals_ShouldConsiderOnlyId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Instant timestamp = Instant.now();

        ExchangeRateEntity entity1 = ExchangeRateEntity.builder()
                .id(id1)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST1")
                .timestamp(timestamp)
                .build();

        ExchangeRateEntity entity2 = ExchangeRateEntity.builder()
                .id(id1)
                .baseCurrency("GBP")
                .targetCurrency("JPY")
                .rate(new BigDecimal("150.00"))
                .source("TEST2")
                .timestamp(timestamp)
                .build();

        ExchangeRateEntity entity3 = ExchangeRateEntity.builder()
                .id(id2)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST1")
                .timestamp(timestamp)
                .build();

        // Same ID should be equal
        assertThat(entity1).isEqualTo(entity2);
        // Different ID should not be equal
        assertThat(entity1).isNotEqualTo(entity3);
        // Same object should be equal to itself
        assertThat(entity1).isEqualTo(entity1);
    }

    @Test
    void hashCode_ShouldBeBasedOnId() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();

        ExchangeRateEntity entity1 = ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST1")
                .timestamp(timestamp)
                .build();

        ExchangeRateEntity entity2 = ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency("GBP")
                .targetCurrency("JPY")
                .rate(new BigDecimal("150.00"))
                .source("TEST2")
                .timestamp(timestamp)
                .build();

        // Same ID should have same hashCode
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void hashCode_ShouldBeConsistent() {
        UUID id = UUID.randomUUID();
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST")
                .timestamp(Instant.now())
                .build();

        int hashCode1 = entity.hashCode();
        int hashCode2 = entity.hashCode();

        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    @Test
    void toString_ShouldContainAllFields() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BigDecimal rate = new BigDecimal("1.23");

        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(rate)
                .source("TEST")
                .timestamp(timestamp)
                .build();

        String toString = entity.toString();

        assertThat(toString).contains(id.toString());
        assertThat(toString).contains("USD");
        assertThat(toString).contains("EUR");
        assertThat(toString).contains("1.23");
        assertThat(toString).contains("TEST");
    }

    @Test
    void equals_WithNull_ShouldReturnFalse() {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST")
                .timestamp(Instant.now())
                .build();

        assertThat(entity).isNotEqualTo(null);
    }

    @Test
    void equals_WithDifferentClass_ShouldReturnFalse() {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.23"))
                .source("TEST")
                .timestamp(Instant.now())
                .build();

        assertThat(entity).isNotEqualTo("Not an ExchangeRateEntity");
    }
}

