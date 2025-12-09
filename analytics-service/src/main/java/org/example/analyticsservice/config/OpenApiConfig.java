package org.example.analyticsservice.config;

import org.springframework.context.annotation.Configuration;

import com.example.cerps.common.config.BaseOpenApiConfig;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

    @Override
    protected String getTitle() {
        return "Analytics Service API";
    }

    @Override
    protected String getDescription() {
        return "REST API for currency trends analytics";
    }

    @Override
    protected String getVersion() {
        return "1.0.0";
    }
}
