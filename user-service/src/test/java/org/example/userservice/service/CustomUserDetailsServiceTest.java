package org.example.userservice.service;

import org.example.userservice.entity.RoleEntity;
import org.example.userservice.entity.UserEntity;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_WithExistingUser_ShouldReturnUserDetails() {
        String email = "user@example.com";
        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("encoded_password");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_WithMultipleRoles_ShouldReturnAllAuthorities() {
        String email = "admin@example.com";
        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();
        RoleEntity roleAdmin = RoleEntity.builder()
                .id(3L)
                .name("ROLE_ADMIN")
                .build();

        UserEntity userEntity = UserEntity.builder()
                .id(3L)
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser, roleAdmin))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(2);

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_WithDisabledUser_ShouldReturnDisabledUser() {
        String email = "disabled@example.com";
        RoleEntity roleUser = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        UserEntity userEntity = UserEntity.builder()
                .id(4L)
                .email(email)
                .password("encoded_password")
                .enabled(false)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_WithNonExistingUser_ShouldThrowException() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + email);

        verify(userRepository, times(1)).findByEmail(email);
    }
}

