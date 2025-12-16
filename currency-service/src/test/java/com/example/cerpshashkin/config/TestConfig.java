package com.example.cerpshashkin.config;

import com.example.cerpshashkin.service.EncryptionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            // Simple test implementation without MDC dependency
            request.getHeaders().add("X-Correlation-ID", "test-correlation-id");
            return execution.execute(request, body);
        };
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public EncryptionService encryptionService(
            @org.springframework.beans.factory.annotation.Value("${api.fixer.access-key:}") String fixerKey,
            @org.springframework.beans.factory.annotation.Value("${api.exchangerates.access-key:}") String exchangeratesKey,
            @org.springframework.beans.factory.annotation.Value("${api.currencyapi.access-key:}") String currencyapiKey) {

        return new EncryptionService("xtCPEriABFzNjq7KmLK5BmGt8vbWPq0PcB1C7Y8DxNo=") {
            @Override
            public String decrypt(String encryptedData) {
                if (encryptedData.contains("fixer")) return fixerKey;
                if (encryptedData.contains("exchangerates")) return exchangeratesKey;
                if (encryptedData.contains("currencyapi")) return currencyapiKey;
                return "test-default-key";
            }

            @Override
            public String encrypt(String data) {
                return java.util.Base64.getEncoder().encodeToString(data.getBytes());
            }
        };
    }
}
