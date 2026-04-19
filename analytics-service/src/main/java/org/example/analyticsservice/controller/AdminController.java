package org.example.analyticsservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Analytics admin operations")
public class AdminController {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final AnalyticsService analyticsService;

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @PostMapping("/cache/refresh")
    @Operation(summary = "Invalidate analytics trends cache")
    @ApiResponse(responseCode = "200", description = "Cache invalidated")
    @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    public ResponseEntity<Map<String, String>> refreshCache(
            @RequestHeader(value = API_KEY_HEADER, required = false) final String apiKey) {
        if (!isAuthorized(apiKey)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        analyticsService.invalidateCache();
        return ResponseEntity.ok(Map.of("status", "cache invalidated"));
    }

    private boolean isAuthorized(final String provided) {
        if (provided == null || provided.isBlank() || adminApiKey == null || adminApiKey.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                adminApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
