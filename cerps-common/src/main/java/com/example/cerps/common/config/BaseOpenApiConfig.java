package com.example.cerps.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.List;

public abstract class BaseOpenApiConfig {

    @Value("${springdoc.server-url:}")
    private String serverUrl;

    protected abstract String getTitle();

    protected abstract String getDescription();

    protected abstract String getVersion();

    @Bean
    public OpenAPI customOpenAPI() {
        final String apiKeySchemeName = "X-API-Key";

        final OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(getTitle())
                        .description(getDescription())
                        .version(getVersion()))
                .components(new Components()
                        .addSecuritySchemes(apiKeySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Enter API Key for admin endpoints")));

        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.servers(List.of(new Server().url(serverUrl).description("API Gateway")));
        }

        return openAPI;
    }
}
