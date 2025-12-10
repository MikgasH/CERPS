package com.example.cerps.common.dto;

import java.util.List;

public record UserValidationResponse(
        boolean valid,
        String username,
        List<String> roles
) {

    public static UserValidationResponse valid(final String username, final List<String> roles) {
        return new UserValidationResponse(true, username, roles);
    }

    public static UserValidationResponse invalid() {
        return new UserValidationResponse(false, null, List.of());
    }
}
