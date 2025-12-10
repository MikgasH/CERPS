package com.example.cerps.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TrendsRequest(
        @NotBlank(message = "From currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "From currency must be a 3-letter code")
        String from,

        @NotBlank(message = "To currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "To currency must be a 3-letter code")
        String to,

        @NotBlank(message = "Period is required")
        @Pattern(regexp = "^\\d+[HDMY]$", message = "Period must be in format: 1H, 7D, 1M, 1Y")
        String period
) {
}
