package com.example.cerpshashkin.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerpshashkin.dto.CurrentRatesResponse;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.exception.ExchangeRateNotAvailableException;
import com.example.cerpshashkin.model.CachedRate;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.cache.CurrencyRateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateService {

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    // Must exceed the scheduler interval (8h) with headroom, otherwise reads return empty between refreshes.
    @Value("${exchange-rates.max-age-hours:24}")
    private int maxAgeHours;

    private final ExchangeRateProviderService providerService;
    private final CurrencyRateCache cache;
    private final ExchangeRateRepository exchangeRateRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public Optional<BigDecimal> getExchangeRate(final Currency from, final Currency to) {
        return getFromCache(from, to)
                .or(() -> getFromDatabase(from, to))
                .or(() -> getExchangeRateFromProviders(from, to));
    }

    public CurrentRatesResponse getAllRatesForBase(final String baseCode) {
        final String normalized = baseCode.trim().toUpperCase();
        Currency.getInstance(normalized);
        final Currency eurCurrency = Currency.getInstance(baseCurrencyCode);
        final Instant maxAge = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);

        // Single batch query: fetch all latest EUR-based rates in one DB roundtrip
        final List<ExchangeRateEntity> latestRates = exchangeRateRepository
                .findAllLatestByBaseCurrency(baseCurrencyCode, maxAge);

        final Map<String, BigDecimal> eurBasedRates = latestRates.stream()
                .collect(Collectors.toMap(
                        e -> e.getTargetCurrency().getCurrencyCode(),
                        ExchangeRateEntity::getRate
                ));

        // Cache the fetched rates
        latestRates.forEach(entity ->
                cache.putRate(eurCurrency, entity.getTargetCurrency(), entity.getRate()));

        final Set<String> supportedCodes = supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .collect(Collectors.toSet());

        final Map<String, BigDecimal> rates;

        if (normalized.equals(baseCurrencyCode)) {
            // Base is EUR — use rates directly
            rates = eurBasedRates.entrySet().stream()
                    .filter(e -> supportedCodes.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            // Cross-rate calculation through EUR
            final BigDecimal baseRate = eurBasedRates.get(normalized);
            if (baseRate == null) {
                throw new ExchangeRateNotAvailableException(baseCurrencyCode, normalized);
            }

            rates = supportedCodes.stream()
                    .filter(code -> !code.equals(normalized))
                    .collect(Collectors.toMap(
                            code -> code,
                            code -> {
                                if (code.equals(baseCurrencyCode)) {
                                    return BigDecimal.ONE.divide(baseRate, CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP);
                                }
                                final BigDecimal targetRate = eurBasedRates.get(code);
                                if (targetRate == null) {
                                    throw new ExchangeRateNotAvailableException(normalized, code);
                                }
                                return targetRate.divide(baseRate, CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP);
                            }
                    ));
        }

        return CurrentRatesResponse.success(normalized, rates);
    }

    @Transactional
    public void refreshRates() {
        log.info("Refreshing exchange rates");

        try {
            final CurrencyExchangeResponse response = providerService.getLatestRatesFromProviders();

            if (!response.success()) {
                log.warn("Provider returned unsuccessful response, keeping existing cache");
                return;
            }

            cache.clearCache();

            final Set<String> supportedCodes = getSupportedCurrencyCodes();
            final Instant now = Instant.now();
            final Currency baseCurrencyObj = Currency.getInstance(baseCurrencyCode);

            final List<ExchangeRateEntity> entities = response.rates().entrySet()
                    .stream()
                    .filter(entry -> supportedCodes.contains(entry.getKey().getCurrencyCode()))
                    .filter(entry -> !entry.getKey().equals(baseCurrencyObj))
                    .map(entry -> ExchangeRateEntity.builder()
                            .id(UUID.randomUUID())
                            .baseCurrency(baseCurrencyObj)
                            .targetCurrency(entry.getKey())
                            .rate(entry.getValue())
                            .source(CerpsConstants.EXCHANGE_RATE_SOURCE_AGGREGATED)
                            .timestamp(now)
                            .build()
                    )
                    .toList();

            exchangeRateRepository.saveAll(entities);

            entities.forEach(entity ->
                    cache.putRate(
                            entity.getBaseCurrency(),
                            entity.getTargetCurrency(),
                            entity.getRate()
                    )
            );

            log.info("Exchange rates refreshed - {} rates saved", entities.size());

        } catch (AllProvidersFailedException e) {
            log.error("All providers failed, keeping existing cache and database data", e);
        } catch (Exception e) {
            log.error("Failed to refresh exchange rates, keeping existing data", e);
        }
    }

    private Optional<BigDecimal> getFromCache(final Currency from, final Currency to) {
        final Optional<BigDecimal> direct = cache.getRate(from, to)
                .map(CachedRate::rate);

        if (direct.isPresent()) {
            return direct;
        }

        final Optional<BigDecimal> inverse = cache.getRate(to, from)
                .map(cached -> BigDecimal.ONE.divide(cached.rate(), CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP));

        if (inverse.isPresent()) {
            return inverse;
        }

        final Currency baseCurrency = Currency.getInstance(baseCurrencyCode);
        final Optional<CachedRate> fromRate = cache.getRate(baseCurrency, from);
        final Optional<CachedRate> toRate = cache.getRate(baseCurrency, to);

        if (fromRate.isPresent() && toRate.isPresent()) {
            return Optional.of(toRate.get().rate()
                    .divide(fromRate.get().rate(), CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP));
        }

        return Optional.empty();
    }

    private Optional<BigDecimal> getFromDatabase(final Currency from, final Currency to) {
        final Instant maxAge = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);

        return exchangeRateRepository.findBestRate(
                from.getCurrencyCode(),
                to.getCurrencyCode(),
                baseCurrencyCode,
                maxAge
        ).map(result -> {
            final BigDecimal rate = result.getRate();
            cache.putRate(from, to, rate);
            return rate;
        });
    }

    private Optional<BigDecimal> getExchangeRateFromProviders(final Currency from, final Currency to) {
        try {
            return Optional.of(providerService.getLatestRatesFromProviders())
                    .filter(CurrencyExchangeResponse::success)
                    .filter(response -> response.rates() != null)
                    .flatMap(response -> {
                        cacheExchangeRates(response);
                        return calculateExchangeRate(from, to, response);
                    });
        } catch (Exception e) {
            log.error("Failed to get rates from providers", e);
            return Optional.empty();
        }
    }

    private void cacheExchangeRates(final CurrencyExchangeResponse response) {
        Optional.ofNullable(response.rates())
                .ifPresent(rates -> rates.forEach((currency, rate) ->
                        cache.putRate(response.base(), currency, rate)
                ));
    }

    private Optional<BigDecimal> calculateExchangeRate(
            final Currency from,
            final Currency to,
            final CurrencyExchangeResponse response) {

        final Currency base = response.base();
        final Map<Currency, BigDecimal> rates = response.rates();

        if (from.equals(base)) {
            return Optional.ofNullable(rates.get(to));
        }

        if (to.equals(base)) {
            return Optional.ofNullable(rates.get(from))
                    .map(rate -> BigDecimal.ONE.divide(rate, CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP));
        }

        return Optional.ofNullable(rates.get(from))
                .flatMap(fromRate -> Optional.ofNullable(rates.get(to))
                        .map(toRate -> toRate.divide(fromRate, CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP))
                );
    }

    private Set<String> getSupportedCurrencyCodes() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .collect(Collectors.toSet());
    }
}
