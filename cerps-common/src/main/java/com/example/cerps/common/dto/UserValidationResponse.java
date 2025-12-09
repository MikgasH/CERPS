package com.example.cerps.common.dto;

import java.util.Set;

public record UserValidationResponse(
        boolean valid,
        String username,
        Set<String> roles,
        String message
) {

    public static UserValidationResponse valid(String username, Set<String> roles) {
        return new UserValidationResponse(true, username, roles, "Valid token");
    }

    public static UserValidationResponse invalid(String message) {
        return new UserValidationResponse(false, null, Set.of(), message);
    }
}
