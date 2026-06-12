package org.example.analyticsservice.integration.repository;

import org.example.analyticsservice.entity.HistoricalCoverage;
import org.example.analyticsservice.repository.HistoricalCoverageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HistoricalCoverageRepositoryIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency PLN = Currency.getInstance("PLN");

    @Autowired
    private HistoricalCoverageRepository repository;

    @Test
    void save_ShouldPersistCoverage_WhenEntityIsValid() {
        repository.saveAndFlush(coverage(USD, LocalDate.of(2023, 6, 12), LocalDate.of(2026, 6, 12)));

        assertThat(repository.findById(USD))
                .hasValueSatisfying(found -> {
                    assertThat(found.getCoveredFrom()).isEqualTo(LocalDate.of(2023, 6, 12));
                    assertThat(found.getCoveredTo()).isEqualTo(LocalDate.of(2026, 6, 12));
                    assertThat(found.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    void findById_ShouldReturnEmpty_WhenCurrencyNeverFetched() {
        repository.saveAndFlush(coverage(USD, LocalDate.of(2023, 6, 12), LocalDate.of(2026, 6, 12)));

        assertThat(repository.findById(PLN)).isEmpty();
    }

    @Test
    void save_ShouldExtendCoverage_WhenIntervalUpdated() {
        repository.saveAndFlush(coverage(USD, LocalDate.of(2023, 6, 12), LocalDate.of(2026, 6, 11)));

        final HistoricalCoverage existing = repository.findById(USD).orElseThrow();
        existing.setCoveredTo(LocalDate.of(2026, 6, 12));
        existing.setUpdatedAt(Instant.parse("2026-06-12T16:00:00Z"));
        repository.saveAndFlush(existing);

        assertThat(repository.findById(USD))
                .hasValueSatisfying(found ->
                        assertThat(found.getCoveredTo()).isEqualTo(LocalDate.of(2026, 6, 12)));
        assertThat(repository.count()).isEqualTo(1);
    }

    private HistoricalCoverage coverage(final Currency target, final LocalDate from, final LocalDate to) {
        return HistoricalCoverage.builder()
                .targetCurrency(target)
                .coveredFrom(from)
                .coveredTo(to)
                .updatedAt(Instant.parse("2026-06-12T10:00:00Z"))
                .build();
    }
}
