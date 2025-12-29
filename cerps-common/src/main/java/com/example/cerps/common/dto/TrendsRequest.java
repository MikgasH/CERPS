package com.example.cerps.common.dto;

import com.example.cerps.common.validation.ValidCurrency;
import com.example.cerps.common.validation.ValidPeriod;
import jakarta.validation.constraints.NotBlank;

public record TrendsRequest(

        @NotBlank(message = "From currency is required")
        @ValidCurrency(message = "Invalid source currency code")
        String from,

        @NotBlank(message = "To currency is required")
        @ValidCurrency(message = "Invalid target currency code")
        String to,

        @NotBlank(message = "Period is required")
        @ValidPeriod
        String period
) {
}
