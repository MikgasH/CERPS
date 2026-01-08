package com.example.cerpshashkin.config;

import com.example.cerpshashkin.filter.GatewayHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_PREMIUM_USER = "PREMIUM_USER";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String ENDPOINT_CURRENCIES = "/api/v1/currencies";
    private static final String ENDPOINT_CURRENCIES_REFRESH = "/api/v1/currencies/refresh";
    private static final String ENDPOINT_CURRENCIES_CONVERT = "/api/v1/currencies/convert";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**"
    };

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        .requestMatchers(HttpMethod.GET, ENDPOINT_CURRENCIES)
                        .hasAnyRole(ROLE_USER, ROLE_PREMIUM_USER, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, ENDPOINT_CURRENCIES)
                        .hasRole(ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, ENDPOINT_CURRENCIES_REFRESH)
                        .hasRole(ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, ENDPOINT_CURRENCIES_CONVERT)
                        .hasAnyRole(ROLE_USER, ROLE_PREMIUM_USER, ROLE_ADMIN)

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
