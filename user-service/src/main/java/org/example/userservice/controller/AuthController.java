package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.ChangePasswordRequest;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserInfoResponse;
import org.example.userservice.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private static final String LOG_REGISTER_REQUEST = "POST /api/v1/auth/register - registering user: {}";
    private static final String LOG_REGISTER_SUCCESS = "User registered successfully: {}";
    private static final String LOG_LOGIN_REQUEST = "POST /api/v1/auth/login - login attempt for user: {}";
    private static final String LOG_LOGIN_SUCCESS = "User logged in successfully: {}";
    private static final String LOG_GET_CURRENT_USER = "GET /api/v1/auth/me - getting current user info";
    private static final String LOG_CHANGE_PASSWORD_REQUEST = "POST /api/v1/auth/change-password - user: {}";

    private static final String MESSAGE_PASSWORD_CHANGED = "Password changed successfully";
    private static final String MESSAGE_REGISTER_SUCCESS = "User registered successfully";

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with ROLE_USER by default. "
                    + "Password must contain at least 8 characters, one digit, one uppercase letter, "
                    + "one lowercase letter, and one special character."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<String> register(@Valid @RequestBody final RegisterRequest request) {
        log.info(LOG_REGISTER_REQUEST, request.email());

        authenticationService.register(request);

        log.info(LOG_REGISTER_SUCCESS, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(MESSAGE_REGISTER_SUCCESS);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates user and returns JWT access token. "
                    + "Token must be included in subsequent requests as: Authorization: Bearer {token}"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account disabled")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
        log.info(LOG_LOGIN_REQUEST, request.email());

        final LoginResponse response = authenticationService.login(request);

        log.info(LOG_LOGIN_SUCCESS, request.email());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user information",
            description = "Returns information about the currently authenticated user including id, email, roles, and account status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserInfoResponse> getCurrentUser(final Authentication authentication) {
        log.info(LOG_GET_CURRENT_USER);

        if (authentication == null) {
            throw new BadCredentialsException("Authentication object is missing.");
        }

        final UserInfoResponse response = authenticationService.getCurrentUserInfo(authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change password",
            description = "Changes the password for the currently authenticated user. "
                    + "Requires current password for verification. New password must meet security requirements."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or new password same as current"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid current password or token")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody final ChangePasswordRequest request,
            final Authentication authentication) {

        if (authentication == null) {
            throw new BadCredentialsException("Authentication object is missing.");
        }

        log.info(LOG_CHANGE_PASSWORD_REQUEST, authentication.getName());

        authenticationService.changePassword(authentication.getName(), request);

        return ResponseEntity.ok(MESSAGE_PASSWORD_CHANGED);
    }
}
