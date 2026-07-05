package org.example.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import org.example.analyticsservice.filter.AdminEndpointRateLimitFilter;
import org.example.analyticsservice.filter.TrendsEndpointRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TrendsEndpointRateLimitFilter publicRateLimitFilter;
    private final AdminEndpointRateLimitFilter adminRateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Health stays public for the Railway healthcheck; the
                        // information-bearing actuator endpoints (prometheus,
                        // metrics) are not world-readable. The trends API and
                        // everything else remain public.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(publicRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<AdminEndpointRateLimitFilter> disableAdminRateLimitFilterAutoRegistration() {
        final FilterRegistrationBean<AdminEndpointRateLimitFilter> registration =
                new FilterRegistrationBean<>(adminRateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }
}
