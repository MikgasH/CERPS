package com.example.cerpshashkin.service;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.exception.AllProvidersFailedException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequiredArgsConstructor
public class ExchangeRateProviderService {

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRateProviderService.class);

    private static final String PROVIDER_SUCCESS_MESSAGE = "Successfully got rates from provider: {}";
    private static final String PROVIDER_FAILED_MESSAGE = "Provider {} failed with error: {}";
    private static final String TRYING_PROVIDER_MESSAGE = "Trying to get rates from provider: {}";
    private static final String COLLECTING_RATES_MESSAGE = "Collecting rates from all available providers";
    private static final String COLLECTED_RATES_MESSAGE = "Collected rates from {} real providers";
    private static final String SELECTING_BEST_RATES_MESSAGE = "Selecting median rates for {} currency pairs";
    private static final String NORMALIZING_TO_BASE_MESSAGE = "Normalizing {} responses to base currency: {}";
    private static final String USING_FALLBACK_MESSAGE = "All real providers failed, using fallback mock services";
    private static final String USING_SINGLE_PROVIDER_MESSAGE = "Single provider response, using directly";
    private static final String USING_MOCK_SERVICES_MESSAGE = "Using {} mock services for fallback";
    private static final String NORMALIZING_SINGLE_PROVIDER = "Normalizing single provider from {} to {}";
    private static final String MEDIAN_EMPTY_LIST_ERROR = "Cannot calculate median of empty list";
    private static final String LOG_WARN_CONVERSION_RATE_NOT_FOUND = "Cannot find conversion rate from {} to {}, skipping normalization";
    private static final int SCALE = 6;

    @Value("${exchange-rates.base-currency:EUR}")
    private String baseCurrencyCode;

    private final List<ExchangeRateClient> clients;

    public CurrencyExchangeResponse getLatestRatesFromProviders() {
        LOG.info(COLLECTING_RATES_MESSAGE);

        final List<CurrencyExchangeResponse> realProviderResponses = collectRatesFromRealProviders();

        if (!realProviderResponses.isEmpty()) {
            LOG.info(COLLECTED_RATES_MESSAGE, realProviderResponses.size());
            return aggregateRates(realProviderResponses, false);
        }

        LOG.warn(USING_FALLBACK_MESSAGE);
        return getFallbackRatesFromMockServices();
    }

    private List<CurrencyExchangeResponse> collectRatesFromRealProviders() {
        return clients.stream()
                .filter(client -> isRealProvider(client.getProviderName()))
                .map(this::tryGetRatesFromClient)
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean isRealProvider(final String providerName) {
        return !providerName.equals(ApiProvider.MOCK_SERVICE_1.getDisplayName())
                && !providerName.equals(ApiProvider.MOCK_SERVICE_2.getDisplayName());
    }

    private Optional<CurrencyExchangeResponse> tryGetRatesFromClient(final ExchangeRateClient client) {
        try {
            LOG.info(TRYING_PROVIDER_MESSAGE, client.getProviderName());

            final CurrencyExchangeResponse response = client.getLatestRates();

            return Optional.of(response)
                    .filter(CurrencyExchangeResponse::success)
                    .filter(r -> r.rates() != null && !r.rates().isEmpty())
                    .map(r -> {
                        LOG.info(PROVIDER_SUCCESS_MESSAGE, client.getProviderName());
                        return r;
                    });

        } catch (Exception e) {
            LOG.warn(PROVIDER_FAILED_MESSAGE, client.getProviderName(), e.getMessage());
            return Optional.empty();
        }
    }

    private CurrencyExchangeResponse getFallbackRatesFromMockServices() {
        final List<CurrencyExchangeResponse> mockResponses = clients.stream()
                .filter(client -> !isRealProvider(client.getProviderName()))
                .map(this::tryGetRatesFromClient)
                .flatMap(Optional::stream)
                .toList();

        if (mockResponses.isEmpty()) {
            throw new AllProvidersFailedException(
                    clients.stream()
                            .map(ExchangeRateClient::getProviderName)
                            .toList()
            );
        }

        LOG.info(USING_MOCK_SERVICES_MESSAGE, mockResponses.size());
        return aggregateRates(mockResponses, true);
    }

    private CurrencyExchangeResponse aggregateRates(
            final List<CurrencyExchangeResponse> allResponses,
            final boolean isMockData) {

        final Currency targetBase = getTargetBaseCurrency();

        if (allResponses.size() == 1) {
            final CurrencyExchangeResponse response = allResponses.getFirst();

            if (response.base().equals(targetBase)) {
                LOG.info(USING_SINGLE_PROVIDER_MESSAGE);
                return new CurrencyExchangeResponse(
                        response.success(),
                        response.lastUpdated(),
                        response.base(),
                        response.rateDate(),
                        response.rates(),
                        isMockData
                );
            }

            LOG.info(NORMALIZING_SINGLE_PROVIDER, response.base(), targetBase);
            return selectMedianRates(allResponses, isMockData);
        }

        return selectMedianRates(allResponses, isMockData);
    }

    private CurrencyExchangeResponse selectMedianRates(
            final List<CurrencyExchangeResponse> allResponses,
            final boolean isMockData) {

        final Currency targetBase = getTargetBaseCurrency();

        LOG.info(NORMALIZING_TO_BASE_MESSAGE, allResponses.size(), targetBase);

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

        LOG.info(SELECTING_BEST_RATES_MESSAGE, medianRates.size());

        return CurrencyExchangeResponse.success(
                targetBase,
                allResponses.getFirst().rateDate(),
                medianRates,
                isMockData
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
                    LOG.warn(LOG_WARN_CONVERSION_RATE_NOT_FOUND,
                            sourceBase, targetBase);
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
                .orElseThrow(() -> new IllegalArgumentException(MEDIAN_EMPTY_LIST_ERROR));
    }
}
