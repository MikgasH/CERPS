package org.example.analyticsservice.client;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class CurrencyServiceClient {

    private final RestClient restClient;

    public CurrencyServiceClient(
            @Value("${currency-service.url}") final String baseUrl,
            final List<ClientHttpRequestInterceptor> interceptors) {

        final RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        interceptors.forEach(builder::requestInterceptor);
        this.restClient = builder.build();
    }

    public List<String> getSupportedCurrencies() {
        try {
            final List<String> currencies = restClient.get()
                    .uri("/api/v1/currencies")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });

            log.debug("Fetched {} supported currencies from currency-service", currencies != null ? currencies.size() : 0);
            return currencies != null ? currencies : List.of();
        } catch (RestClientException e) {
            throw new ExternalServiceException("Failed to fetch supported currencies from currency-service", e);
        }
    }

    public RateHistoryResponse getRateHistory(
            final String from, final String to,
            final Instant startDate, final Instant endDate) {
        try {
            final RateHistoryResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/rates/history")
                                .queryParam("from", from)
                                .queryParam("to", to);
                        if (startDate != null) {
                            uriBuilder.queryParam("startDate", startDate.toString());
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("endDate", endDate.toString());
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(RateHistoryResponse.class);

            log.debug("Fetched rate history for {} -> {}: {} points",
                    from, to, response != null ? response.points().size() : 0);
            return response;
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    String.format("Failed to fetch rate history for %s -> %s from currency-service", from, to), e);
        }
    }
}
