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
import java.time.Duration;
import java.time.Instant;
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

    private static final String LOG_CACHE_HIT_DIRECT = "Cache HIT: {} -> {} (direct)";
    private static final String LOG_CACHE_HIT_INVERSE = "Cache HIT: {} -> {} (inverse)";
    private static final String LOG_CACHE_HIT_CROSS = "Cache HIT: {} -> {} (cross-rate via {})";
    private static final String LOG_CACHE_MISS = "Cache MISS: {} -> {}";
    private static final String LOG_DB_HIT_DIRECT = "Database HIT: {} -> {} (direct)";
    private static final String LOG_DB_HIT_INVERSE = "Database HIT: {} -> {} (inverse)";
    private static final String LOG_DB_HIT_CROSS = "Database HIT: {} -> {} (cross-rate via {})";
    private static final String LOG_PROVIDERS_FALLBACK = "Fetching from providers as fallback";
    private static final String LOG_ERROR_REFRESH_RATES = "Failed to refresh exchange rates";
    private static final String LOG_INFO_REFRESHING_RATES = "Refreshing exchange rates from providers";
    private static final String LOG_INFO_RATES_REFRESHED = "Exchange rates refreshed. Saved {} rates to database";
    private static final String LOG_ERROR_PROVIDERS_FETCH = "Failed to get exchange rates from providers";
    private static final String ERROR_REFRESH_RATES_MESSAGE = "Failed to refresh exchange rates: ";
    private static final String UNSUCCESSFUL_RESPONSE = "Provider returned unsuccessful response";
    private static final int SCALE = 6;
    private static final int MAX_AGE_HOURS = 6;

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    private final ExchangeRateProviderService providerService;
    private final CurrencyRateCache cache;
    private final ExchangeRateRepository exchangeRateRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public Optional<BigDecimal> getExchangeRate(final Currency from, final Currency to) {
        return cache.getRate(from, to)
                .map(cachedRate -> {
                    log.info(LOG_CACHE_HIT_DIRECT, from.getCurrencyCode(), to.getCurrencyCode());
                    return cachedRate.rate();
                })
                .or(() -> calculateInverseFromCache(from, to))
                .or(() -> calculateCrossRateFromCache(from, to))
                .or(() -> {
                    log.warn(LOG_CACHE_MISS, from.getCurrencyCode(), to.getCurrencyCode());
                    return getFromDatabase(from, to);
                })
                .or(() -> {
                    log.warn(LOG_PROVIDERS_FALLBACK);
                    return getExchangeRateFromProviders(from, to);
                });
    }

    @Transactional
    public void refreshRates() {
        log.info(LOG_INFO_REFRESHING_RATES);

        try {
            final CurrencyExchangeResponse response = providerService.getLatestRatesFromProviders();

            if (!response.success()) {
                throw new ExchangeRateNotAvailableException(ERROR_REFRESH_RATES_MESSAGE + UNSUCCESSFUL_RESPONSE);
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

            log.info(LOG_INFO_RATES_REFRESHED, entities.size());

        } catch (ExchangeRateNotAvailableException e) {
            log.error(LOG_ERROR_REFRESH_RATES, e);
            throw e;
        } catch (Exception e) {
            log.error(LOG_ERROR_REFRESH_RATES, e);
            throw new ExchangeRateNotAvailableException(ERROR_REFRESH_RATES_MESSAGE + e.getMessage());
        }
    }

    public void cacheExchangeRates(final CurrencyExchangeResponse response) {
        Optional.ofNullable(response.rates())
                .ifPresent(rates -> rates.forEach((currency, rate) ->
                        cache.putRate(response.base(), currency, rate)
                ));
    }

    private Optional<BigDecimal> getFromDatabase(final Currency from, final Currency to) {
        Optional<BigDecimal> direct = exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(from, to)
                .filter(this::isNotTooOld)
                .map(entity -> {
                    final BigDecimal rate = entity.getRate();
                    cache.putRate(from, to, rate);
                    log.info(LOG_DB_HIT_DIRECT, from.getCurrencyCode(), to.getCurrencyCode());
                    return rate;
                });

        if (direct.isPresent()) {
            return direct;
        }

        Optional<BigDecimal> inverse = exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(to, from)
                .filter(this::isNotTooOld)
                .map(entity -> {
                    final BigDecimal rate = BigDecimal.ONE.divide(
                            entity.getRate(),
                            SCALE,
                            RoundingMode.HALF_UP
                    );
                    cache.putRate(from, to, rate);
                    log.info(LOG_DB_HIT_INVERSE, from.getCurrencyCode(), to.getCurrencyCode());
                    return rate;
                });

        if (inverse.isPresent()) {
            return inverse;
        }

        return calculateCrossRateFromDatabase(from, to);
    }

    private Optional<BigDecimal> calculateCrossRateFromDatabase(
            final Currency from,
            final Currency to
    ) {
        final Currency base = Currency.getInstance(baseCurrencyCode);

        final Optional<ExchangeRateEntity> fromEntity = exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(base, from)
                .filter(this::isNotTooOld);

        final Optional<ExchangeRateEntity> toEntity = exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(base, to)
                .filter(this::isNotTooOld);

        if (fromEntity.isPresent() && toEntity.isPresent()) {
            final BigDecimal fromRate = fromEntity.get().getRate();
            final BigDecimal toRate = toEntity.get().getRate();

            final BigDecimal crossRate = toRate.divide(fromRate, SCALE, RoundingMode.HALF_UP);

            cache.putRate(from, to, crossRate);
            log.info(LOG_DB_HIT_CROSS, from.getCurrencyCode(), to.getCurrencyCode(), base.getCurrencyCode());

            return Optional.of(crossRate);
        }

        return Optional.empty();
    }

    private boolean isNotTooOld(final ExchangeRateEntity entity) {
        final Duration age = Duration.between(entity.getTimestamp(), Instant.now());
        return age.toHours() < MAX_AGE_HOURS;
    }

    private Optional<BigDecimal> calculateInverseFromCache(final Currency from, final Currency to) {
        return cache.getRate(to, from)
                .map(reverseCachedRate -> {
                    final BigDecimal inverseRate = BigDecimal.ONE.divide(
                            reverseCachedRate.rate(),
                            SCALE,
                            RoundingMode.HALF_UP
                    );
                    log.info(LOG_CACHE_HIT_INVERSE, from.getCurrencyCode(), to.getCurrencyCode());
                    return inverseRate;
                });
    }

    private Optional<BigDecimal> calculateCrossRateFromCache(final Currency from, final Currency to) {
        final Currency baseCurrency = Currency.getInstance(baseCurrencyCode);

        final Optional<BigDecimal> fromRate = cache.getRate(baseCurrency, from)
                .map(CachedRate::rate);

        final Optional<BigDecimal> toRate = cache.getRate(baseCurrency, to)
                .map(CachedRate::rate);

        if (fromRate.isPresent() && toRate.isPresent()) {
            final BigDecimal crossRate = toRate.get()
                    .divide(fromRate.get(), SCALE, RoundingMode.HALF_UP);

            log.info(LOG_CACHE_HIT_CROSS, from.getCurrencyCode(), to.getCurrencyCode(),
                    baseCurrency.getCurrencyCode());
            return Optional.of(crossRate);
        }

        return Optional.empty();
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
