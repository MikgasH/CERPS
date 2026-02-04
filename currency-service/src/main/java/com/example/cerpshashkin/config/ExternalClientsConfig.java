package com.example.cerpshashkin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientsConfig {

    private final ClientHttpRequestInterceptor correlationIdInterceptor;

    public ExternalClientsConfig(final ClientHttpRequestInterceptor correlationIdInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
    }

    @Bean("fixerRestClient")
    public RestClient fixerRestClient(
            @Value("${api.fixer.url}") final String fixerUrl) {
        return RestClient.builder()
                .baseUrl(fixerUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("exchangeratesRestClient")
    public RestClient exchangeratesRestClient(
            @Value("${api.exchangerates.url}") final String exchangeratesUrl) {
        return RestClient.builder()
                .baseUrl(exchangeratesUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("currencyapiRestClient")
    public RestClient currencyapiRestClient(
            @Value("${api.currencyapi.url}") final String currencyapiUrl) {
        return RestClient.builder()
                .baseUrl(currencyapiUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }
}
