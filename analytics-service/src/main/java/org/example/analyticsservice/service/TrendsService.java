package org.example.analyticsservice.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.example.analyticsservice.entity.SupportedCurrencyEntity;
import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.repository.ExchangeRateRepository;
import org.example.analyticsservice.repository.SupportedCurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendsService {

    private static final String LOG_CALCULATING_TRENDS = "Calculating trends for {} -> {} over period {}";
    private static final String LOG_FOUND_DATA_POINTS = "Found {} data points for trend analysis";
    private static final String LOG_TREND_RESULT = "Trend: {} -> {}, Change: {}%";
    private static final String LOG_USING_ALL_AVAILABLE = "Requested period has no data, using all available data ({} points)";
    private static final String LOG_SINGLE_DATA_POINT = "Only 1 data point available, returning with 0% change";
    private static final String ERROR_NO_DATA = "No exchange rate data available for %s -> %s. " +
            "Please ensure rates have been fetched at least once.";
    private static final String ERROR_INVALID_PERIOD_UNIT = "Invalid period unit: ";

    private static final int SCALE = 2;
    private static final int CALCULATION_SCALE = 6;
    private static final long DAYS_IN_MONTH = 30L;
    private static final long DAYS_IN_YEAR = 365L;
    private static final int HUNDRED = 100;

    private final ExchangeRateRepository exchangeRateRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;
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

    public TrendsResponse calculateTrends(final TrendsRequest request) {
        return trendsCalculationTimer.record(() -> {
            try {
                log.info(LOG_CALCULATING_TRENDS, request.from(), request.to(), request.period());

                final String fromCode = request.from().toUpperCase();
                final String toCode = request.to().toUpperCase();

                validateSupportedCurrency(fromCode);
                validateSupportedCurrency(toCode);

                final Instant endDate = Instant.now();
                final Instant startDate = calculateStartDate(endDate, request.period());

                List<ExchangeRateEntity> rates = convertToEntityList(
                        exchangeRateRepository.findRatesWithCrossSupport(fromCode, toCode, startDate, endDate)
                );

                log.info(LOG_FOUND_DATA_POINTS, rates.size());

                if (rates.size() < 2) {
                    rates = convertToEntityList(
                            exchangeRateRepository.findRatesWithCrossSupport(fromCode, toCode, null, null)
                    );
                    log.info(LOG_USING_ALL_AVAILABLE, rates.size());
                }

                if (rates.isEmpty()) {
                    throw new InsufficientDataException(
                            String.format(ERROR_NO_DATA, fromCode, toCode)
                    );
                }

                if (rates.size() == 1) {
                    final ExchangeRateEntity singleRate = rates.getFirst();
                    log.info(LOG_SINGLE_DATA_POINT);

                    trendsSuccessCounter.increment();
                    return TrendsResponse.success(
                            fromCode,
                            toCode,
                            request.period().toUpperCase(),
                            singleRate.getRate(),
                            singleRate.getRate(),
                            BigDecimal.ZERO,
                            singleRate.getTimestamp(),
                            singleRate.getTimestamp(),
                            1
                    );
                }

                final ExchangeRateEntity oldestRate = rates.getFirst();
                final ExchangeRateEntity newestRate = rates.getLast();

                final BigDecimal changePercentage = calculatePercentageChange(
                        oldestRate.getRate(),
                        newestRate.getRate()
                );

                log.info(LOG_TREND_RESULT, fromCode, toCode, changePercentage);

                trendsSuccessCounter.increment();
                return TrendsResponse.success(
                        fromCode,
                        toCode,
                        request.period().toUpperCase(),
                        oldestRate.getRate(),
                        newestRate.getRate(),
                        changePercentage,
                        oldestRate.getTimestamp(),
                        newestRate.getTimestamp(),
                        rates.size()
                );
            } catch (Exception e) {
                trendsFailureCounter.increment();
                throw e;
            }
        });
    }

    private void validateSupportedCurrency(final String currencyCode) {
        if (!supportedCurrencyRepository.existsByCurrencyCode(currencyCode)) {
            final List<String> supportedCurrencies = getSupportedCurrencies();
            throw new CurrencyNotSupportedException(currencyCode, supportedCurrencies);
        }
    }

    private List<String> getSupportedCurrencies() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .sorted()
                .toList();
    }

    private Instant calculateStartDate(final Instant endDate, final String period) {
        final String normalized = period.trim().toUpperCase();
        final int amount = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        final char unit = normalized.charAt(normalized.length() - 1);

        return switch (unit) {
            case 'H' -> endDate.minus(amount, ChronoUnit.HOURS);
            case 'D' -> endDate.minus(amount, ChronoUnit.DAYS);
            case 'M' -> endDate.minus(amount * DAYS_IN_MONTH, ChronoUnit.DAYS);
            case 'Y' -> endDate.minus(amount * DAYS_IN_YEAR, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException(ERROR_INVALID_PERIOD_UNIT + unit);
        };
    }

    private BigDecimal calculatePercentageChange(final BigDecimal oldRate, final BigDecimal newRate) {
        return Optional.of(oldRate)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                .map(rate -> newRate.subtract(rate)
                        .divide(rate, CALCULATION_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(HUNDRED))
                        .setScale(SCALE, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    private List<ExchangeRateEntity> convertToEntityList(final List<Object[]> results) {
        return results.stream()
                .map(this::convertToEntity)
                .toList();
    }

    private ExchangeRateEntity convertToEntity(final Object[] row) {
        final UUID id = row[0] instanceof String
                ? UUID.fromString((String) row[0])
                : (UUID) row[0];

        final Object timestampObj = row[5];
        final Instant timestamp;
        if (timestampObj instanceof java.sql.Timestamp) {
            timestamp = ((java.sql.Timestamp) timestampObj).toInstant();
        } else if (timestampObj instanceof java.time.OffsetDateTime) {
            timestamp = ((java.time.OffsetDateTime) timestampObj).toInstant();
        } else {
            timestamp = (Instant) timestampObj;
        }

        return ExchangeRateEntity.builder()
                .id(id)
                .baseCurrency((String) row[1])
                .targetCurrency((String) row[2])
                .rate((BigDecimal) row[3])
                .source((String) row[4])
                .timestamp(timestamp)
                .build();
    }
}
