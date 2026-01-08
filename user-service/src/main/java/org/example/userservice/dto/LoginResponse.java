package org.example.userservice.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String type,
        String email,
        List<String> roles
) {
    public static LoginResponse success(
            final String token,
            final String email,
            final List<String> roles
    ) {
        return new LoginResponse(
                token,
                "Bearer",
                email,
                roles
        );
    }
}
