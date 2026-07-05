package org.example.analyticsservice.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerps.common.dto.RatePoint;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.client.FrankfurterClient;
import org.example.analyticsservice.entity.HistoricalCoverage;
import org.example.analyticsservice.entity.HistoricalRate;
import org.example.analyticsservice.repository.HistoricalCoverageRepository;
import org.example.analyticsservice.repository.HistoricalRateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lazily maintained EUR-based daily rate store sourced from Frankfurter.
 *
 * <p>Per request: for each non-EUR leg whose {@code historical_coverage}
 * interval does not span the window, the missing extension is fetched from
 * Frankfurter (HTTP strictly outside any transaction), missing rows are
 * inserted, and coverage is extended. Cross-rates are then computed per date
 * from the stored EUR series. Dates absent inside a covered interval are
 * genuine upstream gaps and are served sparse, never refetched.
 *
 * <p>Coverage is only ever recorded up to yesterday: today's rate is
 * provisional until the ECB fixing, so a window ending today refetches the
 * tail (one tiny call, absorbed by the response-level {@code TrendsCache})
 * and refreshes today's provisional row in place.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalRatesService {

    // The ECB publishes its euro reference (fixing) rates around 16:00 CET.
    // Stored rows carry only a date, but the trends contract exposes Instant
    // timestamps - each rate_date is therefore anchored at the fixing time,
    // the moment the day's rate actually became official.
    public static final LocalTime ECB_FIXING_TIME = LocalTime.of(16, 0);
    public static final ZoneId ECB_TIME_ZONE = ZoneId.of("CET");

    private static final Currency EUR_CURRENCY = Currency.getInstance("EUR");
    private static final BigDecimal IDENTITY_RATE =
            BigDecimal.ONE.setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP);

    private static final String FETCH_COUNTER = "analytics.historical.fetch";
    private static final String RESULT_TAG = "result";

    private static final String BACKFILL_LOG = "Backfilling {} from Frankfurter for ranges {}";
    private static final String STORED_LOG = "Stored {} historical rows for {}";
    private static final String RACE_RETRY_LOG =
            "Concurrent insert detected for {} - re-reading and retrying: {}";
    private static final String RACE_GIVE_UP_LOG =
            "Concurrent insert persisted after re-read for {} - serving stored data: {}";

    private final FrankfurterClient frankfurterClient;
    private final HistoricalRateRepository historicalRateRepository;
    private final HistoricalCoverageRepository historicalCoverageRepository;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    // Single-flight coalescing per currency: concurrent first-requests for the
    // same currency share one backfill instead of each firing its own
    // Frankfurter call. The key space is the supported-currency set (small,
    // fixed), so the map needs no eviction.
    private final ConcurrentHashMap<Currency, Lock> backfillLocks = new ConcurrentHashMap<>();

    private Counter fetchHitCounter;
    private Counter fetchMissCounter;
    private Counter fetchErrorCounter;

    @PostConstruct
    public void initMetrics() {
        fetchHitCounter = Counter.builder(FETCH_COUNTER).tag(RESULT_TAG, "hit")
                .description("Historical windows served entirely from the local store")
                .register(meterRegistry);

        fetchMissCounter = Counter.builder(FETCH_COUNTER).tag(RESULT_TAG, "miss")
                .description("Historical windows that required a Frankfurter backfill")
                .register(meterRegistry);

        fetchErrorCounter = Counter.builder(FETCH_COUNTER).tag(RESULT_TAG, "error")
                .description("Historical windows whose Frankfurter backfill failed")
                .register(meterRegistry);
    }

    /**
     * Returns the daily cross-rate series {@code from -> to} over
     * {@code [startDate, endDate]}, backfilling the local store from
     * Frankfurter as needed. The series may be sparse where the upstream has
     * genuine gaps; points are sorted by date.
     */
    public List<RatePoint> getRatePoints(final String from, final String to,
                                         final LocalDate startDate, final LocalDate endDate) {
        final Currency fromCurrency = Currency.getInstance(from);
        final Currency toCurrency = Currency.getInstance(to);

        boolean fetched = false;
        try {
            for (final Currency currency : nonEurTargets(fromCurrency, toCurrency)) {
                fetched |= ensureCoverage(currency, startDate, endDate);
            }
        } catch (RuntimeException e) {
            fetchErrorCounter.increment();
            throw e;
        }
        if (fetched) {
            fetchMissCounter.increment();
        } else {
            fetchHitCounter.increment();
        }

        return computeCrossRates(fromCurrency, toCurrency, startDate, endDate);
    }

    /**
     * Brings {@code historical_coverage} for one currency up to the requested
     * window, fetching only the uncovered extension(s).
     *
     * @return whether a Frankfurter call was made
     */
    private boolean ensureCoverage(final Currency currency, final LocalDate startDate, final LocalDate endDate) {
        final LocalDate today = LocalDate.now();
        // Frankfurter has nothing beyond today; never request future dates.
        final LocalDate fetchEnd = endDate.isAfter(today) ? today : endDate;
        if (fetchEnd.isBefore(startDate)) {
            return false;
        }

        // Coalesce concurrent backfills for this currency. The first thread
        // fetches and extends coverage; threads that were waiting then re-read
        // coverage below, find the window already covered, and skip the call.
        // The DB unique-violation handling stays as a second line of defence
        // for races across separate instances (the lock is per-instance).
        final Lock lock = backfillLocks.computeIfAbsent(currency, c -> new ReentrantLock());
        lock.lock();
        try {
            final HistoricalCoverage coverage = historicalCoverageRepository.findById(currency).orElse(null);
            final List<DateRange> missingRanges = missingRanges(coverage, startDate, fetchEnd);
            if (missingRanges.isEmpty()) {
                return false;
            }

            log.info(BACKFILL_LOG, currency, missingRanges);

            // The HTTP calls happen strictly before (and outside) the
            // transactional write - never hold a transaction open across them.
            final List<FrankfurterRateEntry> entries = new ArrayList<>();
            for (final DateRange range : missingRanges) {
                entries.addAll(frankfurterClient.getRates(
                        Set.of(currency.getCurrencyCode()), range.from(), range.to()));
            }

            final LocalDate fetchedFrom = missingRanges.getFirst().from();
            final LocalDate fetchedTo = missingRanges.getLast().to();
            try {
                storeFetchedRange(currency, coverage, entries, fetchedFrom, fetchedTo, today);
            } catch (DataIntegrityViolationException firstRace) {
                // Two simultaneous first-requests racing on the same currency:
                // the loser hits uq_hist_rate (or the coverage PK). Benign -
                // re-read what the winner stored and insert only what is missing.
                log.info(RACE_RETRY_LOG, currency, firstRace.getMessage());
                try {
                    storeFetchedRange(currency,
                            historicalCoverageRepository.findById(currency).orElse(null),
                            entries, fetchedFrom, fetchedTo, today);
                } catch (DataIntegrityViolationException secondRace) {
                    log.warn(RACE_GIVE_UP_LOG, currency, secondRace.getMessage());
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Uncovered sub-ranges of {@code [start, end]}. Extensions always reach
     * the existing interval so coverage stays one contiguous range.
     */
    private List<DateRange> missingRanges(final HistoricalCoverage coverage,
                                          final LocalDate start, final LocalDate end) {
        if (coverage == null) {
            return List.of(new DateRange(start, end));
        }
        final List<DateRange> ranges = new ArrayList<>();
        if (start.isBefore(coverage.getCoveredFrom())) {
            ranges.add(new DateRange(start, coverage.getCoveredFrom().minusDays(1)));
        }
        if (end.isAfter(coverage.getCoveredTo())) {
            ranges.add(new DateRange(coverage.getCoveredTo().plusDays(1), end));
        }
        return ranges;
    }

    /**
     * Transactional write: insert fetched rows that are not stored yet
     * (loaded-first {@code saveAll}, no {@code ON CONFLICT}, so the code stays
     * H2-portable), refresh today's provisional row, extend coverage.
     */
    private void storeFetchedRange(final Currency currency, final HistoricalCoverage coverage,
                                   final List<FrankfurterRateEntry> entries,
                                   final LocalDate fetchedFrom, final LocalDate fetchedTo,
                                   final LocalDate today) {
        final Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> {
            final Map<LocalDate, HistoricalRate> existingByDate = historicalRateRepository
                    .findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                            EUR_CURRENCY, List.of(currency), fetchedFrom, fetchedTo)
                    .stream()
                    .collect(Collectors.toMap(HistoricalRate::getRateDate, Function.identity()));

            final List<HistoricalRate> rows = new ArrayList<>();
            for (final FrankfurterRateEntry entry : entries) {
                if (entry.date() == null || entry.rate() == null
                        || !currency.getCurrencyCode().equals(entry.quote())) {
                    continue;
                }
                final HistoricalRate existing = existingByDate.get(entry.date());
                if (existing == null) {
                    rows.add(HistoricalRate.builder()
                            .baseCurrency(EUR_CURRENCY)
                            .targetCurrency(currency)
                            .rateDate(entry.date())
                            .rate(entry.rate())
                            .fetchedAt(now)
                            .build());
                } else if (!entry.date().isBefore(today)) {
                    // A pre-existing row inside an uncovered range can only be
                    // a provisional same-day value from an earlier fetch -
                    // refresh it; rows for past dates stay immutable.
                    existing.setRate(entry.rate());
                    existing.setFetchedAt(now);
                    rows.add(existing);
                }
            }
            if (!rows.isEmpty()) {
                historicalRateRepository.saveAllAndFlush(rows);
                log.debug(STORED_LOG, rows.size(), currency);
            }

            saveCoverage(currency, coverage, fetchedFrom, fetchedTo, today, now);
        });
    }

    private void saveCoverage(final Currency currency, final HistoricalCoverage coverage,
                              final LocalDate fetchedFrom, final LocalDate fetchedTo,
                              final LocalDate today, final Instant now) {
        // Today's rate is provisional until the ECB fixing - never mark it
        // covered, so the tail is refetched (and refreshed) on a later request.
        final LocalDate lastFinalDate = today.minusDays(1);
        final LocalDate cappedTo = fetchedTo.isAfter(lastFinalDate) ? lastFinalDate : fetchedTo;
        final LocalDate newFrom = coverage == null
                ? fetchedFrom : earlier(coverage.getCoveredFrom(), fetchedFrom);
        final LocalDate newTo = coverage == null
                ? cappedTo : later(coverage.getCoveredTo(), cappedTo);
        if (newTo.isBefore(newFrom)) {
            return;
        }
        historicalCoverageRepository.saveAndFlush(HistoricalCoverage.builder()
                .targetCurrency(currency)
                .coveredFrom(newFrom)
                .coveredTo(newTo)
                .updatedAt(now)
                .build());
    }

    private List<RatePoint> computeCrossRates(final Currency fromCurrency, final Currency toCurrency,
                                              final LocalDate startDate, final LocalDate endDate) {
        if (EUR_CURRENCY.equals(fromCurrency) && EUR_CURRENCY.equals(toCurrency)) {
            return startDate.datesUntil(endDate.plusDays(1))
                    .map(date -> new RatePoint(atEcbFixing(date), IDENTITY_RATE))
                    .toList();
        }

        final List<HistoricalRate> rows = historicalRateRepository
                .findByBaseCurrencyAndTargetCurrencyInAndRateDateBetweenOrderByRateDate(
                        EUR_CURRENCY, nonEurTargets(fromCurrency, toCurrency), startDate, endDate);

        // EUR legs have no stored series; the empty map falls back to the
        // identity rate below (EUR -> EUR is 1 by definition).
        final Map<LocalDate, BigDecimal> fromSeries = seriesFor(rows, fromCurrency);
        final Map<LocalDate, BigDecimal> toSeries = seriesFor(rows, toCurrency);

        final SortedSet<LocalDate> dates;
        if (EUR_CURRENCY.equals(fromCurrency)) {
            dates = new TreeSet<>(toSeries.keySet());
        } else if (EUR_CURRENCY.equals(toCurrency)) {
            dates = new TreeSet<>(fromSeries.keySet());
        } else {
            dates = new TreeSet<>(fromSeries.keySet());
            dates.retainAll(toSeries.keySet());
        }

        final List<RatePoint> points = new ArrayList<>(dates.size());
        for (final LocalDate date : dates) {
            final BigDecimal eurToTarget = toSeries.getOrDefault(date, IDENTITY_RATE);
            final BigDecimal eurToSource = fromSeries.getOrDefault(date, IDENTITY_RATE);
            points.add(new RatePoint(atEcbFixing(date),
                    eurToTarget
                            .divide(eurToSource,
                                    CerpsConstants.INTERMEDIATE_CALCULATION_SCALE, RoundingMode.HALF_UP)
                            .setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP)));
        }
        return points;
    }

    private static Map<LocalDate, BigDecimal> seriesFor(final List<HistoricalRate> rows, final Currency currency) {
        return rows.stream()
                .filter(row -> currency.equals(row.getTargetCurrency()))
                .collect(Collectors.toMap(HistoricalRate::getRateDate, HistoricalRate::getRate, (a, b) -> b));
    }

    private static List<Currency> nonEurTargets(final Currency from, final Currency to) {
        return Stream.of(from, to)
                .filter(currency -> !EUR_CURRENCY.equals(currency))
                .distinct()
                .toList();
    }

    static Instant atEcbFixing(final LocalDate date) {
        return date.atTime(ECB_FIXING_TIME).atZone(ECB_TIME_ZONE).toInstant();
    }

    private static LocalDate earlier(final LocalDate a, final LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate later(final LocalDate a, final LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private record DateRange(LocalDate from, LocalDate to) { }
}
