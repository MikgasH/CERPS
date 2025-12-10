package org.example.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import org.example.analyticsservice.filter.JwtAuthenticationFilter;
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

    private static final String ENDPOINT_CURRENCIES = "/api/v1/analytics/currencies";
    private static final String ENDPOINT_EXCHANGE_RATES = "/api/v1/analytics/exchange-rates";
    private static final String ENDPOINT_REFRESH = "/api/v1/analytics/refresh";
    private static final String ENDPOINT_TRENDS = "/api/v1/analytics/trends";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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

                        .requestMatchers(HttpMethod.GET, ENDPOINT_EXCHANGE_RATES)
                        .hasAnyRole(ROLE_USER, ROLE_PREMIUM_USER, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, ENDPOINT_REFRESH)
                        .hasRole(ROLE_ADMIN)

                        .requestMatchers(HttpMethod.GET, ENDPOINT_TRENDS)
                        .hasAnyRole(ROLE_PREMIUM_USER, ROLE_ADMIN)

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
