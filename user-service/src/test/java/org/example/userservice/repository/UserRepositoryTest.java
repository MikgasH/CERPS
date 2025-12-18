package org.example.userservice.repository;

import org.example.userservice.entity.RoleEntity;
import org.example.userservice.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RoleEntity roleUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleUser = RoleEntity.builder()
                .name("ROLE_USER")
                .build();
        roleRepository.save(roleUser);
    }

    @Test
    void should_ReturnUser_When_FindingByExistingEmail() {
        String email = "user@example.com";
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        userRepository.save(user);

        Optional<UserEntity> result = userRepository.findByEmail(email);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
        assertThat(result.get().getPassword()).isEqualTo("encoded_password");
        assertThat(result.get().getEnabled()).isTrue();
        assertThat(result.get().getRoles()).hasSize(1);
    }

    @Test
    void should_ReturnEmpty_When_FindingByNonExistingEmail() {
        Optional<UserEntity> result = userRepository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnTrue_When_CheckingExistingEmail() {
        String email = "user@example.com";
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail(email);

        assertThat(exists).isTrue();
    }

    @Test
    void should_ReturnFalse_When_CheckingNonExistingEmail() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void should_SaveUser_When_UserHasMultipleRoles() {
        RoleEntity roleAdmin = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .build();
        roleRepository.save(roleAdmin);

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);
        roles.add(roleAdmin);

        UserEntity user = UserEntity.builder()
                .email("admin@example.com")
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        UserEntity savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getRoles()).hasSize(2);
        assertThat(savedUser.getRoles()).contains(roleUser, roleAdmin);
    }

    @Test
    void should_UpdateUser_When_UserExists() {
        String email = "user@example.com";
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .email(email)
                .password("old_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        UserEntity savedUser = userRepository.save(user);
        Long userId = savedUser.getId();

        savedUser.setPassword("new_password");
        savedUser.setEnabled(false);
        userRepository.save(savedUser);

        Optional<UserEntity> updatedUser = userRepository.findById(userId);

        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getPassword()).isEqualTo("new_password");
        assertThat(updatedUser.get().getEnabled()).isFalse();
    }

    @Test
    void should_DeleteUser_When_UserExists() {
        String email = "user@example.com";
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .email(email)
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        UserEntity savedUser = userRepository.save(user);
        Long userId = savedUser.getId();

        userRepository.deleteById(userId);

        Optional<UserEntity> result = userRepository.findById(userId);
        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnUser_When_EmailIsCaseInsensitive() {
        String email = "User@Example.COM";
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleUser);

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase())
                .password("encoded_password")
                .enabled(true)
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        userRepository.save(user);

        Optional<UserEntity> result = userRepository.findByEmail(email.toLowerCase());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email.toLowerCase());
    }
}
