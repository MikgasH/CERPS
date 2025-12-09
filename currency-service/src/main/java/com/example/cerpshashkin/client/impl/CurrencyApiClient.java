package com.example.cerpshashkin.client.impl;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.converter.CurrencyApiConverter;
import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
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
public class CurrencyApiClient implements ExchangeRateClient {

    private static final String ENDPOINT = "/latest";
    private static final String API_KEY_PARAM = "apikey";

    private static final String OPERATION_LATEST = "fetch latest exchange rates";

    private static final String FETCHING_LATEST_LOG = "Fetching latest rates from {}";
    private static final String REQUEST_LOG = "Making request to CurrencyAPI: {}";
    private static final String SUCCESS_LOG = "Successfully received response from CurrencyAPI";

    private static final String NULL_RESPONSE_ERROR = "Null response received";
    private static final String NULL_DATA_ERROR = "Response data is null";
    private static final String EMPTY_DATA_ERROR = "Empty data received";
    private static final String INVALID_META_ERROR = "Invalid meta information";
    private static final String HTTP_ERROR_PREFIX = "HTTP error: ";

    @Value("${api.currencyapi.access-key}")
    private String currencyapiAccessKey;

    private final RestClient currencyapiRestClient;
    private final CurrencyApiConverter converter;

    @Override
    public CurrencyExchangeResponse getLatestRates() {
        log.info(FETCHING_LATEST_LOG, getProviderName());
        log.debug(REQUEST_LOG, ENDPOINT);

        final CurrencyApiRawResponse response = currencyapiRestClient.get()
                .uri(uriBuilder -> uriBuilder.path(ENDPOINT)
                        .queryParam(API_KEY_PARAM, currencyapiAccessKey)
                        .build())
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (request, httpResponse) -> {
                            throw new ExternalApiException(OPERATION_LATEST, getProviderName(),
                                    HTTP_ERROR_PREFIX + httpResponse.getStatusCode());
                        })
                .body(CurrencyApiRawResponse.class);

        validateResponse(response);

        log.debug(SUCCESS_LOG);
        return converter.convertToCurrencyExchange(response);
    }

    private void validateResponse(final CurrencyApiRawResponse response) {
        final CurrencyApiRawResponse validResponse = Optional.ofNullable(response)
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), NULL_RESPONSE_ERROR));

        Optional.ofNullable(validResponse.data())
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), NULL_DATA_ERROR));

        Optional.of(validResponse.data())
                .filter(data -> !data.isEmpty())
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), EMPTY_DATA_ERROR));

        Optional.ofNullable(validResponse.meta())
                .orElseThrow(() -> new ExternalApiException(OPERATION_LATEST, getProviderName(), INVALID_META_ERROR));
    }

    @Override
    public String getProviderName() {
        return ApiProvider.CURRENCY_API.getDisplayName();
    }
}
