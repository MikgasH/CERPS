package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.entity.ApiProviderKeyEntity;
import com.example.cerpshashkin.exception.ProviderKeyNotFoundException;
import com.example.cerpshashkin.exception.ProviderKeyNotFoundByNameException;
import com.example.cerpshashkin.mapper.ProviderKeyMapper;
import com.example.cerpshashkin.repository.ApiProviderKeyRepository;
import com.example.cerpshashkin.service.EncryptionService;
import com.example.cerpshashkin.service.ProviderKeyManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderKeyManagementServiceTest {

    @Mock
    private ApiProviderKeyRepository repository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ProviderKeyMapper mapper;

    @InjectMocks
    private ProviderKeyManagementService service;

    @Test
    void should_CreateProviderKey_When_ValidRequest() {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("fixer", "plaintext_key");
        String encryptedKey = "encrypted_key_value";

        ApiProviderKeyEntity savedEntity = ApiProviderKeyEntity.builder()
                .id(1L)
                .providerName("fixer")
                .encryptedApiKey(encryptedKey)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProviderKeyResponse expectedResponse = new ProviderKeyResponse(
                1L, "fixer", true, Instant.now(), Instant.now()
        );

        when(encryptionService.encrypt("plaintext_key")).thenReturn(encryptedKey);
        when(repository.save(any(ApiProviderKeyEntity.class))).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        ProviderKeyResponse result = service.createProviderKey(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.providerName()).isEqualTo("fixer");
        assertThat(result.active()).isTrue();

        ArgumentCaptor<ApiProviderKeyEntity> entityCaptor = ArgumentCaptor.forClass(ApiProviderKeyEntity.class);
        verify(repository).save(entityCaptor.capture());

        ApiProviderKeyEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getProviderName()).isEqualTo("fixer");
        assertThat(capturedEntity.getEncryptedApiKey()).isEqualTo(encryptedKey);
        assertThat(capturedEntity.getActive()).isTrue();

        verify(encryptionService).encrypt("plaintext_key");
        verify(mapper).toResponse(savedEntity);
    }

    @Test
    void should_ReturnProviderKey_When_ValidId() {
        Long id = 1L;
        ApiProviderKeyEntity entity = createEntity(id, "fixer", "encrypted_key", true);
        ProviderKeyResponse expectedResponse = new ProviderKeyResponse(
                id, "fixer", true, Instant.now(), Instant.now()
        );

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(expectedResponse);

        ProviderKeyResponse result = service.getProviderKey(id);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.providerName()).isEqualTo("fixer");

        verify(repository).findById(id);
        verify(mapper).toResponse(entity);
    }

    @Test
    void should_ThrowProviderKeyNotFoundException_When_IdNotFound() {
        Long id = 999L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProviderKey(id))
                .isInstanceOf(ProviderKeyNotFoundException.class);

        verify(repository).findById(id);
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void should_ReturnAllActiveKeys_When_Called() {
        List<ApiProviderKeyEntity> entities = List.of(
                createEntity(1L, "fixer", "encrypted1", true),
                createEntity(2L, "currencyapi", "encrypted2", true),
                createEntity(3L, "exchangerates", "encrypted3", true)
        );

        List<ProviderKeyResponse> expectedResponses = List.of(
                new ProviderKeyResponse(1L, "fixer", true, Instant.now(), Instant.now()),
                new ProviderKeyResponse(2L, "currencyapi", true, Instant.now(), Instant.now()),
                new ProviderKeyResponse(3L, "exchangerates", true, Instant.now(), Instant.now())
        );

        when(repository.findAllByActiveTrue()).thenReturn(entities);
        when(mapper.toResponseList(entities)).thenReturn(expectedResponses);

        List<ProviderKeyResponse> result = service.getAllActiveProviderKeys();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProviderKeyResponse::providerName)
                .containsExactly("fixer", "currencyapi", "exchangerates");

        verify(repository).findAllByActiveTrue();
        verify(mapper).toResponseList(entities);
    }

    @Test
    void should_UpdateProviderKey_When_ValidRequest() {
        Long id = 1L;
        UpdateProviderKeyRequest request = new UpdateProviderKeyRequest("new_plaintext_key");
        String newEncryptedKey = "new_encrypted_key";

        ApiProviderKeyEntity existingEntity = createEntity(id, "fixer", "old_encrypted_key", true);
        ApiProviderKeyEntity updatedEntity = createEntity(id, "fixer", newEncryptedKey, true);

        ProviderKeyResponse expectedResponse = new ProviderKeyResponse(
                id, "fixer", true, Instant.now(), Instant.now()
        );

        when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(encryptionService.encrypt("new_plaintext_key")).thenReturn(newEncryptedKey);
        when(repository.save(any(ApiProviderKeyEntity.class))).thenReturn(updatedEntity);
        when(mapper.toResponse(updatedEntity)).thenReturn(expectedResponse);

        ProviderKeyResponse result = service.updateProviderKey(id, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);

        verify(repository).findById(id);
        verify(encryptionService).encrypt("new_plaintext_key");
        verify(repository).save(existingEntity);
        verify(mapper).toResponse(updatedEntity);
    }

    @Test
    void should_ThrowProviderKeyNotFoundException_When_UpdateWithInvalidId() {
        Long id = 999L;
        UpdateProviderKeyRequest request = new UpdateProviderKeyRequest("new_key");

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProviderKey(id, request))
                .isInstanceOf(ProviderKeyNotFoundException.class);

        verify(repository).findById(id);
        verify(encryptionService, never()).encrypt(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void should_DeactivateProviderKey_When_ValidId() {
        Long id = 1L;
        ApiProviderKeyEntity entity = createEntity(id, "fixer", "encrypted_key", true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(any(ApiProviderKeyEntity.class))).thenReturn(entity);

        service.deleteProviderKey(id);

        assertThat(entity.getActive()).isFalse();

        verify(repository).findById(id);
        verify(repository).save(entity);
    }

    @Test
    void should_ThrowProviderKeyNotFoundException_When_DeleteWithInvalidId() {
        Long id = 999L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProviderKey(id))
                .isInstanceOf(ProviderKeyNotFoundException.class);

        verify(repository).findById(id);
        verify(repository, never()).save(any());
    }

    @Test
    void should_ReturnDecryptedKey_When_ProviderNameExists() {
        String providerName = "fixer";
        String encryptedKey = "encrypted_key_value";
        String decryptedKey = "plaintext_key_value";

        ApiProviderKeyEntity entity = createEntity(1L, providerName, encryptedKey, true);

        when(repository.findByProviderNameAndActiveTrue(providerName)).thenReturn(Optional.of(entity));
        when(encryptionService.decrypt(encryptedKey)).thenReturn(decryptedKey);

        String result = service.getDecryptedApiKey(providerName);

        assertThat(result).isEqualTo(decryptedKey);

        verify(repository).findByProviderNameAndActiveTrue(providerName);
        verify(encryptionService).decrypt(encryptedKey);
    }

    @Test
    void should_ThrowProviderKeyNotFoundByNameException_When_ProviderNameNotFound() {
        String providerName = "nonexistent";

        when(repository.findByProviderNameAndActiveTrue(providerName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDecryptedApiKey(providerName))
                .isInstanceOf(ProviderKeyNotFoundByNameException.class);

        verify(repository).findByProviderNameAndActiveTrue(providerName);
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    void should_EvictCacheForProvider_When_Called() {
        String providerName = "fixer";

        service.evictCacheForProvider(providerName);

        verify(repository, never()).findByProviderNameAndActiveTrue(anyString());
        verify(encryptionService, never()).decrypt(anyString());
    }

    private ApiProviderKeyEntity createEntity(Long id, String providerName, String encryptedKey, boolean active) {
        return ApiProviderKeyEntity.builder()
                .id(id)
                .providerName(providerName)
                .encryptedApiKey(encryptedKey)
                .active(active)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
