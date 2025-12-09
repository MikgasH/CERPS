package com.example.cerpshashkin.filter;

import com.example.cerpshashkin.client.UserServiceClient;
import com.example.cerpshashkin.dto.UserValidationResponse;
import com.example.cerpshashkin.service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("=== FILTER START === URI: {} Method: {}", request.getRequestURI(), request.getMethod());

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        log.info("Authorization header: {}", authHeader != null ? "Present (length: " + authHeader.length() + ")" : "MISSING");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                final UserValidationResponse validation = userServiceClient.validateToken(jwt);

                log.info("=== JWT VALIDATION DEBUG ===");
                log.info("Username from token: {}", username);
                log.info("Validation result: {}", validation);
                if (validation != null) {
                    log.info("Is valid: {}", validation.valid());
                    log.info("Roles from user-service: {}", validation.roles());
                }

                if (validation != null && validation.valid()) {
                    final List<SimpleGrantedAuthority> authorities = validation.roles() != null
                            ? validation.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList()
                            : Collections.emptyList();

                    log.info("Authorities created: {}", authorities);

                    final UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            validation.username(),
                            null,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info("User {} authenticated successfully. Authorities: {}", validation.username(), authToken.getAuthorities());
                } else {
                    log.warn("Token validation failed for user: {}", username);
                }
            }
        } catch (final Exception e) {
            log.error("Cannot set user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}
