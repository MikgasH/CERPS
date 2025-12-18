package org.example.userservice.unit.service;

import org.example.userservice.entity.RoleEntity;
import org.example.userservice.entity.UserEntity;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private UserEntity testUser;
    private RoleEntity userRole;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {
        userRole = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .createdAt(Instant.now())
                .build();

        adminRole = RoleEntity.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .createdAt(Instant.now())
                .build();

        testUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should convert roles to authorities correctly")
    void loadUserByUsername_ShouldConvertRolesToAuthorities() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should load user with multiple roles")
    void loadUserByUsername_WithMultipleRoles_ShouldReturnAllAuthorities() {
        testUser.setRoles(Set.of(userRole, adminRole));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void loadUserByUsername_WithNonExistentEmail_ShouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent@example.com");

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should mark user as disabled when enabled=false")
    void loadUserByUsername_WithDisabledUser_ShouldReturnDisabledUserDetails() {
        testUser.setEnabled(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should handle user with no roles")
    void loadUserByUsername_WithNoRoles_ShouldReturnEmptyAuthorities() {
        testUser.setRoles(Set.of());

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Should return correct account status flags")
    void loadUserByUsername_ShouldSetCorrectAccountFlags() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
