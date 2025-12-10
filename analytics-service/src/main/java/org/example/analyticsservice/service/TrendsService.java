package org.example.analyticsservice.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendsService {

    private static final String LOG_CALCULATING_TRENDS = "Calculating trends for {} -> {} over period {}";
    private static final String LOG_FOUND_DATA_POINTS = "Found {} data points for trend analysis";
    private static final String LOG_TREND_RESULT = "Trend: {} -> {}, Change: {}%";

    private static final String ERROR_INSUFFICIENT_DATA = "Insufficient data for trend analysis. Found %d data points, need at least 2";
    private static final int SCALE = 2;
    private static final int CALCULATION_SCALE = 6;

    private final ExchangeRateRepository exchangeRateRepository;

    public TrendsResponse calculateTrends(final TrendsRequest request) {
        log.info(LOG_CALCULATING_TRENDS, request.from(), request.to(), request.period());

        final Currency fromCurrency = Currency.getInstance(request.from().toUpperCase());
        final Currency toCurrency = Currency.getInstance(request.to().toUpperCase());

        final Instant endDate = Instant.now();
        final Instant startDate = calculateStartDate(endDate, request.period());

        final List<ExchangeRateEntity> rates = exchangeRateRepository.findRatesForPeriod(
                fromCurrency.getCurrencyCode(),
                toCurrency.getCurrencyCode(),
                startDate,
                endDate
        );

        log.info(LOG_FOUND_DATA_POINTS, rates.size());

        if (rates.size() < 2) {
            throw new InsufficientDataException(
                    String.format(ERROR_INSUFFICIENT_DATA, rates.size())
            );
        }

        final ExchangeRateEntity oldestRate = rates.getFirst();
        final ExchangeRateEntity newestRate = rates.getLast();

        final BigDecimal changePercentage = calculatePercentageChange(
                oldestRate.getRate(),
                newestRate.getRate()
        );

        log.info(LOG_TREND_RESULT, request.from(), request.to(), changePercentage);

        return TrendsResponse.success(
                request.from().toUpperCase(),
                request.to().toUpperCase(),
                request.period().toUpperCase(),
                oldestRate.getRate(),
                newestRate.getRate(),
                changePercentage,
                oldestRate.getTimestamp(),
                newestRate.getTimestamp(),
                rates.size()
        );
    }

    private Instant calculateStartDate(final Instant endDate, final String period) {
        final String normalized = period.trim().toUpperCase();
        final int amount = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        final char unit = normalized.charAt(normalized.length() - 1);

        return switch (unit) {
            case 'H' -> endDate.minus(amount, ChronoUnit.HOURS);
            case 'D' -> endDate.minus(amount, ChronoUnit.DAYS);
            case 'M' -> endDate.minus(amount * 30L, ChronoUnit.DAYS);
            case 'Y' -> endDate.minus(amount * 365L, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Invalid period unit: " + unit);
        };
    }

    private BigDecimal calculatePercentageChange(final BigDecimal oldRate, final BigDecimal newRate) {
        return Optional.of(oldRate)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) != 0)
                .map(rate -> newRate.subtract(rate)
                        .divide(rate, CALCULATION_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(SCALE, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }
}
