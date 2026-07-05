package org.example.analyticsservice.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.client.CurrencyServiceClient;
import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.exception.MinimumPeriodNotSupportedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendsService {

    static final int FALLBACK_WIDEN_FACTOR = 2;
    static final Duration FALLBACK_MAX_WINDOW = Duration.ofDays(30);

    // Currencies covered only by the Frankfurter gap-fill provider in
    // currency-service: one rate per business day, so a 1D window yields
    // 1-2 identical points — not enough for a meaningful chart.
    //
    // ⚠️ CROSS-SERVICE COUPLING: this set duplicates provider coverage that
    // only currency-service actually knows (its primary providers vs the
    // fillMissingRatesFromFallback gap-fill in ExchangeRateService; the codes
    // below are the v2.0-add-cis-currencies seed). currency-service exposes
    // no per-currency source metadata yet, so this cannot be queried — if a
    // currency gains/loses primary-provider coverage there, update this set
    // in the same change (see the matching note in ExchangeRateService).
    static final Set<String> FRANKFURTER_ONLY_CURRENCIES = Set.of(
            "BYN", "RUB", "GEL", "AMD", "AZN", "MDL", "KZT", "UZS",
            "ISK", "RSD", "BAM", "MKD", "ALL");

    private static final String PERIOD_1D = "1D";
    private static final String MINIMUM_PERIOD = "7D";
    private static final String MINIMUM_PERIOD_MESSAGE =
            "1D period not available for this currency pair. Minimum period is 7D.";

    private static final String HISTORICAL_FALLBACK_LOG =
            "Historical store yielded {} point(s) for {} -> {} - falling back to currency-service history";
    private static final String HISTORICAL_FAILURE_LOG =
            "Historical store failed for {} -> {} - falling back to currency-service history: {}";

    private final CurrencyServiceClient currencyServiceClient;
    private final HistoricalRatesService historicalRatesService;
    private final MeterRegistry meterRegistry;

    private Counter trendsSuccessCounter;
    private Counter trendsFailureCounter;
    private Timer trendsCalculationTimer;

    @PostConstruct
    public void initMetrics() {
        trendsSuccessCounter = Counter.builder("analytics.trends.success")
                .description("Number of successful trend calculations")
                .register(meterRegistry);

        trendsFailureCounter = Counter.builder("analytics.trends.failure")
                .description("Number of failed trend calculations")
                .register(meterRegistry);

        trendsCalculationTimer = Timer.builder("analytics.trends.time")
                .description("Time taken for trend calculation")
                .register(meterRegistry);
    }

    public TrendsResult calculateTrends(final TrendsRequest request) {
        return trendsCalculationTimer.record(() -> {
            try {
                final String fromCode = request.from().toUpperCase();
                final String toCode = request.to().toUpperCase();

                validateSupportedCurrency(fromCode);
                validateSupportedCurrency(toCode);
                validateMinimumPeriod(fromCode, toCode, request.period());

                final Instant endDate = Instant.now()
                        .truncatedTo(ChronoUnit.DAYS)
                        .plus(1, ChronoUnit.DAYS)
                        .minusMillis(1);
                final Instant startDate = calculateStartDate(endDate, request.period());

                // 1D needs intraday points, which only the currency-service
                // scheduler produces - it keeps the legacy path verbatim.
                // 7D..1Y are served from the daily historical store, with the
                // legacy path as fallback (cold store / Frankfurter down).
                List<RatePoint> rates;
                boolean fromFallback = false;

                if (PERIOD_1D.equals(request.period().trim().toUpperCase())) {
                    final LegacyHistory legacy = fetchLegacyHistory(fromCode, toCode, startDate, endDate);
                    rates = legacy.points();
                    fromFallback = legacy.widened();
                } else {
                    rates = fetchHistoricalPoints(fromCode, toCode, startDate, endDate);
                    if (rates.size() < 2) {
                        log.warn(HISTORICAL_FALLBACK_LOG, rates.size(), fromCode, toCode);
                        final LegacyHistory legacy = fetchLegacyHistory(fromCode, toCode, startDate, endDate);
                        rates = legacy.points();
                        fromFallback = true;
                    }
                }

                if (rates.isEmpty()) {
                    throw new InsufficientDataException(
                            String.format("No exchange rate data available for %s -> %s", fromCode, toCode)
                    );
                }

                if (rates.size() == 1) {
                    throw new InsufficientDataException(
                            String.format("Only one data point available for %s -> %s over period %s; "
                                    + "need at least two to compute a trend",
                                    fromCode, toCode, request.period().toUpperCase())
                    );
                }

                final int maxPoints = getMaxPointsForPeriod(request.period());
                final List<RatePoint> sampled = downsample(rates, maxPoints);

                final RatePoint oldestRate = rates.getFirst();
                final RatePoint newestRate = rates.getLast();

                final BigDecimal changePercentage = calculatePercentageChange(
                        oldestRate.rate(),
                        newestRate.rate()
                );

                log.info("Trend calculated: {} -> {}, change: {}%", fromCode, toCode, changePercentage);

                trendsSuccessCounter.increment();
                final TrendsResponse response = TrendsResponse.success(
                        fromCode,
                        toCode,
                        request.period().toUpperCase(),
                        sampled,
                        oldestRate.rate(),
                        newestRate.rate(),
                        changePercentage,
                        oldestRate.timestamp(),
                        newestRate.timestamp(),
                        rates.size()
                );
                return new TrendsResult(response, fromFallback);
            } catch (Exception e) {
                trendsFailureCounter.increment();
                throw e;
            }
        });
    }

    /**
     * Legacy data acquisition against currency-service, including the
     * widened-window retry. Used directly for 1D and as the fallback source
     * for 7D..1Y when the historical store cannot serve the window.
     */
    private LegacyHistory fetchLegacyHistory(final String fromCode, final String toCode,
                                             final Instant startDate, final Instant endDate) {
        final RateHistoryResponse history = currencyServiceClient
                .getRateHistory(fromCode, toCode, startDate, endDate);
        List<RatePoint> rates = history.points();
        boolean widenedFallback = false;

        if (rates.size() < 2) {
            final List<RatePoint> widened = widenWindow(fromCode, toCode, startDate, endDate);
            if (widened.size() >= 2) {
                rates = widened;
                widenedFallback = true;
                log.info("Used widened fallback window for {} -> {} - {} points",
                        fromCode, toCode, rates.size());
            }
        }
        return new LegacyHistory(rates, widenedFallback);
    }

    /**
     * Daily points from the local historical store (lazily backfilled from
     * Frankfurter). Failures are swallowed and reported as an empty list so
     * the caller falls back to the legacy currency-service path; if that
     * fallback also fails, its own exception taxonomy (404/503) applies.
     */
    private List<RatePoint> fetchHistoricalPoints(final String fromCode, final String toCode,
                                                  final Instant startDate, final Instant endDate) {
        try {
            final List<RatePoint> points = historicalRatesService.getRatePoints(
                    fromCode, toCode,
                    startDate.atZone(ZoneOffset.UTC).toLocalDate(),
                    endDate.atZone(ZoneOffset.UTC).toLocalDate());
            return points != null ? points : List.of();
        } catch (RuntimeException e) {
            log.warn(HISTORICAL_FAILURE_LOG, fromCode, toCode, e.getMessage());
            return List.of();
        }
    }

    private List<RatePoint> widenWindow(final String fromCode, final String toCode,
                                        final Instant originalStart, final Instant endDate) {
        final Duration originalSpan = Duration.between(originalStart, endDate);
        Duration widened = originalSpan.multipliedBy(FALLBACK_WIDEN_FACTOR);
        if (widened.isZero() || widened.isNegative()) {
            widened = FALLBACK_MAX_WINDOW;
        }
        if (widened.compareTo(FALLBACK_MAX_WINDOW) > 0) {
            widened = FALLBACK_MAX_WINDOW;
        }
        final Instant widenedStart = endDate.minus(widened);
        final RateHistoryResponse response =
                currencyServiceClient.getRateHistory(fromCode, toCode, widenedStart, endDate);
        return response != null && response.points() != null ? response.points() : List.of();
    }

    private void validateMinimumPeriod(final String fromCode, final String toCode, final String period) {
        final boolean frankfurterOnlyPair = FRANKFURTER_ONLY_CURRENCIES.contains(fromCode)
                || FRANKFURTER_ONLY_CURRENCIES.contains(toCode);
        if (frankfurterOnlyPair && PERIOD_1D.equals(period.trim().toUpperCase())) {
            throw new MinimumPeriodNotSupportedException(MINIMUM_PERIOD_MESSAGE, MINIMUM_PERIOD);
        }
    }

    private void validateSupportedCurrency(final String currencyCode) {
        final List<String> supportedCurrencies = currencyServiceClient.getSupportedCurrencies();
        if (!supportedCurrencies.contains(currencyCode)) {
            throw new CurrencyNotSupportedException(currencyCode, supportedCurrencies);
        }
    }

    private Instant calculateStartDate(final Instant endDate, final String period) {
        final String normalized = period.trim().toUpperCase();
        final int amount = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        final char unit = normalized.charAt(normalized.length() - 1);

        return switch (unit) {
            case 'D' -> endDate.minus(amount, ChronoUnit.DAYS);
            case 'Y' -> {
                final LocalDate endLocalDate = endDate.atZone(ZoneOffset.UTC).toLocalDate();
                final LocalDate startLocalDate = endLocalDate.minusYears(amount);
                yield startLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            default -> throw new IllegalArgumentException("Invalid period unit: " + unit);
        };
    }

    private BigDecimal calculatePercentageChange(final BigDecimal oldRate, final BigDecimal newRate) {
        return Optional.of(oldRate)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                .map(rate -> newRate.subtract(rate)
                        .divide(rate, CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(CerpsConstants.HUNDRED))
                        .setScale(CerpsConstants.DISPLAY_SCALE, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    static int getMaxPointsForPeriod(final String period) {
        final String normalized = period.trim().toUpperCase();
        return switch (normalized) {
            case "1D" -> CerpsConstants.MAX_POINTS_1D;
            case "7D" -> CerpsConstants.MAX_POINTS_7D;
            case "30D" -> CerpsConstants.MAX_POINTS_30D;
            case "90D" -> CerpsConstants.MAX_POINTS_90D;
            case "180D" -> CerpsConstants.MAX_POINTS_180D;
            case "1Y" -> CerpsConstants.MAX_POINTS_1Y;
            case "2Y" -> CerpsConstants.MAX_POINTS_2Y;
            case "3Y" -> CerpsConstants.MAX_POINTS_3Y;
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }

    static <T> List<T> downsample(final List<T> data, final int maxPoints) {
        if (data.size() <= maxPoints) {
            return data;
        }

        final List<T> result = new ArrayList<>(maxPoints);
        result.add(data.getFirst());

        final int innerPoints = maxPoints - 2;
        for (int i = 1; i <= innerPoints; i++) {
            final int index = (int) Math.round((double) i * (data.size() - 1) / (maxPoints - 1));
            result.add(data.get(index));
        }

        result.add(data.getLast());
        return result;
    }

    public record TrendsResult(TrendsResponse response, boolean fromFallback) {
    }

    private record LegacyHistory(List<RatePoint> points, boolean widened) {
    }
}
