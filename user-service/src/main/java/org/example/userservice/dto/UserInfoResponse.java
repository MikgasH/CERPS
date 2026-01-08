package org.example.userservice.dto;

import java.time.Instant;
import java.util.List;

public record UserInfoResponse(
        Long id,
        String email,
        List<String> roles,
        boolean enabled,
        Instant createdAt
) {
    public static UserInfoResponse from(
            final Long id,
            final String email,
            final List<String> roles,
            final boolean enabled,
            final Instant createdAt
    ) {
        return new UserInfoResponse(id, email, roles, enabled, createdAt);
    }
}
