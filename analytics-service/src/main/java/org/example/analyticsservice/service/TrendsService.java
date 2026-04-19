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

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendsService {

    static final int FALLBACK_WIDEN_FACTOR = 2;
    static final Duration FALLBACK_MAX_WINDOW = Duration.ofDays(30);
    private static final int MAX_POINTS_1Y = 365;

    private final CurrencyServiceClient currencyServiceClient;
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

                final Instant endDate = Instant.now()
                        .truncatedTo(ChronoUnit.DAYS)
                        .plus(1, ChronoUnit.DAYS)
                        .minusMillis(1);
                final Instant startDate = calculateStartDate(endDate, request.period());

                RateHistoryResponse history = currencyServiceClient
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
                return new TrendsResult(response, widenedFallback);
            } catch (Exception e) {
                trendsFailureCounter.increment();
                throw e;
            }
        });
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
            case 'H' -> endDate.minus(amount, ChronoUnit.HOURS);
            case 'D' -> endDate.minus(amount, ChronoUnit.DAYS);
            case 'M' -> {
                final LocalDate endLocalDate = endDate.atZone(ZoneOffset.UTC).toLocalDate();
                final LocalDate startLocalDate = endLocalDate.minusMonths(amount);
                yield startLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
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
            case "1Y" -> MAX_POINTS_1Y;
            default -> CerpsConstants.MAX_POINTS_180D;
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
}
