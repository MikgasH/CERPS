package com.example.cerpshashkin.unit.service;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.service.RateHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The repository query is native SQL, so the timestamp column arrives as
 * whatever the JDBC driver hands back: {@code java.sql.Timestamp} on H2,
 * {@code OffsetDateTime} on PostgreSQL, or already an {@code Instant}. All
 * three must map to the same instant on the wire.
 */
@ExtendWith(MockitoExtension.class)
class RateHistoryServiceTest {

    private static final Instant POINT_TIME = Instant.parse("2026-06-15T08:30:00.123456Z");

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private RateHistoryService rateHistoryService;

    @Test
    void getRateHistory_ShouldConvertTimestamp_WhenRowCarriesSqlTimestamp() {
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", null, null))
                .thenReturn(List.<Object[]>of(row("1.0834", Timestamp.from(POINT_TIME))));

        RateHistoryResponse result = rateHistoryService.getRateHistory("USD", "EUR", null, null);

        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().timestamp()).isEqualTo(POINT_TIME);
        assertThat(result.points().getFirst().rate()).isEqualByComparingTo(new BigDecimal("1.0834"));
    }

    @Test
    void getRateHistory_ShouldConvertTimestamp_WhenRowCarriesOffsetDateTime() {
        // PostgreSQL returns timestamptz as OffsetDateTime; a non-UTC offset
        // must still normalize to the same instant, not shift by the offset.
        OffsetDateTime nonUtc = POINT_TIME.atOffset(ZoneOffset.ofHours(2));
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", null, null))
                .thenReturn(List.<Object[]>of(row("1.0834", nonUtc)));

        RateHistoryResponse result = rateHistoryService.getRateHistory("USD", "EUR", null, null);

        assertThat(result.points().getFirst().timestamp()).isEqualTo(POINT_TIME);
    }

    @Test
    void getRateHistory_ShouldPassTimestampThrough_WhenRowCarriesInstant() {
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", null, null))
                .thenReturn(List.<Object[]>of(row("1.0834", POINT_TIME)));

        RateHistoryResponse result = rateHistoryService.getRateHistory("USD", "EUR", null, null);

        assertThat(result.points().getFirst().timestamp()).isEqualTo(POINT_TIME);
    }

    @Test
    void getRateHistory_ShouldPreserveOrderAndRates_WhenRowsMixTimestampTypes() {
        // One response can mix types (e.g. direct + cross-rate branches); the
        // repository returns rows ordered by timestamp and that order must survive.
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", null, null))
                .thenReturn(List.of(
                        row("1.08", Timestamp.from(POINT_TIME.minusSeconds(120))),
                        row("1.09", POINT_TIME.minusSeconds(60).atOffset(ZoneOffset.UTC)),
                        row("1.10", POINT_TIME)));

        RateHistoryResponse result = rateHistoryService.getRateHistory("USD", "EUR", null, null);

        assertThat(result.points()).hasSize(3);
        assertThat(result.points()).extracting(point -> point.timestamp()).containsExactly(
                POINT_TIME.minusSeconds(120), POINT_TIME.minusSeconds(60), POINT_TIME);
        assertThat(result.points()).extracting(point -> point.rate()).containsExactly(
                new BigDecimal("1.08"), new BigDecimal("1.09"), new BigDecimal("1.10"));
    }

    @Test
    void getRateHistory_ShouldNormalizeCurrencyCodes_WhenInputPaddedLowercase() {
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", null, null))
                .thenReturn(List.of());

        RateHistoryResponse result = rateHistoryService.getRateHistory(" usd", "eur ", null, null);

        verify(exchangeRateRepository).findRatesWithCrossSupport("USD", "EUR", null, null);
        assertThat(result.from()).isEqualTo("USD");
        assertThat(result.to()).isEqualTo("EUR");
    }

    @Test
    void getRateHistory_ShouldReturnEmptyPoints_WhenRepositoryHasNoRows() {
        when(exchangeRateRepository.findRatesWithCrossSupport(any(), any(), any(), any()))
                .thenReturn(List.of());

        RateHistoryResponse result = rateHistoryService.getRateHistory("USD", "EUR", null, null);

        assertThat(result.points()).isEmpty();
    }

    @Test
    void getRateHistory_ShouldPassDateBoundsThrough_WhenProvided() {
        Instant start = Instant.parse("2026-06-01T00:00:00Z");
        Instant end = Instant.parse("2026-06-30T00:00:00Z");
        when(exchangeRateRepository.findRatesWithCrossSupport("USD", "EUR", start, end))
                .thenReturn(List.of());

        rateHistoryService.getRateHistory("USD", "EUR", start, end);

        verify(exchangeRateRepository).findRatesWithCrossSupport("USD", "EUR", start, end);
    }

    private Object[] row(final String rate, final Object timestamp) {
        return new Object[] {
                "8b6f2c1a-0000-0000-0000-000000000001", "EUR", "USD",
                new BigDecimal(rate), "AGGREGATED", timestamp
        };
    }
}
