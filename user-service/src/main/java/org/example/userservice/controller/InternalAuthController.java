package org.example.userservice.controller;

import com.example.cerps.common.dto.UserValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.service.CustomUserDetailsService;
import org.example.userservice.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/auth")
@RequiredArgsConstructor
@Slf4j
public class InternalAuthController {

    private static final String LOG_VALIDATE_TOKEN = "Validating token for inter-service communication";
    private static final String LOG_TOKEN_VALID = "Token validated successfully for user: {}";
    private static final String LOG_TOKEN_INVALID = "Token validation failed";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/validate")
    public ResponseEntity<UserValidationResponse> validateToken(
            @RequestHeader("Authorization") final String authHeader) {

        log.debug(LOG_VALIDATE_TOKEN);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn(LOG_TOKEN_INVALID);
            return ResponseEntity.ok(UserValidationResponse.invalid());
        }

        try {
            final String jwt = authHeader.substring(BEARER_PREFIX_LENGTH);
            final String username = jwtService.extractUsername(jwt);
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                final List<String> roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

                log.debug(LOG_TOKEN_VALID, username);
                return ResponseEntity.ok(UserValidationResponse.valid(username, roles));
            }

            log.warn(LOG_TOKEN_INVALID);
            return ResponseEntity.ok(UserValidationResponse.invalid());

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.ok(UserValidationResponse.invalid());
        }
    }
}
