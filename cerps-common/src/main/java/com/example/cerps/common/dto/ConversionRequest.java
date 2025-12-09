package com.example.cerps.common.dto;

import com.example.cerps.common.validation.ValidCurrency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ConversionRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @ValidCurrency(message = "Invalid source currency code")
        String from,

        @ValidCurrency(message = "Invalid target currency code")
        String to
) {}
