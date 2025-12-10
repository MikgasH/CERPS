package org.example.userservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final String ROLE_USER = "ROLE_USER";

    private static final String LOG_REGISTER_START = "Registering new user: {}";
    private static final String LOG_REGISTER_SUCCESS = "User registered successfully: {}";
    private static final String LOG_LOGIN_START = "User login attempt: {}";
    private static final String LOG_LOGIN_SUCCESS = "User logged in successfully: {}";
    private static final String LOG_GET_USER_INFO = "Getting user info for: {}";
    private static final String LOG_CHANGE_PASSWORD = "Changing password for user: {}";
    private static final String LOG_PASSWORD_CHANGED = "Password changed successfully for user: {}";

    private static final String ERROR_EMAIL_EXISTS = "Email '%s' is already registered";
    private static final String ERROR_ROLE_NOT_FOUND = "Default role not found: %s";
    private static final String ERROR_USER_NOT_FOUND_FOR_INFO = "User not found: %s";
    private static final String ERROR_INVALID_CURRENT_PASSWORD = "Current password is incorrect";
    private static final String ERROR_SAME_PASSWORD = "New password must be different from the current password";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MeterRegistry meterRegistry;

    private Counter registrationSuccessCounter;
    private Counter registrationFailureCounter;
    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;

    @PostConstruct
    public void initMetrics() {
        registrationSuccessCounter = Counter.builder("user.registrations.success")
                .description("Number of successful user registrations")
                .register(meterRegistry);

        registrationFailureCounter = Counter.builder("user.registrations.failure")
                .description("Number of failed user registrations")
                .register(meterRegistry);

        loginSuccessCounter = Counter.builder("user.logins.success")
                .description("Number of successful user logins")
                .register(meterRegistry);

        loginFailureCounter = Counter.builder("user.logins.failure")
                .description("Number of failed user logins")
                .register(meterRegistry);
    }

    @Transactional
    public void register(final RegisterRequest request) {
        log.info(LOG_REGISTER_START, request.email());

        try {
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException(
                        String.format(ERROR_EMAIL_EXISTS, request.email())
                );
            }

            final RoleEntity userRole = roleRepository.findByName(ROLE_USER)
                    .orElseThrow(() -> new RuntimeException(
                            String.format(ERROR_ROLE_NOT_FOUND, ROLE_USER)
                    ));

            final Set<RoleEntity> roles = new HashSet<>();
            roles.add(userRole);

            final UserEntity user = UserEntity.builder()
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .enabled(true)
                    .createdAt(Instant.now())
                    .roles(roles)
                    .build();

            userRepository.save(user);
            registrationSuccessCounter.increment();
            log.info(LOG_REGISTER_SUCCESS, request.email());
        } catch (Exception e) {
            registrationFailureCounter.increment();
            throw e;
        }
    }

    public LoginResponse login(final LoginRequest request) {
        log.info(LOG_LOGIN_START, request.email());

        try {
            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String token = jwtService.generateToken(userDetails);

            final List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            loginSuccessCounter.increment();
            log.info(LOG_LOGIN_SUCCESS, request.email());
            return LoginResponse.success(token, request.email(), roles);
        } catch (Exception e) {
            loginFailureCounter.increment();
            throw e;
        }
    }

    public UserInfoResponse getCurrentUserInfo(final String email) {
        log.info(LOG_GET_USER_INFO, email);

        final UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format(ERROR_USER_NOT_FOUND_FOR_INFO, email)
                ));

        final List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();

        return UserInfoResponse.from(
                user.getId(),
                user.getEmail(),
                roles,
                user.getEnabled(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(final String email, final ChangePasswordRequest request) {
        log.info(LOG_CHANGE_PASSWORD, email);

        final UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format(ERROR_USER_NOT_FOUND_FOR_INFO, email)
                ));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException(ERROR_INVALID_CURRENT_PASSWORD);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException(ERROR_SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info(LOG_PASSWORD_CHANGED, email);
    }
}
