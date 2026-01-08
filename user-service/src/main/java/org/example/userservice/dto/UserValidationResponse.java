package org.example.userservice.dto;

import java.util.List;

public record UserValidationResponse(
        boolean valid,
        String username,
        List<String> roles
) {}
