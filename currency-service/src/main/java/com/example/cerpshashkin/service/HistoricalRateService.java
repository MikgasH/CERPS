package com.example.cerpshashkin.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.client.impl.FrankfurterClient;
import com.example.cerpshashkin.dto.HistoricalRatesResponse;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.exception.HistoricalRatesNotFoundException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a full EUR-based rate snapshot for a calendar date: database
 * first (with a business-week look-back for weekends/holidays), Frankfurter
 * for dates the database does not cover (older than the 395-day retention or
 * before a currency gained provider coverage).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalRateService {

    // Eurozone start — EUR-based rates cannot precede the euro itself.
    static final LocalDate MIN_DATE = LocalDate.of(1999, 1, 4);
    // Bridges long weekends the same way the Frankfurter staleness guard does.
    static final int LOOK_BACK_DAYS = 4;

    // ECB reference rates are fixed around 16:00 CET; used when the database
    // has no snapshot and only the Frankfurter rate date is known.
    private static final LocalTime ECB_FIX_TIME_UTC = LocalTime.of(16, 0);

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    private final ExchangeRateRepository exchangeRateRepository;
    private final FrankfurterClient frankfurterClient;
    private final SupportedCurrenciesService supportedCurrenciesService;

    @Cacheable(value = "historicalRates", key = "#base + '_' + #date")
    public HistoricalRatesResponse getHistoricalRates(final String base, final LocalDate date) {
        validateDate(date);
        Currency.getInstance(base);

        final Set<String> supportedCodes = supportedCurrenciesService.getSupportedCurrencyCodesAsSet();

        final ResolvedSnapshot snapshot = resolveEurSnapshot(date, supportedCodes);

        final Map<String, BigDecimal> rates = base.equals(baseCurrencyCode)
                ? snapshot.eurRates()
                : convertToCrossRates(base, date, snapshot.eurRates(), supportedCodes);

        // Cross-rates cover exactly what the EUR snapshot covers, so the
        // snapshot's completeness carries over to the response unchanged.
        return new HistoricalRatesResponse(
                base, date, rates, snapshot.source(), snapshot.timestamp(), snapshot.complete());
    }

    private void validateDate(final LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date must not be in the future: " + date);
        }
        if (date.isBefore(MIN_DATE)) {
            throw new IllegalArgumentException(
                    "EUR-based rates are not available before " + MIN_DATE + " (eurozone start): " + date);
        }
    }

    private ResolvedSnapshot resolveEurSnapshot(final LocalDate date, final Set<String> supportedCodes) {
        // Per-currency latest snapshot within [date - LOOK_BACK_DAYS, date + 1) UTC:
        // covers the requested day and bridges weekends/holidays in one query.
        final Instant windowStart = date.minusDays(LOOK_BACK_DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant windowEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<ExchangeRateEntity> entities = exchangeRateRepository
                .findLatestPerTargetInWindow(baseCurrencyCode, windowStart, windowEnd);

        final Map<String, BigDecimal> rates = entities.stream()
                .filter(entity -> supportedCodes.contains(entity.getTargetCurrency().getCurrencyCode()))
                .collect(Collectors.toMap(
                        entity -> entity.getTargetCurrency().getCurrencyCode(),
                        ExchangeRateEntity::getRate
                ));

        if (rates.isEmpty()) {
            return resolveFromFrankfurter(date, supportedCodes);
        }

        final Instant snapshotTimestamp = entities.stream()
                .map(ExchangeRateEntity::getTimestamp)
                .max(Comparator.naturalOrder())
                .orElse(windowEnd);

        // Partial DB data (e.g. a currency added after this date never got an
        // older snapshot) is gap-filled from Frankfurter, mirroring the refresh
        // Phase 2 fallback, so the response always covers every supported code.
        fillMissingFromFrankfurter(date, supportedCodes, rates);

        log.info("Historical rates for {} resolved from database - {} rates", date, rates.size());
        return new ResolvedSnapshot(rates, HistoricalRatesResponse.SOURCE_DATABASE, snapshotTimestamp,
                coversAllSupported(rates, supportedCodes));
    }

    /**
     * Supported currencies absent from the database snapshot are fetched from
     * Frankfurter for the same date and merged into {@code rates} in place. A
     * failure here must never discard the DB snapshot we already have, so it is
     * caught and logged — the response then carries the partial set, exactly as
     * before this gap-fill existed.
     */
    private void fillMissingFromFrankfurter(final LocalDate date,
                                            final Set<String> supportedCodes,
                                            final Map<String, BigDecimal> rates) {
        final Set<String> missing = supportedCodes.stream()
                .filter(code -> !code.equals(baseCurrencyCode))
                .filter(code -> !rates.containsKey(code))
                .collect(Collectors.toSet());

        if (missing.isEmpty()) {
            return;
        }

        log.info("Historical snapshot for {} missing {} supported currencies, gap-filling from Frankfurter: {}",
                date, missing.size(), missing);

        try {
            final CurrencyExchangeResponse response = frankfurterClient.getHistoricalRates(date, missing);

            if (!response.success() || response.rates() == null || response.rates().isEmpty()) {
                log.warn("Frankfurter gap-fill returned no rates for {} on {}", missing, date);
                return;
            }

            response.rates().forEach((currency, rate) -> {
                final String code = currency.getCurrencyCode();
                if (missing.contains(code)) {
                    rates.put(code, rate);
                }
            });

            log.info("Historical gap-fill for {} complete - {} rates total", date, rates.size());

        } catch (Exception e) {
            log.warn("Historical gap-fill from Frankfurter failed for {}, returning partial DB snapshot: {}",
                    date, e.getMessage());
        }
    }

    private ResolvedSnapshot resolveFromFrankfurter(final LocalDate date, final Set<String> supportedCodes) {
        log.info("No database rates for {} within look-back window, falling back to Frankfurter", date);

        final Set<String> symbols = supportedCodes.stream()
                .filter(code -> !code.equals(baseCurrencyCode))
                .collect(Collectors.toSet());

        final CurrencyExchangeResponse response = frankfurterClient.getHistoricalRates(date, symbols);

        if (!response.success() || response.rates() == null || response.rates().isEmpty()) {
            throw new HistoricalRatesNotFoundException(baseCurrencyCode, date);
        }

        final Map<String, BigDecimal> rates = response.rates().entrySet().stream()
                .filter(entry -> supportedCodes.contains(entry.getKey().getCurrencyCode()))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCurrencyCode(),
                        Map.Entry::getValue
                ));

        if (rates.isEmpty()) {
            throw new HistoricalRatesNotFoundException(baseCurrencyCode, date);
        }

        final LocalDate rateDate = response.rateDate() != null ? response.rateDate() : date;
        final Instant timestamp = rateDate.atTime(ECB_FIX_TIME_UTC).toInstant(ZoneOffset.UTC);

        log.info("Historical rates for {} resolved from Frankfurter - {} rates", date, rates.size());
        return new ResolvedSnapshot(rates, HistoricalRatesResponse.SOURCE_FRANKFURTER, timestamp,
                coversAllSupported(rates, supportedCodes));
    }

    /**
     * A snapshot is complete when every supported currency (other than the
     * EUR pivot itself) has a rate. Partial snapshots get a short cache TTL
     * (see {@code CacheConfig}) so they heal quickly once the gap-fill
     * provider recovers, instead of being served for the full TTL.
     */
    private boolean coversAllSupported(final Map<String, BigDecimal> eurRates, final Set<String> supportedCodes) {
        return supportedCodes.stream()
                .filter(code -> !code.equals(baseCurrencyCode))
                .allMatch(eurRates::containsKey);
    }

    private Map<String, BigDecimal> convertToCrossRates(final String base,
                                                        final LocalDate date,
                                                        final Map<String, BigDecimal> eurRates,
                                                        final Set<String> supportedCodes) {
        final BigDecimal baseRate = eurRates.get(base);
        if (baseRate == null) {
            throw new HistoricalRatesNotFoundException(base, date);
        }

        final Map<String, BigDecimal> crossRates = new HashMap<>();
        crossRates.put(baseCurrencyCode, crossRate(BigDecimal.ONE, baseRate));

        eurRates.forEach((code, rate) -> {
            if (!code.equals(base) && supportedCodes.contains(code)) {
                crossRates.put(code, crossRate(rate, baseRate));
            }
        });

        return crossRates;
    }

    /**
     * Divides at the intermediate scale and rounds only the published rate to
     * CALCULATION_SCALE, so no significant digits are lost mid-calculation.
     */
    private static BigDecimal crossRate(final BigDecimal eurRate, final BigDecimal baseRate) {
        return eurRate
                .divide(baseRate, CerpsConstants.INTERMEDIATE_CALCULATION_SCALE, RoundingMode.HALF_UP)
                .setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private record ResolvedSnapshot(Map<String, BigDecimal> eurRates, String source, Instant timestamp,
                                    boolean complete) {
    }
}
