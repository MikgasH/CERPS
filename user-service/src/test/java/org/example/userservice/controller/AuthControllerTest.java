package org.example.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.userservice.config.SecurityConfig;
import org.example.userservice.dto.ChangePasswordRequest;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserInfoResponse;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.service.AuthenticationService;
import org.example.userservice.service.CustomUserDetailsService;
import org.example.userservice.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void should_RegisterUser_When_ValidRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        doNothing().when(authenticationService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void should_ReturnConflict_When_RegisteringExistingEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .build();

        doThrow(new UserAlreadyExistsException("Email 'existing@example.com' is already registered"))
                .when(authenticationService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void should_ReturnBadRequest_When_RegisteringWithInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void should_ReturnToken_When_LoginWithValidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .build();

        LoginResponse response = new LoginResponse(
                "jwt_token_here",
                "Bearer",
                "user@example.com",
                List.of("ROLE_USER")
        );

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token_here"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void should_ReturnUnauthorized_When_LoginWithInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword!")
                .build();

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void should_ReturnUserInfo_When_AuthenticatedUserCallsMe() throws Exception {
        UserInfoResponse response = new UserInfoResponse(
                1L,
                "user@example.com",
                List.of("ROLE_USER"),
                true,
                Instant.now()
        );

        when(authenticationService.getCurrentUserInfo("user@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(authenticationService, times(1)).getCurrentUserInfo("user@example.com");
    }

    @Test
    void should_ReturnForbidden_When_UnauthenticatedUserCallsMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).getCurrentUserInfo(anyString());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void should_ChangePassword_When_ValidRequest() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .build();

        doNothing().when(authenticationService).changePassword(anyString(), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        verify(authenticationService, times(1)).changePassword(anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void should_ReturnBadRequest_When_ChangingWithInvalidCurrentPassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword!")
                .newPassword("NewPassword123!")
                .build();

        doThrow(new BadCredentialsException("Current password is incorrect"))
                .when(authenticationService).changePassword(anyString(), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void should_ReturnBadRequest_When_ChangingToSamePassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Password123!")
                .newPassword("Password123!")
                .build();

        doThrow(new IllegalArgumentException("New password must be different from the current password"))
                .when(authenticationService).changePassword(anyString(), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_ReturnForbidden_When_UnauthenticatedUserTriesToChangePassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).changePassword(anyString(), any());
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com")
    void should_ReturnUnauthorized_When_UserNotFound() throws Exception {
        when(authenticationService.getCurrentUserInfo("nonexistent@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
