package com.example.cerpshashkin.client.impl;

import com.example.cerpshashkin.client.ApiProvider;
import com.example.cerpshashkin.client.ExchangeRateClient;
import com.example.cerpshashkin.converter.ExternalApiConverter;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.exception.ExternalApiException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class MockService1Client implements ExchangeRateClient {

    private static final String ENDPOINT = "/api/rates/latest";
    private static final String OPERATION_LATEST = "fetch latest exchange rates";
    private static final String HTTP_ERROR_PREFIX = "HTTP error: ";
    private static final String LOG_FETCHING_RATES = "Fetching rates from {}";

    private final RestClient mockService1RestClient;
    private final ExternalApiConverter converter;

    @Override
    public CurrencyExchangeResponse getLatestRates() {
        log.info(LOG_FETCHING_RATES, getProviderName());

        final FixerioResponse response = mockService1RestClient.get()
                .uri(ENDPOINT)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (request, httpResponse) -> {
                            throw new ExternalApiException(OPERATION_LATEST, getProviderName(),
                                    HTTP_ERROR_PREFIX + httpResponse.getStatusCode());
                        })
                .body(FixerioResponse.class);

        return converter.convertFromFixer(response);
    }

    @Override
    public String getProviderName() {
        return ApiProvider.MOCK_SERVICE_1.getDisplayName();
    }
}
