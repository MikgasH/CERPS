package org.example.analyticsservice.unit.entity;

import org.example.analyticsservice.entity.HistoricalCoverage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalCoverageTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void builder_ShouldPopulateAllFields_WhenAllValuesProvided() {
        final HistoricalCoverage coverage = HistoricalCoverage.builder()
                .targetCurrency(USD)
                .coveredFrom(LocalDate.of(2023, 6, 12))
                .coveredTo(LocalDate.of(2026, 6, 12))
                .updatedAt(Instant.parse("2026-06-12T10:00:00Z"))
                .build();

        assertThat(coverage.getTargetCurrency()).isEqualTo(USD);
        assertThat(coverage.getCoveredFrom()).isEqualTo(LocalDate.of(2023, 6, 12));
        assertThat(coverage.getCoveredTo()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(coverage.getUpdatedAt()).isEqualTo(Instant.parse("2026-06-12T10:00:00Z"));
    }

    @Test
    void setters_ShouldUpdateFields_WhenCalledOnEmptyInstance() {
        final HistoricalCoverage coverage = new HistoricalCoverage();
        coverage.setTargetCurrency(USD);
        coverage.setCoveredFrom(LocalDate.of(2024, 1, 1));
        coverage.setCoveredTo(LocalDate.of(2026, 1, 1));
        coverage.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(coverage.getTargetCurrency()).isEqualTo(USD);
        assertThat(coverage.getCoveredFrom()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void equalsAndHashCode_ShouldBeConsistent_WhenComparedToSameTypeInstance() {
        final HistoricalCoverage first = HistoricalCoverage.builder().targetCurrency(USD).build();
        final HistoricalCoverage second = HistoricalCoverage.builder().build();

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(new Object());
    }

    @Test
    void toString_ShouldContainFieldValues_WhenEntityIsPopulated() {
        final HistoricalCoverage coverage = HistoricalCoverage.builder()
                .targetCurrency(USD)
                .coveredFrom(LocalDate.of(2023, 6, 12))
                .build();

        assertThat(coverage.toString())
                .contains("USD")
                .contains("2023-06-12");
    }
}
