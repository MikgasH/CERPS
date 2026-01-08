package com.example.cerpshashkin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProviderKeyRequest(

        @NotBlank(message = "API key is required")
        String apiKey
) {
}
