package org.example.userservice.config;

import org.springframework.context.annotation.Configuration;
import com.example.cerps.common.config.BaseOpenApiConfig;


@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

    @Override
    protected String getTitle() {
        return "User Management Service API";
    }

    @Override
    protected String getDescription() {
        return "REST API for user authentication and authorization";
    }

    @Override
    protected String getVersion() {
        return "1.0.0";
    }
}
