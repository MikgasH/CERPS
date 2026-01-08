package com.example.cerpshashkin.service;

import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.entity.ExchangeRateSource;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
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
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String LOG_CACHE_HIT = "Cache HIT: {} -> {} ({})";
    private static final String LOG_CACHE_MISS = "Cache MISS: {} -> {}";
    private static final String LOG_DB_HIT = "Database HIT: {} -> {} ({})";
    private static final String LOG_PROVIDERS_FALLBACK = "Fetching from providers as fallback";
    private static final String LOG_ERROR_REFRESH_RATES = "Failed to refresh exchange rates";
    private static final String LOG_REFRESHING_RATES = "Refreshing exchange rates from providers";
    private static final String LOG_RATES_REFRESHED = "Exchange rates refreshed. Saved {} rates to database";
    private static final String LOG_ERROR_PROVIDERS_FETCH = "Failed to get exchange rates from providers";

    private static final String ERROR_REFRESH_RATES = "Failed to refresh exchange rates: ";
    private static final String ERROR_UNSUCCESSFUL_RESPONSE = "Provider returned unsuccessful response";

    private static final String RATE_TYPE_DIRECT = "direct";
    private static final String RATE_TYPE_INVERSE = "inverse";
    private static final String RATE_TYPE_CROSS_PREFIX = "cross via ";

    private static final int SCALE = 6;
    private static final int MAX_AGE_HOURS = 6;

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    private final ExchangeRateProviderService providerService;
    private final CurrencyRateCache cache;
    private final ExchangeRateRepository exchangeRateRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public Optional<BigDecimal> getExchangeRate(final Currency from, final Currency to) {
        return getFromCache(from, to)
                .or(() -> {
                    log.info(LOG_CACHE_MISS, from.getCurrencyCode(), to.getCurrencyCode());
                    return getFromDatabase(from, to);
                })
                .or(() -> {
                    log.warn(LOG_PROVIDERS_FALLBACK);
                    return getExchangeRateFromProviders(from, to);
                });
    }

    private Optional<BigDecimal> getFromCache(final Currency from, final Currency to) {
        Optional<BigDecimal> direct = cache.getRate(from, to)
                .map(cached -> {
                    logCacheHit(from, to, RATE_TYPE_DIRECT);
                    return cached.rate();
                });

        if (direct.isPresent()) {
            return direct;
        }

        Optional<BigDecimal> inverse = cache.getRate(to, from)
                .map(cached -> {
                    logCacheHit(from, to, RATE_TYPE_INVERSE);
                    return BigDecimal.ONE.divide(cached.rate(), SCALE, RoundingMode.HALF_UP);
                });

        if (inverse.isPresent()) {
            return inverse;
        }

        Currency baseCurrency = Currency.getInstance(baseCurrencyCode);
        Optional<CachedRate> fromRate = cache.getRate(baseCurrency, from);
        Optional<CachedRate> toRate = cache.getRate(baseCurrency, to);

        if (fromRate.isPresent() && toRate.isPresent()) {
            logCacheHit(from, to, RATE_TYPE_CROSS_PREFIX + baseCurrencyCode);
            return Optional.of(toRate.get().rate()
                    .divide(fromRate.get().rate(), SCALE, RoundingMode.HALF_UP));
        }

        return Optional.empty();
    }

    private void logCacheHit(final Currency from, final Currency to, final String rateType) {
        log.info(LOG_CACHE_HIT, from.getCurrencyCode(), to.getCurrencyCode(), rateType);
    }

    private Optional<BigDecimal> getFromDatabase(final Currency from, final Currency to) {
        Instant maxAge = Instant.now().minus(MAX_AGE_HOURS, ChronoUnit.HOURS);

        return exchangeRateRepository.findBestRate(
                from.getCurrencyCode(),
                to.getCurrencyCode(),
                baseCurrencyCode,
                maxAge
        ).map(result -> {
            BigDecimal rate = result.getRate();
            cache.putRate(from, to, rate);
            log.info(LOG_DB_HIT, from.getCurrencyCode(), to.getCurrencyCode(),
                    result.getRateType().toLowerCase());
            return rate;
        });
    }

    @Transactional
    public void refreshRates() {
        log.info(LOG_REFRESHING_RATES);

        try {
            final CurrencyExchangeResponse response = providerService.getLatestRatesFromProviders();

            if (!response.success()) {
                throw new ExchangeRateNotAvailableException(ERROR_REFRESH_RATES + ERROR_UNSUCCESSFUL_RESPONSE);
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
                            .source(ExchangeRateSource.AGGREGATED)
                            .timestamp(now)
                            .build()
                    )
                    .toList();

            if (!response.isMockData()) {
                exchangeRateRepository.saveAll(entities);
            }

            entities.forEach(entity ->
                    cache.putRate(
                            entity.getBaseCurrency(),
                            entity.getTargetCurrency(),
                            entity.getRate()
                    )
            );

            log.info(LOG_RATES_REFRESHED, entities.size());

        } catch (ExchangeRateNotAvailableException e) {
            log.error(LOG_ERROR_REFRESH_RATES, e);
            throw e;
        } catch (Exception e) {
            log.error(LOG_ERROR_REFRESH_RATES, e);
            throw new ExchangeRateNotAvailableException(ERROR_REFRESH_RATES + e.getMessage());
        }
    }

    public void cacheExchangeRates(final CurrencyExchangeResponse response) {
        Optional.ofNullable(response.rates())
                .ifPresent(rates -> rates.forEach((currency, rate) ->
                        cache.putRate(response.base(), currency, rate)
                ));
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
            log.error(LOG_ERROR_PROVIDERS_FETCH, e);
            return Optional.empty();
        }
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
                    .map(rate -> BigDecimal.ONE.divide(rate, SCALE, RoundingMode.HALF_UP));
        }

        return Optional.ofNullable(rates.get(from))
                .flatMap(fromRate -> Optional.ofNullable(rates.get(to))
                        .map(toRate -> toRate.divide(fromRate, SCALE, RoundingMode.HALF_UP))
                );
    }

    private Set<String> getSupportedCurrencyCodes() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .collect(Collectors.toSet());
    }
}
