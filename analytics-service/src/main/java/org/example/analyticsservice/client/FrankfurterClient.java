package org.example.analyticsservice.client;

import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerps.common.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only client for the free Frankfurter v2 API (ECB reference rates),
 * used to backfill the analytics historical store with EUR-based daily series.
 *
 * <p>v2 range queries ({@code from}/{@code to}) return the full window in one
 * flat-array response — verified at ~2,200 entries / 165 KB for a 3-year
 * two-quote range with no row cap. If a future much larger call ever
 * misbehaves, chunk the request per calendar year as an escape hatch.
 */
@Component
@Slf4j
public class FrankfurterClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String RATES_ENDPOINT = "/rates";
    private static final String BASE_PARAM = "base";
    private static final String QUOTES_PARAM = "quotes";
    private static final String FROM_PARAM = "from";
    private static final String TO_PARAM = "to";
    private static final String BASE_CURRENCY = "EUR";

    private static final ParameterizedTypeReference<List<FrankfurterRateEntry>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private static final String FETCHING_RANGE_LOG = "Fetching Frankfurter rates for {} from {} to {}";
    private static final String FETCHED_RANGE_LOG = "Fetched {} Frankfurter entries for {} ({} - {})";
    private static final String NULL_RESPONSE_ERROR = "Null response received from Frankfurter";
    private static final String REQUEST_FAILED_ERROR = "Failed to fetch rates from Frankfurter for %s (%s - %s)";

    private final RestClient restClient;

    public FrankfurterClient(
            @Value("${api.frankfurter.url}") final String baseUrl,
            final List<ClientHttpRequestInterceptor> interceptors) {

        final ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        final ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        final RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);
        interceptors.forEach(builder::requestInterceptor);
        this.restClient = builder.build();
    }

    /**
     * Fetches EUR-based daily rates for the given quote currencies over
     * {@code [from, to]} inclusive. An empty list is a valid "no data
     * upstream" answer (e.g. dates before a currency's history starts) —
     * only a null body or transport/HTTP error is an exception.
     */
    // Circuit breaker fails fast once Frankfurter is repeatedly unhealthy: an
    // open breaker raises CallNotPermittedException (not in the retry list, so
    // not retried), which the trends pipeline catches and routes to the legacy
    // currency-service fallback instead of hammering a down upstream.
    @CircuitBreaker(name = "frankfurter")
    @Retry(name = "frankfurter")
    public List<FrankfurterRateEntry> getRates(final Set<String> quotes, final LocalDate from, final LocalDate to) {
        log.info(FETCHING_RANGE_LOG, quotes, from, to);
        try {
            final List<FrankfurterRateEntry> entries = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(RATES_ENDPOINT)
                            .queryParam(BASE_PARAM, BASE_CURRENCY)
                            .queryParam(QUOTES_PARAM, String.join(",", new TreeSet<>(quotes)))
                            .queryParam(FROM_PARAM, from)
                            .queryParam(TO_PARAM, to)
                            .build())
                    .retrieve()
                    .body(RESPONSE_TYPE);

            if (entries == null) {
                throw new ExternalServiceException(NULL_RESPONSE_ERROR);
            }

            log.debug(FETCHED_RANGE_LOG, entries.size(), quotes, from, to);
            return entries;
        } catch (RestClientException e) {
            throw new ExternalServiceException(String.format(REQUEST_FAILED_ERROR, quotes, from, to), e);
        }
    }
}
