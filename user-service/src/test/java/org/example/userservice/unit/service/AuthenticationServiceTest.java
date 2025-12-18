package org.example.userservice.unit.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.example.userservice.service.AuthenticationService;
import org.example.userservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
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

    private SimpleMeterRegistry meterRegistry;
    private AuthenticationService authenticationService;

    private RoleEntity userRole;
    private UserEntity testUser;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String TEST_TOKEN = "jwt.token.here";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        authenticationService = new AuthenticationService(
                userRepository, roleRepository, passwordEncoder, jwtService,
                authenticationManager, meterRegistry
        );

        authenticationService.initMetrics();

        userRole = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        testUser = UserEntity.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("Should initialize metrics on post construct")
    void initMetrics_ShouldRegisterAllCounters() {
        SimpleMeterRegistry testRegistry = new SimpleMeterRegistry();

        AuthenticationService service = new AuthenticationService(
                userRepository, roleRepository, passwordEncoder, jwtService,
                authenticationManager, testRegistry
        );
        service.initMetrics();

        assertThat(service).isNotNull();
        assertThat(testRegistry.find("user.registrations.success").counter()).isNotNull();
        assertThat(testRegistry.find("user.registrations.failure").counter()).isNotNull();
        assertThat(testRegistry.find("user.logins.success").counter()).isNotNull();
        assertThat(testRegistry.find("user.logins.failure").counter()).isNotNull();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void register_WithValidRequest_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        authenticationService.register(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getEnabled()).isTrue();
        assertThat(savedUser.getRoles()).containsExactly(userRole);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_WithExistingEmail_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email 'test@example.com' is already registered");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void register_WhenRoleNotFound_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Default role not found: ROLE_USER");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should encode password when registering")
    void register_ShouldEncodePassword() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        authenticationService.register(request);

        verify(passwordEncoder).encode(TEST_PASSWORD);
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WithValidCredentials_ShouldReturnToken() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        UserDetails userDetails = User.builder()
                .username(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, TEST_PASSWORD, userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn(TEST_TOKEN);

        LoginResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(TEST_TOKEN);
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
        assertThat(response.roles()).containsExactly("ROLE_USER");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void login_WithInvalidCredentials_ShouldThrowException() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should include all roles in login response")
    void login_WithMultipleRoles_ShouldReturnAllRoles() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        UserDetails userDetails = User.builder()
                .username(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, TEST_PASSWORD, userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn(TEST_TOKEN);

        LoginResponse response = authenticationService.login(request);

        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should get current user info successfully")
    void getCurrentUserInfo_WithExistingUser_ShouldReturnInfo() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        UserInfoResponse response = authenticationService.getCurrentUserInfo(TEST_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
        assertThat(response.roles()).containsExactly("ROLE_USER");
        assertThat(response.enabled()).isTrue();
        assertThat(response.createdAt()).isNotNull();

        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when user not found for info")
    void getCurrentUserInfo_WithNonExistingUser_ShouldThrowException() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.getCurrentUserInfo(TEST_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: test@example.com");

        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should return all roles in user info")
    void getCurrentUserInfo_WithMultipleRoles_ShouldReturnAllRoles() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .build();

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        UserEntity multiRoleUser = UserEntity.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(multiRoleUser));

        UserInfoResponse response = authenticationService.getCurrentUserInfo(TEST_EMAIL);

        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_WithValidRequest_ShouldUpdatePassword() {
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        String encodedNewPassword = "encodedNewPassword";

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, ENCODED_PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        authenticationService.changePassword(TEST_EMAIL, request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo(encodedNewPassword);

        verify(passwordEncoder).matches(currentPassword, ENCODED_PASSWORD);
        verify(passwordEncoder).matches(newPassword, ENCODED_PASSWORD);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void changePassword_WithIncorrectCurrentPassword_ShouldThrowException() {
        String wrongCurrentPassword = "wrongPassword";
        String newPassword = "newPassword";

        ChangePasswordRequest request = new ChangePasswordRequest(wrongCurrentPassword, newPassword);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongCurrentPassword, ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.changePassword(TEST_EMAIL, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when new password is same as current")
    void changePassword_WithSamePassword_ShouldThrowException() {
        String currentPassword = "currentPassword";
        String samePassword = "currentPassword";

        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, samePassword);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.matches(samePassword, ENCODED_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.changePassword(TEST_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New password must be different from the current password");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for password change")
    void changePassword_WithNonExistingUser_ShouldThrowException() {
        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "newPassword");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.changePassword(TEST_EMAIL, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: test@example.com");

        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
