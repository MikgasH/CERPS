package com.example.cerpshashkin.service;

import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateProviderService {

    private static final int SCALE = 6;
    private static final BigDecimal MAX_RATE = BigDecimal.valueOf(1_000_000);

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    private final List<ExchangeRateClient> clients;

    public CurrencyExchangeResponse getLatestRatesFromProviders() {
        log.info("Collecting rates from providers");

        final List<CurrencyExchangeResponse> responses = collectRatesFromProviders();

        if (responses.isEmpty()) {
            log.error("All providers failed");
            throw new AllProvidersFailedException(
                    clients.stream()
                            .map(ExchangeRateClient::getProviderName)
                            .toList()
            );
        }

        log.info("Collected rates from {} providers", responses.size());
        return aggregateRates(responses);
    }

    private List<CurrencyExchangeResponse> collectRatesFromProviders() {
        return clients.stream()
                .map(this::tryGetRatesFromClient)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<CurrencyExchangeResponse> tryGetRatesFromClient(final ExchangeRateClient client) {
        try {
            log.info("Trying provider: {}", client.getProviderName());

            final CurrencyExchangeResponse response = client.getLatestRates();

            return Optional.of(response)
                    .filter(CurrencyExchangeResponse::success)
                    .filter(r -> r.rates() != null && !r.rates().isEmpty())
                    .map(r -> filterValidRates(r, client.getProviderName()))
                    .filter(r -> !r.rates().isEmpty())
                    .map(r -> {
                        log.info("Provider {} succeeded", client.getProviderName());
                        return r;
                    });

        } catch (Exception e) {
            log.warn("Provider {} failed: {}", client.getProviderName(), e.getMessage());
            return Optional.empty();
        }
    }

    private CurrencyExchangeResponse filterValidRates(
            final CurrencyExchangeResponse response, final String providerName) {
        final Map<Currency, BigDecimal> validRates = response.rates().entrySet().stream()
                .filter(entry -> {
                    final BigDecimal rate = entry.getValue();
                    if (!isValidRate(rate)) {
                        log.warn("Invalid rate from provider {}: {} = {} — skipping",
                                providerName, entry.getKey().getCurrencyCode(), rate);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return CurrencyExchangeResponse.success(
                response.base(), response.rateDate(), validRates, response.isMockData());
    }

    private boolean isValidRate(final BigDecimal rate) {
        if (rate == null) {
            return false;
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (rate.compareTo(MAX_RATE) >= 0) {
            return false;
        }
        try {
            final double doubleVal = rate.doubleValue();
            return Double.isFinite(doubleVal);
        } catch (Exception e) {
            return false;
        }
    }

    private CurrencyExchangeResponse aggregateRates(final List<CurrencyExchangeResponse> allResponses) {
        final Currency targetBase = getTargetBaseCurrency();

        if (allResponses.size() == 1) {
            final CurrencyExchangeResponse response = allResponses.getFirst();

            if (response.base().equals(targetBase)) {
                log.info("Single provider response, using directly");
                return response;
            }

            log.info("Normalizing single provider from {} to {}", response.base(), targetBase);
            return selectMedianRates(allResponses);
        }

        return selectMedianRates(allResponses);
    }

    private CurrencyExchangeResponse selectMedianRates(final List<CurrencyExchangeResponse> allResponses) {
        final Currency targetBase = getTargetBaseCurrency();

        log.info("Normalizing {} responses to base currency: {}", allResponses.size(), targetBase);

        final Map<Currency, List<BigDecimal>> normalizedRatesByCurrency = allResponses.stream()
                .map(response -> normalizeRatesToBase(response, targetBase))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(rates -> rates.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toList()
                        )
                ));

        final Map<Currency, BigDecimal> medianRates = normalizedRatesByCurrency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateMedian(entry.getValue())
                ));

        log.info("Aggregated {} currency pairs", medianRates.size());

        return CurrencyExchangeResponse.success(
                targetBase,
                allResponses.getFirst().rateDate(),
                medianRates,
                false
        );
    }

    private Currency getTargetBaseCurrency() {
        return Currency.getInstance(baseCurrencyCode);
    }

    private Optional<Map<Currency, BigDecimal>> normalizeRatesToBase(
            final CurrencyExchangeResponse response,
            final Currency targetBase) {

        final Currency sourceBase = response.base();
        final Map<Currency, BigDecimal> sourceRates = response.rates();

        if (sourceRates == null || sourceRates.isEmpty()) {
            return Optional.empty();
        }

        if (sourceBase.equals(targetBase)) {
            return Optional.of(new HashMap<>(sourceRates));
        }

        return Optional.ofNullable(sourceRates.get(targetBase))
                .map(conversionFactor -> {
                    final Map<Currency, BigDecimal> normalizedRates = sourceRates.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(targetBase))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue()
                                            .divide(conversionFactor, SCALE, RoundingMode.HALF_UP)
                            ));

                    normalizedRates.put(
                            sourceBase,
                            BigDecimal.ONE.divide(conversionFactor, SCALE, RoundingMode.HALF_UP)
                    );

                    return normalizedRates;
                })
                .or(() -> {
                    log.warn("Cannot find conversion rate from {} to {}", sourceBase, targetBase);
                    return Optional.empty();
                });
    }

    private BigDecimal calculateMedian(final List<BigDecimal> values) {
        return Optional.of(values)
                .filter(list -> !list.isEmpty())
                .map(list -> {
                    if (list.size() == 1) {
                        return list.getFirst();
                    }

                    final List<BigDecimal> sorted = list.stream()
                            .sorted()
                            .toList();

                    final int size = sorted.size();
                    final int middle = size / 2;

                    if (size % 2 == 0) {
                        return sorted.get(middle - 1)
                                .add(sorted.get(middle))
                                .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
                    }
                    return sorted.get(middle);
                })
                .orElseThrow(() -> new IllegalArgumentException("Cannot calculate median of empty list"));
    }
}
