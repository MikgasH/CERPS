package com.example.cerps.common.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * Configuration for Correlation ID propagation in HTTP clients.
 * Provides an interceptor that automatically adds Correlation ID to outgoing requests.
 */
@Configuration
public class CorrelationIdClientConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC = "correlationId";

    /**
     * Creates an interceptor that adds Correlation ID header to outgoing HTTP requests.
     * The Correlation ID is retrieved from MDC (Mapped Diagnostic Context).
     *
     * @return ClientHttpRequestInterceptor that propagates Correlation ID
     */
    @Bean
    public ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            String correlationId = MDC.get(CORRELATION_ID_MDC);
            if (correlationId != null && !correlationId.isBlank()) {
                request.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }
}

