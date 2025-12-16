package com.example.cerpshashkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProviderKeyRequest(

        @NotBlank(message = "Provider name is required")
        @Size(min = 1, max = 50, message = "Provider name must be between 1 and 50 characters")
        String providerName,

        @NotBlank(message = "API key is required")
        String apiKey
) {
}
