package com.example.cerpshashkin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientsConfig {

    @Bean("fixerRestClient")
    public RestClient fixerRestClient(
            @Value("${api.fixer.url}") final String fixerUrl) {
        return RestClient.builder()
                .baseUrl(fixerUrl)
                .build();
    }

    @Bean("exchangeratesRestClient")
    public RestClient exchangeratesRestClient(
            @Value("${api.exchangerates.url}") final String exchangeratesUrl) {
        return RestClient.builder()
                .baseUrl(exchangeratesUrl)
                .build();
    }

    @Bean("currencyapiRestClient")
    public RestClient currencyapiRestClient(
            @Value("${api.currencyapi.url}") final String currencyapiUrl) {
        return RestClient.builder()
                .baseUrl(currencyapiUrl)
                .build();
    }

    @Bean("mockService1RestClient")
    public RestClient mockService1RestClient(
            @Value("${api.mock1.url}") final String mockService1Url) {
        return RestClient.builder()
                .baseUrl(mockService1Url)
                .build();
    }

    @Bean("mockService2RestClient")
    public RestClient mockService2RestClient(
            @Value("${api.mock2.url}") final String mockService2Url) {
        return RestClient.builder()
                .baseUrl(mockService2Url)
                .build();
    }
}
