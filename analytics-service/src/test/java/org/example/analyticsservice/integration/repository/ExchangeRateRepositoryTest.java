package org.example.analyticsservice.integration.repository;

import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.example.analyticsservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ExchangeRateRepositoryTest {

    @Autowired
    private ExchangeRateRepository repository;

    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findRatesForPeriod_WithDataInRange_ShouldReturnRates() {
        Instant start = now.minus(7, ChronoUnit.DAYS);
        Instant end = now;

        repository.save(createRate("EUR", "USD", start.plus(1, ChronoUnit.DAYS), "1.10"));
        repository.save(createRate("EUR", "USD", start.plus(5, ChronoUnit.DAYS), "1.15"));

        List<Object[]> result = repository.findRatesWithCrossSupport("EUR", "USD", start, end);

        assertThat(result).hasSize(2);
    }

    @Test
    void findRatesForPeriod_WithNoDataInRange_ShouldReturnEmpty() {
        Instant start = now.minus(30, ChronoUnit.DAYS);
        Instant end = now.minus(20, ChronoUnit.DAYS);

        List<Object[]> result = repository.findRatesWithCrossSupport("EUR", "USD", start, end);

        assertThat(result).isEmpty();
    }

    @Test
    void findRatesForPeriod_ShouldOrderByTimestampAsc() {
        Instant start = now.minus(7, ChronoUnit.DAYS);
        Instant end = now;

        repository.save(createRate("EUR", "USD", start.plus(5, ChronoUnit.DAYS), "1.15"));
        repository.save(createRate("EUR", "USD", start.plus(1, ChronoUnit.DAYS), "1.10"));
        repository.save(createRate("EUR", "USD", start.plus(3, ChronoUnit.DAYS), "1.12"));

        List<Object[]> result = repository.findRatesWithCrossSupport("EUR", "USD", start, end);

        assertThat(result).hasSize(3);
        assertThat((BigDecimal) result.get(0)[3]).isEqualByComparingTo("1.10");
        assertThat((BigDecimal) result.get(1)[3]).isEqualByComparingTo("1.12");
        assertThat((BigDecimal) result.get(2)[3]).isEqualByComparingTo("1.15");
    }

    @Test
    void findRatesForPeriod_WithDifferentCurrencyPairs_ShouldFilter() {
        Instant start = now.minus(7, ChronoUnit.DAYS);
        Instant end = now;

        repository.save(createRate("EUR", "USD", start.plus(1, ChronoUnit.DAYS), "1.10"));
        repository.save(createRate("EUR", "JPY", start.plus(2, ChronoUnit.DAYS), "145.0"));

        List<Object[]> result = repository.findRatesWithCrossSupport("EUR", "USD", start, end);

        assertThat(result).hasSize(1);
        assertThat((String) result.getFirst()[2]).isEqualTo("USD");
    }

    @Test
    void save_ShouldPersistEntity() {
        ExchangeRateEntity entity = createRate("EUR", "USD", now, "1.18");

        ExchangeRateEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.count()).isEqualTo(1);
    }


    private ExchangeRateEntity createRate(String from, String to, Instant timestamp, String rate) {
        return ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(from)
                .targetCurrency(to)
                .rate(new BigDecimal(rate))
                .source("TEST")
                .timestamp(timestamp)
                .build();
    }
}
