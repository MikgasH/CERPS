package com.example.cerpshashkin.service.security;

import com.example.cerps.common.security.JwtAuthenticationFilter;
import com.example.cerps.common.security.JwtTokenParser;

import org.springframework.stereotype.Component;

@Component
public class JwtAuthFilter extends JwtAuthenticationFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(final JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected JwtTokenParser getJwtTokenParser() {
        return jwtService;
    }
}
