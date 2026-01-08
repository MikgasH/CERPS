package com.example.cerpshashkin.config;

import com.example.cerps.common.config.BaseOpenApiConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

    @Override
    protected String getTitle() {
        return "Currency Exchange Service API";
    }

    @Override
    protected String getDescription() {
        return "REST API for currency exchange rate management and conversion";
    }

    @Override
    protected String getVersion() {
        return "1.0.0";
    }
}
