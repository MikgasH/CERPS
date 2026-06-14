package com.example.cerpshashkin.client.impl;

import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.converter.ExternalApiConverter;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Gap-fill provider: free ECB-based rates, no API key. Excluded from the
 * median aggregation ({@link #isFallback()}) — queried only for currencies
 * the primary providers did not return.
 *
 * <p>Frankfurter v2 exposes a single {@code /rates} endpoint returning a flat
 * JSON array of per-pair entries; without a {@code date} parameter it serves
 * the latest available rates (there is no {@code /latest} path in v2).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FrankfurterClient implements ExchangeRateClient {

    private static final String RATES_ENDPOINT = "/rates";
    private static final String BASE_PARAM = "base";
    private static final String QUOTES_PARAM = "quotes";
    private static final String DATE_PARAM = "date";
    private static final String BASE_CURRENCY = "EUR";

    private static final ParameterizedTypeReference<List<FrankfurterRateEntry>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    // Some central banks pause publication around long holidays; older data is
    // worse than no data because it would be presented as fresh.
    private static final int MAX_STALE_BUSINESS_DAYS = 4;

    private static final String OPERATION_LATEST = "fetch latest exchange rates";
    private static final String OPERATION_HISTORICAL = "fetch historical exchange rates";

    private static final String FETCHING_LATEST_LOG = "Fetching latest rates from {}";
    private static final String FETCHING_HISTORICAL_LOG = "Fetching historical rates from {} for date {}";
    private static final String SUCCESS_LOG = "Successfully received response from Frankfurter";
    private static final String STALE_ENTRIES_LOG =
            "Dropped {} Frankfurter entries older than {} business days";

    private static final String NULL_RESPONSE_ERROR = "Null response received";
    private static final String HTTP_ERROR_PREFIX = "HTTP error: ";

    private final RestClient frankfurterRestClient;
    private final ExternalApiConverter converter;
    private final MeterRegistry meterRegistry;

    @Override
    public CurrencyExchangeResponse getLatestRates() {
        return getLatestRates(Set.of());
    }

    @Override
    @CircuitBreaker(name = "frankfurterClient")
    @Retry(name = "frankfurterClient")
    public CurrencyExchangeResponse getLatestRates(final Set<String> symbols) {
        log.info(FETCHING_LATEST_LOG, getProviderName());

        final List<FrankfurterRateEntry> entries = executeRequest(OPERATION_LATEST, uriBuilder -> {
            uriBuilder.path(RATES_ENDPOINT).queryParam(BASE_PARAM, BASE_CURRENCY);
            appendQuotes(uriBuilder, symbols);
            return uriBuilder.build();
        });

        validateResponse(entries, OPERATION_LATEST);

        final LocalDate today = LocalDate.now();
        final List<FrankfurterRateEntry> fresh = entries.stream()
                .filter(entry -> entry.date() != null && !isStale(entry.date(), today))
                .toList();

        if (fresh.size() < entries.size()) {
            log.warn(STALE_ENTRIES_LOG, entries.size() - fresh.size(), MAX_STALE_BUSINESS_DAYS);
            meterRegistry.counter("currency.provider.failures", "provider", getProviderName()).increment();
        }

        log.debug(SUCCESS_LOG);
        return converter.convertFromFrankfurter(fresh);
    }

    // Shares the "frankfurterClient" breaker with getLatestRates, so scheduler
    // gap-fill failures and historical-endpoint failures aggregate. When open,
    // CallNotPermittedException is raised; the scheduler path swallows it and
    // the /rates/historical path maps it to 503 (see GlobalExceptionHandler).
    @CircuitBreaker(name = "frankfurterClient")
    @Retry(name = "frankfurterClient")
    public CurrencyExchangeResponse getHistoricalRates(final LocalDate date, final Set<String> symbols) {
        log.info(FETCHING_HISTORICAL_LOG, getProviderName(), date);

        final List<FrankfurterRateEntry> entries = executeRequest(OPERATION_HISTORICAL, uriBuilder -> {
            uriBuilder.path(RATES_ENDPOINT)
                    .queryParam(DATE_PARAM, date)
                    .queryParam(BASE_PARAM, BASE_CURRENCY);
            appendQuotes(uriBuilder, symbols);
            return uriBuilder.build();
        });

        validateResponse(entries, OPERATION_HISTORICAL);

        log.debug(SUCCESS_LOG);
        return converter.convertFromFrankfurter(entries);
    }

    /**
     * A response entry is stale when its rate date lies more than
     * {@value #MAX_STALE_BUSINESS_DAYS} business days before {@code today}.
     */
    public static boolean isStale(final LocalDate responseDate, final LocalDate today) {
        int businessDays = 0;
        LocalDate cursor = responseDate.plusDays(1);
        while (!cursor.isAfter(today)) {
            final DayOfWeek day = cursor.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            cursor = cursor.plusDays(1);
        }
        return businessDays > MAX_STALE_BUSINESS_DAYS;
    }

    private List<FrankfurterRateEntry> executeRequest(final String operation,
                                                      final Function<UriBuilder, URI> uriFunction) {
        return frankfurterRestClient.get()
                .uri(uriFunction)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (request, httpResponse) -> {
                            throw new ExternalApiException(operation, getProviderName(),
                                    HTTP_ERROR_PREFIX + httpResponse.getStatusCode());
                        })
                .body(RESPONSE_TYPE);
    }

    private void appendQuotes(final UriBuilder uriBuilder, final Set<String> symbols) {
        if (symbols != null && !symbols.isEmpty()) {
            uriBuilder.queryParam(QUOTES_PARAM, String.join(",", new TreeSet<>(symbols)));
        }
    }

    private void validateResponse(final List<FrankfurterRateEntry> entries, final String operation) {
        // An empty array is a valid "no data" answer (e.g. quotes Frankfurter
        // does not track) — callers map it to 404 / skip. Only null is an error.
        Optional.ofNullable(entries)
                .orElseThrow(() -> new ExternalApiException(operation, getProviderName(), NULL_RESPONSE_ERROR));
    }

    @Override
    public boolean isFallback() {
        return true;
    }

    @Override
    public String getProviderName() {
        return ApiProvider.FRANKFURTER.getDisplayName();
    }
}
