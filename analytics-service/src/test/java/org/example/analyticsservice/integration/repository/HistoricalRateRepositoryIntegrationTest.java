package org.example.analyticsservice.integration.repository;

import org.example.analyticsservice.entity.HistoricalRate;
import org.example.analyticsservice.repository.HistoricalRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class HistoricalRateRepositoryIntegrationTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency PLN = Currency.getInstance("PLN");
    private static final Currency GBP = Currency.getInstance("GBP");

    private static final LocalDate DAY_1 = LocalDate.of(2026, 6, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 6, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 6, 3);

    @Autowired
    private HistoricalRateRepository repository;

    @Test
    void save_ShouldPersistRateWithGeneratedId_WhenEntityIsValid() {
        final HistoricalRate saved = repository.saveAndFlush(rate(USD, DAY_1, "1.0851234567"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getBaseCurrency()).isEqualTo(EUR);
                    assertThat(found.getTargetCurrency()).isEqualTo(USD);
                    assertThat(found.getRateDate()).isEqualTo(DAY_1);
                    assertThat(found.getRate()).isEqualByComparingTo("1.0851234567");
                    assertThat(found.getFetchedAt()).isNotNull();
                });
    }

    @Test
    void findByDerivedQuery_ShouldReturnMatchingRowsOrderedByDate_WhenWindowAndTargetsMatch() {
        repository.save(rate(USD, DAY_3, "1.09"));
        repository.save(rate(USD, DAY_1, "1.08"));
        repository.save(rate(PLN, DAY_2, "4.30"));
        repository.saveAndFlush(rate(GBP, DAY_2, "0.85"));

        final List<HistoricalRate> result = repository
                .findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                        EUR, Set.of(USD, PLN), DAY_1, DAY_3);

        assertThat(result)
                .hasSize(3)
                .extracting(HistoricalRate::getRateDate)
                .containsExactly(DAY_1, DAY_2, DAY_3);
        assertThat(result)
                .extracting(HistoricalRate::getTargetCurrency)
                .containsExactly(USD, PLN, USD);
    }

    @Test
    void findByDerivedQuery_ShouldExcludeRowsOutsideWindow_WhenRangeIsNarrow() {
        repository.save(rate(USD, DAY_1, "1.08"));
        repository.saveAndFlush(rate(USD, DAY_3, "1.09"));

        final List<HistoricalRate> result = repository
                .findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                        EUR, Set.of(USD), DAY_2, DAY_3);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(row -> assertThat(row.getRateDate()).isEqualTo(DAY_3));
    }

    @Test
    void findByDerivedQuery_ShouldReturnEmptyList_WhenNothingStored() {
        final List<HistoricalRate> result = repository
                .findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                        EUR, Set.of(USD), DAY_1, DAY_3);

        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFlush_ShouldThrowDataIntegrityViolation_WhenDuplicatePairAndDateInserted() {
        repository.saveAndFlush(rate(USD, DAY_1, "1.08"));

        assertThatThrownBy(() -> repository.saveAndFlush(rate(USD, DAY_1, "1.09")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private HistoricalRate rate(final Currency target, final LocalDate date, final String rate) {
        return HistoricalRate.builder()
                .baseCurrency(EUR)
                .targetCurrency(target)
                .rateDate(date)
                .rate(new BigDecimal(rate))
                .fetchedAt(Instant.parse("2026-06-12T10:00:00Z"))
                .build();
    }
}
