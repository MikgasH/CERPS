package org.example.analyticsservice.integration.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.integration.config.TestConfig;
import org.example.analyticsservice.repository.ExchangeRateRepository;
import org.example.analyticsservice.service.TrendsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class TrendsServiceIntegrationTest {

    @Autowired
    private TrendsService trendsService;

    @Autowired
    private ExchangeRateRepository repository;

    private final Instant now = Instant.now();

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void calculateTrends_WithEmptyDatabase_ShouldThrowException() {
        TrendsRequest request = new TrendsRequest("CHF", "CAD", "7D");

        assertThatThrownBy(() -> trendsService.calculateTrends(request))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("Found 0 data points");
    }

    @Test
    void calculateTrends_For1DayPeriod_ShouldQueryCorrectDateRange() {
        repository.save(createRate("EUR", "GBP", now.minus(2, ChronoUnit.HOURS), "0.80"));
        repository.save(createRate("EUR", "GBP", now, "0.81"));

        TrendsRequest request = new TrendsRequest("EUR", "GBP", "1D");
        TrendsResponse response = trendsService.calculateTrends(request);

        assertThat(response.dataPoints()).isGreaterThanOrEqualTo(2);
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
