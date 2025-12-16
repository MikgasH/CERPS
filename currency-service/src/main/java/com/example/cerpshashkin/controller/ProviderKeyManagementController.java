package com.example.cerpshashkin.controller;

import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.service.ProviderKeyManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/provider-keys")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Provider Keys", description = "API key management")
@SecurityRequirement(name = "Bearer Authentication")
public class ProviderKeyManagementController {

    private final ProviderKeyManagementService service;

    @PostMapping
    @Operation(
            summary = "Create new provider API key",
            description = "Creates and encrypts a new API key for an external provider. Only administrators can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Provider key created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "providerName": "fixer",
                                      "active": true,
                                      "createdAt": "2025-12-14T10:15:30Z",
                                      "updatedAt": "2025-12-14T10:15:30Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ProviderKeyResponse> createProviderKey(
            @Valid @RequestBody CreateProviderKeyRequest request) {

        log.info("POST /api/v1/admin/provider-keys - creating key for provider: {}", request.providerName());
        ProviderKeyResponse response = service.createProviderKey(request);
        log.info("Provider key created with id: {}", response.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get all active provider keys",
            description = "Retrieves a list of all active provider API keys. Returns metadata only, not the actual keys."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of active provider keys retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "providerName": "fixer",
                                        "active": true,
                                        "createdAt": "2025-12-14T10:15:30Z",
                                        "updatedAt": "2025-12-14T10:15:30Z"
                                      },
                                      {
                                        "id": 2,
                                        "providerName": "currencyapi",
                                        "active": true,
                                        "createdAt": "2025-12-14T11:20:00Z",
                                        "updatedAt": "2025-12-14T11:20:00Z"
                                      }
                                    ]
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<List<ProviderKeyResponse>> getAllActiveProviderKeys() {
        log.info("GET /api/v1/admin/provider-keys - retrieving all active keys");
        List<ProviderKeyResponse> response = service.getAllActiveProviderKeys();
        log.info("Found {} active provider keys", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get provider key by ID",
            description = "Retrieves provider API key metadata by ID. Returns metadata only, not the actual key."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Provider key found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "providerName": "fixer",
                                      "active": true,
                                      "createdAt": "2025-12-14T10:15:30Z",
                                      "updatedAt": "2025-12-14T10:15:30Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Provider key not found")
    })
    public ResponseEntity<ProviderKeyResponse> getProviderKey(@PathVariable Long id) {
        log.info("GET /api/v1/admin/provider-keys/{} - retrieving provider key", id);
        ProviderKeyResponse response = service.getProviderKey(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update provider API key",
            description = "Updates and re-encrypts the API key for an existing provider."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Provider key updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "providerName": "fixer",
                                      "active": true,
                                      "createdAt": "2025-12-14T10:15:30Z",
                                      "updatedAt": "2025-12-14T12:30:00Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Provider key not found")
    })
    public ResponseEntity<ProviderKeyResponse> updateProviderKey(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderKeyRequest request) {

        log.info("PUT /api/v1/admin/provider-keys/{} - updating provider key", id);
        ProviderKeyResponse response = service.updateProviderKey(id, request);
        log.info("Provider key {} updated successfully", id);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate provider key",
            description = "Soft deletes a provider API key by setting its active status to false."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Provider key deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Provider key not found")
    })
    public ResponseEntity<Void> deleteProviderKey(@PathVariable Long id) {
        log.info("DELETE /api/v1/admin/provider-keys/{} - deactivating provider key", id);
        service.deleteProviderKey(id);
        log.info("Provider key {} deactivated successfully", id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate")
    @Operation(
            summary = "Rotate provider API key",
            description = "Rotates the API key for a provider by replacing it with a new one and re-encrypting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Provider key rotated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "providerName": "fixer",
                                      "active": true,
                                      "createdAt": "2025-12-14T10:15:30Z",
                                      "updatedAt": "2025-12-14T13:45:00Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Provider key not found")
    })
    public ResponseEntity<ProviderKeyResponse> rotateProviderKey(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderKeyRequest request) {

        log.info("POST /api/v1/admin/provider-keys/{}/rotate - rotating provider key", id);
        ProviderKeyResponse response = service.rotateProviderKey(id, request);
        log.info("Provider key {} rotated successfully", id);

        return ResponseEntity.ok(response);
    }
}
