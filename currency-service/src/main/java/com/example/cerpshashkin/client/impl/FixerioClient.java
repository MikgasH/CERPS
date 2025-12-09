package com.example.cerpshashkin.client.impl;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.converter.ExternalApiConverter;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FixerioClient implements ExchangeRateClient {

    private static final String ENDPOINT = "/latest";
    private static final String ACCESS_KEY_PARAM = "access_key";

    private static final String OPERATION_LATEST = "fetch latest exchange rates";

    private static final String FETCHING_LATEST_LOG = "Fetching latest rates from {}";
    private static final String REQUEST_LOG = "Making request to Fixer.io: {}";
    private static final String SUCCESS_LOG = "Successfully received response from Fixer.io";

    private static final String NULL_RESPONSE_ERROR = "Null response received";
    private static final String SUCCESS_FALSE_ERROR = "API returned success=false";
    private static final String EMPTY_RATES_ERROR = "Empty rates received";
    private static final String HTTP_ERROR_PREFIX = "HTTP error: ";

    @Value("${api.fixer.access-key}")
    private String fixerAccessKey;

    private final RestClient fixerRestClient;
    private final ExternalApiConverter converter;

    @Override
    public CurrencyExchangeResponse getLatestRates() {
        log.info(FETCHING_LATEST_LOG, getProviderName());
        log.debug(REQUEST_LOG, ENDPOINT);

        final FixerioResponse response = fixerRestClient.get()
                .uri(uriBuilder -> uriBuilder.path(ENDPOINT)
                        .queryParam(ACCESS_KEY_PARAM, fixerAccessKey)
                        .build())
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (request, httpResponse) -> {
                            throw new ExternalApiException(OPERATION_LATEST, getProviderName(),
                                    HTTP_ERROR_PREFIX + httpResponse.getStatusCode());
                        })
                .body(FixerioResponse.class);

        validateResponse(response);

        log.debug(SUCCESS_LOG);
        return converter.convertFromFixer(response);
    }

    private void validateResponse(final FixerioResponse response) {
        final FixerioResponse validResponse = Optional.ofNullable(response)
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), NULL_RESPONSE_ERROR));

        Optional.of(validResponse)
                .filter(FixerioResponse::success)
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), SUCCESS_FALSE_ERROR));

        Optional.ofNullable(validResponse.rates())
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), EMPTY_RATES_ERROR));
    }

    @Override
    public String getProviderName() {
        return ApiProvider.FIXER.getDisplayName();
    }
}
