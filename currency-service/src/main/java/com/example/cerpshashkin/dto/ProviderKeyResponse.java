package com.example.cerpshashkin.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ProviderKeyResponse(
        Long id,
        String providerName,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
