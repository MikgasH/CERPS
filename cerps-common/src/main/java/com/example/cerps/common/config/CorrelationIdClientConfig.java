package com.example.cerps.common.config;

import com.example.cerps.common.CerpsConstants;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration
public class CorrelationIdClientConfig {

    @Bean
    public ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            String correlationId = MDC.get(CerpsConstants.CORRELATION_ID_MDC);
            if (correlationId != null && !correlationId.isBlank()) {
                request.getHeaders().add(CerpsConstants.CORRELATION_ID_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }
}

