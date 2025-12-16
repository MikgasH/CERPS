/*package org.example.userservice.service;

import org.example.userservice.dto.ChangePasswordRequest;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.LoginResponse;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UserInfoResponse;
import org.example.userservice.entity.RoleEntity;
import org.example.userservice.entity.UserEntity;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.repository.RoleRepository;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_WithNewUser_ShouldCreateUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");

        authenticationService.register(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded_password");
        assertThat(savedUser.getEnabled()).isTrue();
        assertThat(savedUser.getRoles()).hasSize(1);
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email 'existing@example.com' is already registered");

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findByName(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_WhenRoleNotFound_ShouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Default role not found: ROLE_USER");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .build();

        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("encoded_password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token_here");

        LoginResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt_token_here");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).hasSize(1);
        assertThat(response.roles()).contains("ROLE_USER");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(userDetails);
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword123!")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_WithMultipleRoles_ShouldReturnAllRoles() {
        LoginRequest request = LoginRequest.builder()
                .email("admin@example.com")
                .password("Password123!")
                .build();

        UserDetails userDetails = User.builder()
                .username("admin@example.com")
                .password("encoded_password")
                .authorities(
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                )
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token_here");

        LoginResponse response = authenticationService.login(request);

        assertThat(response.roles()).hasSize(2);
        assertThat(response.roles()).contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void login_WithDisabledUser_ShouldThrowException() {
        LoginRequest request = LoginRequest.builder()
                .email("disabled@example.com")
                .password("Password123!")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User account is disabled"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void getCurrentUserInfo_WithValidEmail_ShouldReturnUserInfo() {
        String email = "user@example.com";

        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserInfoResponse response = authenticationService.getCurrentUserInfo(email);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.roles()).hasSize(1);
        assertThat(response.roles()).contains("ROLE_USER");
        assertThat(response.enabled()).isTrue();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void getCurrentUserInfo_WithNonExistentUser_ShouldThrowException() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.getCurrentUserInfo(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent@example.com");
    }

    @Test
    void getCurrentUserInfo_WithMultipleRoles_ShouldReturnAllRoles() {
        String email = "admin@example.com";

        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        RoleEntity roleAdmin = RoleEntity.builder()
                .id(3L)
                .name("ROLE_ADMIN")
                .build();

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);
        roles.add(roleAdmin);

        UserEntity user = UserEntity.builder()
                .id(102L)
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserInfoResponse response = authenticationService.getCurrentUserInfo(email);

        assertThat(response.roles()).hasSize(2);
        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void changePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
        String email = "user@example.com";
        String currentPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email(email)
                .password("encoded_old_password")
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");

        authenticationService.changePassword(email, request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encoded_new_password");
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldThrowException() {
        String email = "user@example.com";
        String currentPassword = "WrongPassword123!";
        String newPassword = "NewPassword123!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email(email)
                .password("encoded_old_password")
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.changePassword(email, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WithSamePassword_ShouldThrowException() {
        String email = "user@example.com";
        String currentPassword = "Password123!";
        String newPassword = "Password123!";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.changePassword(email, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New password must be different from the current password");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WithNonExistentUser_ShouldThrowException() {
        String email = "nonexistent@example.com";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.changePassword(email, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent@example.com");

        verify(userRepository, never()).save(any());
    }
}*/

