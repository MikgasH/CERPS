package com.example.cerpshashkin.controller;

import com.example.cerps.common.validation.ValidCurrency;
import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin", description = "Admin operations (requires X-API-Key header)")
@SecurityRequirement(name = "ApiKey")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/currencies")
    @Operation(summary = "Add new supported currency")
    public ResponseEntity<String> addCurrency(
            @RequestParam @NotBlank @ValidCurrency final String currency) {
        log.info("POST /api/v1/admin/currencies - {}", currency);
        adminService.addCurrency(currency);
        return ResponseEntity.ok(String.format("Currency %s added", currency.toUpperCase()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh exchange rates from providers")
    public ResponseEntity<String> refreshRates() {
        log.info("POST /api/v1/admin/refresh");
        adminService.refreshExchangeRates();
        return ResponseEntity.ok("Rates updated");
    }

    @PostMapping("/provider-keys")
    @Operation(summary = "Create provider API key")
    public ResponseEntity<ProviderKeyResponse> createProviderKey(
            @Valid @RequestBody final CreateProviderKeyRequest request) {
        log.info("POST /api/v1/admin/provider-keys - {}", request.providerName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createProviderKey(request));
    }

    @GetMapping("/provider-keys")
    @Operation(summary = "Get all active provider keys")
    public ResponseEntity<List<ProviderKeyResponse>> getAllProviderKeys() {
        return ResponseEntity.ok(adminService.getAllProviderKeys());
    }

    @GetMapping("/provider-keys/{id}")
    @Operation(summary = "Get provider key by ID")
    public ResponseEntity<ProviderKeyResponse> getProviderKey(@PathVariable final Long id) {
        return ResponseEntity.ok(adminService.getProviderKey(id));
    }

    @PutMapping("/provider-keys/{id}")
    @Operation(summary = "Update provider API key")
    public ResponseEntity<ProviderKeyResponse> updateProviderKey(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateProviderKeyRequest request) {
        log.info("PUT /api/v1/admin/provider-keys/{}", id);
        return ResponseEntity.ok(adminService.updateProviderKey(id, request));
    }

    @DeleteMapping("/provider-keys/{id}")
    @Operation(summary = "Deactivate provider key")
    public ResponseEntity<Void> deleteProviderKey(@PathVariable final Long id) {
        log.info("DELETE /api/v1/admin/provider-keys/{}", id);
        adminService.deleteProviderKey(id);
        return ResponseEntity.noContent().build();
    }
}
