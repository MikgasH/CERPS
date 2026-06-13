package com.example.cerpshashkin.integration.repository;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ExchangeRateRepositoryTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Autowired
    private ExchangeRateRepository repository;

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        repository.deleteAll();
    }

    @Test
    void deleteByTimestampBefore_WithOldRates_ShouldDeleteThem() {
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        ExchangeRateEntity oldRate1 = createRate(EUR, USD, BigDecimal.valueOf(1.17), cutoff.minus(10, ChronoUnit.DAYS));
        ExchangeRateEntity oldRate2 = createRate(EUR, GBP, BigDecimal.valueOf(0.87), cutoff.minus(5, ChronoUnit.DAYS));
        ExchangeRateEntity newRate = createRate(EUR, JPY, BigDecimal.valueOf(130.0), cutoff.plus(1, ChronoUnit.DAYS));

        repository.saveAll(List.of(oldRate1, oldRate2, newRate));

        int deleted = repository.deleteByTimestampBefore(cutoff);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().getFirst().getTargetCurrency().getCurrencyCode()).isEqualTo("JPY");
    }

    @Test
    void deleteByTimestampBefore_WithNoOldRates_ShouldDeleteNothing() {
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        ExchangeRateEntity rate = createRate(EUR, USD, BigDecimal.valueOf(1.18), now);
        repository.save(rate);

        int deleted = repository.deleteByTimestampBefore(cutoff);

        assertThat(deleted).isZero();
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void save_WithAllFields_ShouldPersist() {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(EUR)
                .targetCurrency(USD)
                .rate(BigDecimal.valueOf(1.18))
                .source(CerpsConstants.EXCHANGE_RATE_SOURCE_AGGREGATED)
                .timestamp(now)
                .build();

        ExchangeRateEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBaseCurrency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(saved.getTargetCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(saved.getRate()).isEqualByComparingTo(BigDecimal.valueOf(1.18));
        assertThat(saved.getSource()).isEqualTo(CerpsConstants.EXCHANGE_RATE_SOURCE_AGGREGATED);
        assertThat(saved.getTimestamp()).isEqualTo(now);
    }

    @Test
    void findAll_WithMultipleCurrencyPairs_ShouldReturnAll() {
        repository.save(createRate(EUR, USD, BigDecimal.valueOf(1.18), now));
        repository.save(createRate(EUR, GBP, BigDecimal.valueOf(0.87), now));
        repository.save(createRate(USD, JPY, BigDecimal.valueOf(110.0), now));

        List<ExchangeRateEntity> all = repository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void findLatestPerTargetInWindow_ShouldReturnLatestSnapshotPerCurrency() {
        Instant windowStart = now.minus(2, ChronoUnit.DAYS);
        Instant windowEnd = now.plus(1, ChronoUnit.HOURS);

        repository.save(createRate(EUR, USD, BigDecimal.valueOf(1.17), now.minus(1, ChronoUnit.DAYS)));
        repository.save(createRate(EUR, USD, BigDecimal.valueOf(1.18), now));
        repository.save(createRate(EUR, GBP, BigDecimal.valueOf(0.87), now.minus(1, ChronoUnit.DAYS)));

        List<ExchangeRateEntity> results = repository
                .findLatestPerTargetInWindow("EUR", windowStart, windowEnd);

        assertThat(results).hasSize(2);
        assertThat(results)
                .filteredOn(e -> e.getTargetCurrency().equals(USD))
                .first()
                .extracting(ExchangeRateEntity::getRate)
                .satisfies(rate -> assertThat(rate).isEqualByComparingTo(BigDecimal.valueOf(1.18)));
    }

    @Test
    void findLatestPerTargetInWindow_ShouldExcludeRatesOutsideWindow() {
        Instant windowStart = now.minus(1, ChronoUnit.DAYS);
        Instant windowEnd = now;

        repository.save(createRate(EUR, USD, BigDecimal.valueOf(1.17), windowStart.minus(1, ChronoUnit.HOURS)));
        repository.save(createRate(EUR, GBP, BigDecimal.valueOf(0.87), windowEnd.plus(1, ChronoUnit.HOURS)));

        List<ExchangeRateEntity> results = repository
                .findLatestPerTargetInWindow("EUR", windowStart, windowEnd);

        assertThat(results).isEmpty();
    }

    private ExchangeRateEntity createRate(Currency base, Currency target, BigDecimal rate, Instant timestamp) {
        return ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(base)
                .targetCurrency(target)
                .rate(rate)
                .source(CerpsConstants.EXCHANGE_RATE_SOURCE_AGGREGATED)
                .timestamp(timestamp)
                .build();
    }
}
