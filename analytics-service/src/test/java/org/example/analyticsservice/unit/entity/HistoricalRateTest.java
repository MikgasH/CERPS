package org.example.analyticsservice.unit.entity;

import org.example.analyticsservice.entity.HistoricalRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalRateTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void builder_ShouldPopulateAllFields_WhenAllValuesProvided() {
        final HistoricalRate rate = HistoricalRate.builder()
                .id(1L)
                .baseCurrency(EUR)
                .targetCurrency(USD)
                .rateDate(LocalDate.of(2026, 6, 12))
                .rate(new BigDecimal("1.0851234567"))
                .fetchedAt(Instant.parse("2026-06-12T10:00:00Z"))
                .build();

        assertThat(rate.getId()).isEqualTo(1L);
        assertThat(rate.getBaseCurrency()).isEqualTo(EUR);
        assertThat(rate.getTargetCurrency()).isEqualTo(USD);
        assertThat(rate.getRateDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(rate.getRate()).isEqualByComparingTo("1.0851234567");
        assertThat(rate.getFetchedAt()).isEqualTo(Instant.parse("2026-06-12T10:00:00Z"));
    }

    @Test
    void setters_ShouldUpdateFields_WhenCalledOnEmptyInstance() {
        final HistoricalRate rate = new HistoricalRate();
        rate.setId(2L);
        rate.setBaseCurrency(EUR);
        rate.setTargetCurrency(USD);
        rate.setRateDate(LocalDate.of(2026, 6, 11));
        rate.setRate(new BigDecimal("1.09"));
        rate.setFetchedAt(Instant.parse("2026-06-11T16:00:00Z"));

        assertThat(rate.getId()).isEqualTo(2L);
        assertThat(rate.getRate()).isEqualByComparingTo("1.09");
    }

    @Test
    void equalsAndHashCode_ShouldBeConsistent_WhenComparedToSameTypeInstance() {
        final HistoricalRate first = HistoricalRate.builder().id(1L).build();
        final HistoricalRate second = HistoricalRate.builder().id(2L).build();

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(new Object());
    }

    @Test
    void toString_ShouldContainFieldValues_WhenEntityIsPopulated() {
        final HistoricalRate rate = HistoricalRate.builder()
                .targetCurrency(USD)
                .rateDate(LocalDate.of(2026, 6, 12))
                .build();

        assertThat(rate.toString())
                .contains("USD")
                .contains("2026-06-12");
    }
}
