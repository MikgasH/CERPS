package com.example.cerpshashkin.service;

import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.entity.ApiProviderKeyEntity;
import com.example.cerpshashkin.exception.ProviderKeyNotFoundException;
import com.example.cerpshashkin.exception.ProviderKeyNotFoundByNameException;
import com.example.cerpshashkin.mapper.ProviderKeyMapper;
import com.example.cerpshashkin.repository.ApiProviderKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderKeyManagementService {

    private final ApiProviderKeyRepository repository;
    private final EncryptionService encryptionService;
    private final ProviderKeyMapper mapper;

    @Transactional
    @CacheEvict(value = "decryptedApiKeys", key = "#request.providerName()")
    public ProviderKeyResponse createProviderKey(final CreateProviderKeyRequest request) {
        log.info("Creating provider key for provider: {}", request.providerName());

        String encryptedKey = encryptionService.encrypt(request.apiKey());
        Instant now = Instant.now();

        ApiProviderKeyEntity entity = ApiProviderKeyEntity.builder()
                .providerName(request.providerName())
                .encryptedApiKey(encryptedKey)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ApiProviderKeyEntity saved = repository.save(entity);
        log.info("Provider key created successfully with id: {}", saved.getId());

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProviderKeyResponse getProviderKey(final Long id) {
        log.debug("Retrieving provider key with id: {}", id);

        ApiProviderKeyEntity entity = repository.findById(id)
                .orElseThrow(() -> new ProviderKeyNotFoundException(id));

        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProviderKeyResponse> getAllActiveProviderKeys() {
        log.debug("Retrieving all active provider keys");

        List<ApiProviderKeyEntity> entities = repository.findAllByActiveTrue();
        log.debug("Found {} active provider keys", entities.size());

        return mapper.toResponseList(entities);
    }

    @Transactional
    @CacheEvict(value = "decryptedApiKeys", key = "#result.providerName()")
    public ProviderKeyResponse updateProviderKey(final Long id, final UpdateProviderKeyRequest request) {
        log.info("Updating provider key with id: {}", id);

        ApiProviderKeyEntity entity = repository.findById(id)
                .orElseThrow(() -> new ProviderKeyNotFoundException(id));

        String encryptedKey = encryptionService.encrypt(request.apiKey());
        entity.setEncryptedApiKey(encryptedKey);
        entity.setUpdatedAt(Instant.now());

        ApiProviderKeyEntity updated = repository.save(entity);
        log.info("Provider key updated successfully with id: {}", updated.getId());

        return mapper.toResponse(updated);
    }

    @Transactional
    public void deleteProviderKey(final Long id) {
        log.info("Deactivating provider key with id: {}", id);

        ApiProviderKeyEntity entity = repository.findById(id)
                .orElseThrow(() -> new ProviderKeyNotFoundException(id));

        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);

        evictCacheForProvider(entity.getProviderName());
        log.info("Provider key deactivated successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "decryptedApiKeys", key = "#providerName")
    public String getDecryptedApiKey(final String providerName) {
        log.debug("Retrieving and decrypting API key for provider: {}", providerName);

        ApiProviderKeyEntity entity = repository.findByProviderNameAndActiveTrue(providerName)
                .orElseThrow(() -> new ProviderKeyNotFoundByNameException(providerName));

        String decryptedKey = encryptionService.decrypt(entity.getEncryptedApiKey());
        log.debug("Successfully decrypted API key for provider: {}", providerName);

        return decryptedKey;
    }

    @CacheEvict(value = "decryptedApiKeys", key = "#providerName")
    public void evictCacheForProvider(final String providerName) {
        log.debug("Evicting cache for provider: {}", providerName);
    }
}
