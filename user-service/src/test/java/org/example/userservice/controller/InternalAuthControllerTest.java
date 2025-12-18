package org.example.userservice.controller;

import org.example.userservice.service.CustomUserDetailsService;
import org.example.userservice.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalAuthController.class)
class InternalAuthControllerTest {

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void should_ReturnValidResponse_When_TokenIsValid() throws Exception {
        String token = "valid_jwt_token";
        String email = "user@example.com";

        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(jwtService, times(1)).extractUsername(token);
        verify(userDetailsService, times(1)).loadUserByUsername(email);
        verify(jwtService, times(1)).isTokenValid(token, userDetails);
    }

    @Test
    void should_ReturnInvalidResponse_When_TokenIsInvalid() throws Exception {
        String token = "invalid_jwt_token";
        String email = "user@example.com";

        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void should_ReturnInvalidResponse_When_AuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/internal/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void should_ReturnInvalidResponse_When_AuthorizationHeaderInvalid() throws Exception {
        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void should_ReturnInvalidResponse_When_UserNotFound() throws Exception {
        String token = "valid_jwt_token";
        String email = "nonexistent@example.com";

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void should_ReturnInvalidResponse_When_TokenExtractionFails() throws Exception {
        String token = "malformed_token";

        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Token extraction failed"));

        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void should_ReturnValidResponseWithMultipleRoles_When_TokenIsValid() throws Exception {
        String token = "valid_jwt_token";
        String email = "admin@example.com";

        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                )
                .build();

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        mockMvc.perform(post("/api/internal/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.roles[1]").value("ROLE_ADMIN"));
    }
}
