package com.example.cerpshashkin.integration.repository;

import com.example.cerpshashkin.entity.ApiProviderKeyEntity;
import com.example.cerpshashkin.repository.ApiProviderKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiProviderKeyRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false)
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private ApiProviderKeyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByName() {
        Instant now = Instant.now();
        ApiProviderKeyEntity entity = ApiProviderKeyEntity.builder()
                .providerName("fixer")
                .encryptedApiKey("encrypted_test_key_12345")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ApiProviderKeyEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();

        Optional<ApiProviderKeyEntity> found = repository.findByProviderNameAndActiveTrue("fixer");

        assertThat(found).isPresent();
        assertThat(found.get().getProviderName()).isEqualTo("fixer");
        assertThat(found.get().getEncryptedApiKey()).isEqualTo("encrypted_test_key_12345");
        assertThat(found.get().getActive()).isTrue();
    }

    @Test
    void shouldFindOnlyActiveKeys() {
        Instant now = Instant.now();

        ApiProviderKeyEntity activeKey1 = ApiProviderKeyEntity.builder()
                .providerName("fixer")
                .encryptedApiKey("encrypted_key_1")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ApiProviderKeyEntity activeKey2 = ApiProviderKeyEntity.builder()
                .providerName("currencyapi")
                .encryptedApiKey("encrypted_key_2")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ApiProviderKeyEntity inactiveKey = ApiProviderKeyEntity.builder()
                .providerName("exchangerates")
                .encryptedApiKey("encrypted_key_3")
                .active(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.saveAll(List.of(activeKey1, activeKey2, inactiveKey));

        List<ApiProviderKeyEntity> activeKeys = repository.findAllByActiveTrue();

        assertThat(activeKeys).hasSize(2);
        assertThat(activeKeys).extracting(ApiProviderKeyEntity::getProviderName)
                .containsExactlyInAnyOrder("fixer", "currencyapi");

        Optional<ApiProviderKeyEntity> inactiveFound = repository.findByProviderNameAndActiveTrue("exchangerates");
        assertThat(inactiveFound).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<ApiProviderKeyEntity> found = repository.findByProviderNameAndActiveTrue("nonexistent");

        assertThat(found).isEmpty();
    }
}
