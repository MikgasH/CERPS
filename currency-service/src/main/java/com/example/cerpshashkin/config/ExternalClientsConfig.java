package com.example.cerpshashkin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ExternalClientsConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final ClientHttpRequestInterceptor correlationIdInterceptor;

    public ExternalClientsConfig(final ClientHttpRequestInterceptor correlationIdInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
    }

    @Bean("fixerRestClient")
    public RestClient fixerRestClient(
            @Value("${api.fixer.url}") final String fixerUrl) {
        return RestClient.builder()
                .baseUrl(fixerUrl)
                .requestFactory(createRequestFactory())
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("exchangeratesRestClient")
    public RestClient exchangeratesRestClient(
            @Value("${api.exchangerates.url}") final String exchangeratesUrl) {
        return RestClient.builder()
                .baseUrl(exchangeratesUrl)
                .requestFactory(createRequestFactory())
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("currencyapiRestClient")
    public RestClient currencyapiRestClient(
            @Value("${api.currencyapi.url}") final String currencyapiUrl) {
        return RestClient.builder()
                .baseUrl(currencyapiUrl)
                .requestFactory(createRequestFactory())
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
