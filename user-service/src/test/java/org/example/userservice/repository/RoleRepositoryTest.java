package org.example.userservice.repository;

import org.example.userservice.entity.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
    }

    @Test
    void should_ReturnRole_When_FindingByExistingName() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        roleRepository.save(role);

        Optional<RoleEntity> result = roleRepository.findByName("ROLE_USER");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("ROLE_USER");
    }

    @Test
    void should_ReturnEmpty_When_FindingByNonExistingName() {
        Optional<RoleEntity> result = roleRepository.findByName("ROLE_NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void should_SaveRole_When_RoleIsValid() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .build();

        RoleEntity savedRole = roleRepository.save(role);

        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void should_ReturnAllRoles_When_MultipleSaved() {
        RoleEntity roleUser = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        RoleEntity roleAdmin = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .build();

        RoleEntity roleModerator = RoleEntity.builder()
                .name("ROLE_MODERATOR")
                .build();

        roleRepository.saveAll(List.of(roleUser, roleAdmin, roleModerator));

        List<RoleEntity> allRoles = roleRepository.findAll();

        assertThat(allRoles).hasSize(3);
        assertThat(allRoles).extracting(RoleEntity::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
    }

    @Test
    void should_UpdateRole_When_RoleExists() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        RoleEntity savedRole = roleRepository.save(role);
        Long roleId = savedRole.getId();

        savedRole.setName("ROLE_UPDATED");
        roleRepository.save(savedRole);

        Optional<RoleEntity> updatedRole = roleRepository.findById(roleId);

        assertThat(updatedRole).isPresent();
        assertThat(updatedRole.get().getName()).isEqualTo("ROLE_UPDATED");
    }

    @Test
    void should_DeleteRole_When_RoleExists() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        RoleEntity savedRole = roleRepository.save(role);
        Long roleId = savedRole.getId();

        roleRepository.deleteById(roleId);

        Optional<RoleEntity> result = roleRepository.findById(roleId);
        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnCount_When_CountingRoles() {
        RoleEntity roleUser = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        RoleEntity roleAdmin = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .build();

        roleRepository.saveAll(List.of(roleUser, roleAdmin));

        long count = roleRepository.count();

        assertThat(count).isEqualTo(2);
    }
}
