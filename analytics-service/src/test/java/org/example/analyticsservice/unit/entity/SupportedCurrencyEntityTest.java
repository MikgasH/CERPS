package org.example.analyticsservice.unit.entity;

import org.example.analyticsservice.entity.SupportedCurrencyEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportedCurrencyEntityTest {

    @Test
    void builder_ShouldCreateEntityWithAllFields() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .id(1L)
                .currencyCode("USD")
                .build();

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyEntity() {
        SupportedCurrencyEntity entity = new SupportedCurrencyEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCurrencyCode()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldSetAllFields() {
        SupportedCurrencyEntity entity = new SupportedCurrencyEntity(1L, "EUR");

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        SupportedCurrencyEntity entity = new SupportedCurrencyEntity();

        entity.setId(2L);
        entity.setCurrencyCode("GBP");

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getCurrencyCode()).isEqualTo("GBP");
    }

    @Test
    void hashCode_ShouldBeConsistent() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .id(1L)
                .currencyCode("USD")
                .build();

        int hashCode1 = entity.hashCode();
        int hashCode2 = entity.hashCode();

        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    @Test
    void toString_ShouldContainAllFields() {
        SupportedCurrencyEntity entity = SupportedCurrencyEntity.builder()
                .id(1L)
                .currencyCode("USD")
                .build();

        String toString = entity.toString();

        assertThat(toString).contains("1");
        assertThat(toString).contains("USD");
    }
}

