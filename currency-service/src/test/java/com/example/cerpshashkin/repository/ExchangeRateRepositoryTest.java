package com.example.cerpshashkin.repository;

import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.entity.ExchangeRateSource;
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
import java.util.Optional;
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
    void findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc_WithExistingRate_ShouldReturnLatest() {
        ExchangeRateEntity oldRate = createRate(EUR, USD, BigDecimal.valueOf(1.17), now.minus(2, ChronoUnit.HOURS));
        ExchangeRateEntity newRate = createRate(EUR, USD, BigDecimal.valueOf(1.18), now);

        repository.save(oldRate);
        repository.save(newRate);

        Optional<ExchangeRateEntity> result = repository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(EUR, USD);

        assertThat(result).isPresent();
        assertThat(result.get().getRate()).isEqualByComparingTo(BigDecimal.valueOf(1.18));
        assertThat(result.get().getTimestamp()).isEqualTo(now);
    }

    @Test
    void findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc_WithNonExisting_ShouldReturnEmpty() {
        Optional<ExchangeRateEntity> result = repository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(EUR, JPY);

        assertThat(result).isEmpty();
    }

    @Test
    void findRatesForPeriod_WithRatesInPeriod_ShouldReturnAll() {
        Instant start = now.minus(10, ChronoUnit.HOURS);
        Instant end = now;

        ExchangeRateEntity rate1 = createRate(EUR, USD, BigDecimal.valueOf(1.17), start.plus(1, ChronoUnit.HOURS));
        ExchangeRateEntity rate2 = createRate(EUR, USD, BigDecimal.valueOf(1.18), start.plus(5, ChronoUnit.HOURS));
        ExchangeRateEntity rate3 = createRate(EUR, USD, BigDecimal.valueOf(1.19), start.plus(9, ChronoUnit.HOURS));
        ExchangeRateEntity outsideRate = createRate(EUR, USD, BigDecimal.valueOf(1.20), end.plus(1, ChronoUnit.HOURS));

        repository.saveAll(List.of(rate1, rate2, rate3, outsideRate));

        List<ExchangeRateEntity> results = repository.findRatesForPeriod(EUR, USD, start, end);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(ExchangeRateEntity::getRate)
                .containsExactly(
                        BigDecimal.valueOf(1.17),
                        BigDecimal.valueOf(1.18),
                        BigDecimal.valueOf(1.19)
                );
    }

    @Test
    void findRatesForPeriod_WithNoRatesInPeriod_ShouldReturnEmpty() {
        Instant start = now.minus(10, ChronoUnit.HOURS);
        Instant end = now.minus(5, ChronoUnit.HOURS);

        ExchangeRateEntity rate = createRate(EUR, USD, BigDecimal.valueOf(1.18), now);
        repository.save(rate);

        List<ExchangeRateEntity> results = repository.findRatesForPeriod(EUR, USD, start, end);

        assertThat(results).isEmpty();
    }

    @Test
    void findRatesForPeriod_ShouldOrderByTimestampAsc() {
        Instant start = now.minus(5, ChronoUnit.HOURS);
        Instant end = now;

        ExchangeRateEntity rate1 = createRate(EUR, USD, BigDecimal.valueOf(1.17), start.plus(3, ChronoUnit.HOURS));
        ExchangeRateEntity rate2 = createRate(EUR, USD, BigDecimal.valueOf(1.18), start.plus(1, ChronoUnit.HOURS));
        ExchangeRateEntity rate3 = createRate(EUR, USD, BigDecimal.valueOf(1.19), start.plus(4, ChronoUnit.HOURS));

        repository.saveAll(List.of(rate1, rate2, rate3));

        List<ExchangeRateEntity> results = repository.findRatesForPeriod(EUR, USD, start, end);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(ExchangeRateEntity::getRate)
                .containsExactly(
                        BigDecimal.valueOf(1.18),
                        BigDecimal.valueOf(1.17),
                        BigDecimal.valueOf(1.19)
                );
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
                .source(ExchangeRateSource.AGGREGATED)
                .timestamp(now)
                .build();

        ExchangeRateEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBaseCurrency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(saved.getTargetCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(saved.getRate()).isEqualByComparingTo(BigDecimal.valueOf(1.18));
        assertThat(saved.getSource()).isEqualTo(ExchangeRateSource.AGGREGATED);
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
    void findRatesForPeriod_WithDifferentCurrencyPairs_ShouldFilterCorrectly() {
        Instant start = now.minus(5, ChronoUnit.HOURS);
        Instant end = now;

        repository.save(createRate(EUR, USD, BigDecimal.valueOf(1.18), start.plus(1, ChronoUnit.HOURS)));
        repository.save(createRate(EUR, GBP, BigDecimal.valueOf(0.87), start.plus(2, ChronoUnit.HOURS)));
        repository.save(createRate(USD, JPY, BigDecimal.valueOf(110.0), start.plus(3, ChronoUnit.HOURS)));

        List<ExchangeRateEntity> results = repository.findRatesForPeriod(EUR, USD, start, end);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTargetCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    private ExchangeRateEntity createRate(Currency base, Currency target, BigDecimal rate, Instant timestamp) {
        return ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(base)
                .targetCurrency(target)
                .rate(rate)
                .source(ExchangeRateSource.AGGREGATED)
                .timestamp(timestamp)
                .build();
    }
}
