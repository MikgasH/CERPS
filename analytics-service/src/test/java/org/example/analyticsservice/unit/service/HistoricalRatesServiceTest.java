package org.example.analyticsservice.unit.service;

import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.exception.ExternalServiceException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.analyticsservice.client.FrankfurterClient;
import org.example.analyticsservice.entity.HistoricalCoverage;
import org.example.analyticsservice.entity.HistoricalRate;
import org.example.analyticsservice.repository.HistoricalCoverageRepository;
import org.example.analyticsservice.repository.HistoricalRateRepository;
import org.example.analyticsservice.service.HistoricalRatesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalRatesServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency PLN = Currency.getInstance("PLN");

    @Mock
    private FrankfurterClient frankfurterClient;

    @Mock
    private HistoricalRateRepository rateRepository;

    @Mock
    private HistoricalCoverageRepository coverageRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<List<HistoricalRate>> rowsCaptor;

    @Captor
    private ArgumentCaptor<HistoricalCoverage> coverageCaptor;

    private SimpleMeterRegistry meterRegistry;
    private HistoricalRatesService service;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new HistoricalRatesService(frankfurterClient, rateRepository, coverageRepository,
                new TransactionTemplate(transactionManager), meterRegistry);
        service.initMetrics();
    }

    @Test
    void getRatePoints_ShouldFetchFullRangeAndStore_WhenNeverFetched() {
        LocalDate start = today.minusDays(6);
        List<FrankfurterRateEntry> entries = dailyEntries("USD", "1.10", start, today);

        when(coverageRepository.findById(USD)).thenReturn(Optional.empty());
        when(frankfurterClient.getRates(Set.of("USD"), start, today)).thenReturn(entries);
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), start, today))
                .thenReturn(List.of())
                .thenReturn(rowsFor(entries));

        List<RatePoint> points = service.getRatePoints("USD", "EUR", start, today);

        assertThat(points).hasSize(7);
        // USD -> EUR = 1 / (EUR -> USD) = 1 / 1.10
        assertThat(points.getFirst().rate()).isEqualByComparingTo(new BigDecimal("0.909091"));
        assertThat(points.getFirst().timestamp()).isEqualTo(expectedFixingInstant(start));

        verify(frankfurterClient).getRates(Set.of("USD"), start, today);
        verify(rateRepository).saveAllAndFlush(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(7);
        assertThat(rowsCaptor.getValue().getFirst().getBaseCurrency()).isEqualTo(EUR);
        assertThat(rowsCaptor.getValue().getFirst().getTargetCurrency()).isEqualTo(USD);

        verify(coverageRepository).saveAndFlush(coverageCaptor.capture());
        assertThat(coverageCaptor.getValue().getCoveredFrom()).isEqualTo(start);
        // today's rate is provisional until the ECB fixing - never marked covered
        assertThat(coverageCaptor.getValue().getCoveredTo()).isEqualTo(today.minusDays(1));

        assertThat(counterValue("miss")).isEqualTo(1.0);
        assertThat(counterValue("hit")).isZero();
    }

    @Test
    void getRatePoints_ShouldFetchOnlyMissingExtension_WhenPartiallyCovered() {
        LocalDate start = today.minusDays(9);
        LocalDate coveredFrom = today.minusDays(40);
        LocalDate coveredTo = today.minusDays(5);
        LocalDate extensionFrom = coveredTo.plusDays(1);
        List<FrankfurterRateEntry> entries = dailyEntries("USD", "1.20", extensionFrom, today);

        when(coverageRepository.findById(USD)).thenReturn(Optional.of(coverage(USD, coveredFrom, coveredTo)));
        when(frankfurterClient.getRates(Set.of("USD"), extensionFrom, today)).thenReturn(entries);
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), extensionFrom, today)).thenReturn(List.of());
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), start, today)).thenReturn(rowsFor(entries));

        service.getRatePoints("USD", "EUR", start, today);

        verify(frankfurterClient, times(1)).getRates(any(), any(), any());
        verify(frankfurterClient).getRates(Set.of("USD"), extensionFrom, today);

        verify(coverageRepository).saveAndFlush(coverageCaptor.capture());
        assertThat(coverageCaptor.getValue().getCoveredFrom()).isEqualTo(coveredFrom);
        assertThat(coverageCaptor.getValue().getCoveredTo()).isEqualTo(today.minusDays(1));
    }

    @Test
    void getRatePoints_ShouldNotCallFrankfurter_WhenFullyCovered() {
        LocalDate start = today.minusDays(7);
        LocalDate end = today.minusDays(1);
        List<FrankfurterRateEntry> entries = dailyEntries("USD", "1.25", start, end);

        when(coverageRepository.findById(USD)).thenReturn(Optional.of(coverage(USD, start.minusDays(30), end)));
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), start, end)).thenReturn(rowsFor(entries));

        List<RatePoint> points = service.getRatePoints("EUR", "USD", start, end);

        assertThat(points).hasSize(7);
        // EUR -> USD is the stored EUR-based series itself
        assertThat(points.getFirst().rate()).isEqualByComparingTo(new BigDecimal("1.25"));

        verifyNoInteractions(frankfurterClient);
        verify(rateRepository, never()).saveAllAndFlush(anyList());
        verify(coverageRepository, never()).saveAndFlush(any());

        assertThat(counterValue("hit")).isEqualTo(1.0);
        assertThat(counterValue("miss")).isZero();
    }

    @Test
    void getRatePoints_ShouldComputeCrossRatePerDate_ForTwoNonEurCurrencies() {
        LocalDate start = today.minusDays(10);
        LocalDate end = today.minusDays(8);
        LocalDate d1 = start;
        LocalDate d2 = start.plusDays(1);
        LocalDate usdOnlyDate = start.plusDays(2);

        when(coverageRepository.findById(USD)).thenReturn(Optional.of(coverage(USD, start.minusDays(1), end)));
        when(coverageRepository.findById(PLN)).thenReturn(Optional.of(coverage(PLN, start.minusDays(1), end)));
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD, PLN), start, end)).thenReturn(List.of(
                row(USD, d1, "1.10"),
                row(USD, d2, "1.20"),
                row(USD, usdOnlyDate, "1.15"),
                row(PLN, d1, "4.40"),
                row(PLN, d2, "4.20")));

        List<RatePoint> points = service.getRatePoints("USD", "PLN", start, end);

        // the date with only a USD row is a genuine PLN gap - excluded
        assertThat(points).hasSize(2);
        assertThat(points.get(0).rate()).isEqualByComparingTo(new BigDecimal("4.000000"));
        assertThat(points.get(1).rate()).isEqualByComparingTo(new BigDecimal("3.500000"));
        assertThat(points.get(0).timestamp()).isEqualTo(expectedFixingInstant(d1));
        assertThat(points.get(1).timestamp()).isEqualTo(expectedFixingInstant(d2));
    }

    @Test
    void getRatePoints_ShouldHandleConcurrentInsertRace_Gracefully() {
        LocalDate start = today.minusDays(7);
        LocalDate end = today.minusDays(1);
        List<FrankfurterRateEntry> entries = dailyEntries("USD", "1.10", start, end);
        List<HistoricalRate> winnerRows = rowsFor(entries);

        when(coverageRepository.findById(USD)).thenReturn(Optional.empty());
        when(frankfurterClient.getRates(Set.of("USD"), start, end)).thenReturn(entries);
        // first store attempt sees an empty table, the retry re-reads the
        // winner's rows, the final load serves them
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), start, end))
                .thenReturn(List.of())
                .thenReturn(winnerRows)
                .thenReturn(winnerRows);
        when(rateRepository.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("uq_hist_rate"));

        List<RatePoint> points = service.getRatePoints("USD", "EUR", start, end);

        assertThat(points).hasSize(7);
        // the retry found every row already present, so it inserted nothing
        verify(rateRepository, times(1)).saveAllAndFlush(anyList());
        verify(frankfurterClient, times(1)).getRates(any(), any(), any());
        verify(coverageRepository).saveAndFlush(any());
        assertThat(counterValue("miss")).isEqualTo(1.0);
        assertThat(counterValue("error")).isZero();
    }

    @Test
    void getRatePoints_ShouldServeSparseSeriesWithoutRefetch_WhenUpstreamHasGap() {
        LocalDate start = today.minusDays(7);
        LocalDate end = today.minusDays(1);
        LocalDate gapDate = start.plusDays(3);
        List<FrankfurterRateEntry> entries = new ArrayList<>(dailyEntries("USD", "1.10", start, end));
        entries.removeIf(entry -> entry.date().equals(gapDate));

        when(coverageRepository.findById(USD))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(coverage(USD, start, end)));
        when(frankfurterClient.getRates(Set.of("USD"), start, end)).thenReturn(entries);
        when(rateRepository.findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                EUR, List.of(USD), start, end))
                .thenReturn(List.of())
                .thenReturn(rowsFor(entries))
                .thenReturn(rowsFor(entries));

        List<RatePoint> firstCall = service.getRatePoints("USD", "EUR", start, end);
        List<RatePoint> secondCall = service.getRatePoints("USD", "EUR", start, end);

        assertThat(firstCall).hasSize(6);
        assertThat(firstCall)
                .extracting(RatePoint::timestamp)
                .doesNotContain(expectedFixingInstant(gapDate));
        assertThat(secondCall).hasSize(6);

        // the gap is recorded as covered - no refetch loop
        verify(frankfurterClient, times(1)).getRates(any(), any(), any());
        assertThat(counterValue("miss")).isEqualTo(1.0);
        assertThat(counterValue("hit")).isEqualTo(1.0);
    }

    @Test
    void getRatePoints_ShouldReturnIdentitySeries_WhenBothSidesAreEur() {
        LocalDate start = today.minusDays(2);
        LocalDate end = today.minusDays(1);

        List<RatePoint> points = service.getRatePoints("EUR", "EUR", start, end);

        assertThat(points).hasSize(2);
        assertThat(points.getFirst().rate()).isEqualByComparingTo(BigDecimal.ONE);
        verifyNoInteractions(frankfurterClient, rateRepository, coverageRepository);
    }

    @Test
    void getRatePoints_ShouldCountErrorAndPropagate_WhenFrankfurterFails() {
        LocalDate start = today.minusDays(7);

        when(coverageRepository.findById(USD)).thenReturn(Optional.empty());
        when(frankfurterClient.getRates(any(), any(), any()))
                .thenThrow(new ExternalServiceException("Frankfurter down"));

        assertThatThrownBy(() -> service.getRatePoints("USD", "EUR", start, today))
                .isInstanceOf(ExternalServiceException.class);

        assertThat(counterValue("error")).isEqualTo(1.0);
        verify(rateRepository, never()).saveAllAndFlush(anyList());
    }

    private double counterValue(String result) {
        return meterRegistry.counter("analytics.historical.fetch", "result", result).count();
    }

    private static Instant expectedFixingInstant(LocalDate date) {
        return date.atTime(HistoricalRatesService.ECB_FIXING_TIME)
                .atZone(HistoricalRatesService.ECB_TIME_ZONE)
                .toInstant();
    }

    private static List<FrankfurterRateEntry> dailyEntries(
            String quote, String rate, LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .map(date -> new FrankfurterRateEntry(date, EUR, quote, new BigDecimal(rate)))
                .toList();
    }

    private static List<HistoricalRate> rowsFor(List<FrankfurterRateEntry> entries) {
        List<HistoricalRate> rows = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            FrankfurterRateEntry entry = entries.get(i);
            rows.add(HistoricalRate.builder()
                    .id((long) i + 1)
                    .baseCurrency(EUR)
                    .targetCurrency(Currency.getInstance(entry.quote()))
                    .rateDate(entry.date())
                    .rate(entry.rate())
                    .fetchedAt(Instant.now())
                    .build());
        }
        return rows;
    }

    private static HistoricalRate row(Currency target, LocalDate date, String rate) {
        return HistoricalRate.builder()
                .id(date.toEpochDay())
                .baseCurrency(EUR)
                .targetCurrency(target)
                .rateDate(date)
                .rate(new BigDecimal(rate))
                .fetchedAt(Instant.now())
                .build();
    }

    private static HistoricalCoverage coverage(Currency currency, LocalDate from, LocalDate to) {
        return HistoricalCoverage.builder()
                .targetCurrency(currency)
                .coveredFrom(from)
                .coveredTo(to)
                .updatedAt(Instant.now())
                .build();
    }
}
