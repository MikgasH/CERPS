package com.example.cerpshashkin.service;

import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final ExchangeRateService exchangeRateService;
    private final ProviderKeyManagementService providerKeyService;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    @Transactional
    public void addCurrency(final String currencyCode) {
        final String normalized = validateAndNormalize(currencyCode);

        if (supportedCurrencyRepository.existsByCurrencyCode(normalized)) {
            log.debug("Currency {} already exists", normalized);
            return;
        }

        supportedCurrencyRepository.save(
                SupportedCurrencyEntity.builder()
                        .currencyCode(normalized)
                        .build()
        );
        log.info("Currency {} added", normalized);
    }

    public void refreshExchangeRates() {
        log.info("AUDIT: Manual rate refresh initiated. operation=RATE_REFRESH, timestamp={}", Instant.now());
        try {
            exchangeRateService.refreshRates();
            log.info("AUDIT: Manual rate refresh completed. operation=RATE_REFRESH, result=SUCCESS, timestamp={}", Instant.now());
        } catch (Exception e) {
            log.warn("AUDIT: Manual rate refresh failed. operation=RATE_REFRESH, result=FAILURE, reason={}, timestamp={}",
                    e.getMessage(), Instant.now());
            throw e;
        }
    }

    public ProviderKeyResponse createProviderKey(final CreateProviderKeyRequest request) {
        log.info("AUDIT: Provider key creation. operation=PROVIDER_KEY_CREATE, provider={}, timestamp={}",
                request.providerName(), Instant.now());
        final ProviderKeyResponse response = providerKeyService.createProviderKey(request);
        log.info("AUDIT: Provider key created. operation=PROVIDER_KEY_CREATE, result=SUCCESS, provider={}, keyId={}, timestamp={}",
                request.providerName(), response.id(), Instant.now());
        return response;
    }

    public List<ProviderKeyResponse> getAllProviderKeys() {
        return providerKeyService.getAllActiveProviderKeys();
    }

    public ProviderKeyResponse getProviderKey(final Long id) {
        return providerKeyService.getProviderKey(id);
    }

    public ProviderKeyResponse updateProviderKey(final Long id, final UpdateProviderKeyRequest request) {
        log.info("AUDIT: Provider key update. operation=PROVIDER_KEY_UPDATE, keyId={}, timestamp={}", id, Instant.now());
        final ProviderKeyResponse response = providerKeyService.updateProviderKey(id, request);
        log.info("AUDIT: Provider key updated. operation=PROVIDER_KEY_UPDATE, result=SUCCESS, keyId={}, provider={}, timestamp={}",
                id, response.providerName(), Instant.now());
        return response;
    }

    public void deleteProviderKey(final Long id) {
        log.info("AUDIT: Provider key deletion. operation=PROVIDER_KEY_DELETE, keyId={}, timestamp={}", id, Instant.now());
        providerKeyService.deleteProviderKey(id);
        log.info("AUDIT: Provider key deactivated. operation=PROVIDER_KEY_DELETE, result=SUCCESS, keyId={}, timestamp={}",
                id, Instant.now());
    }

    private String validateAndNormalize(final String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new InvalidCurrencyException("Currency code cannot be empty");
        }

        final String normalized = currencyCode.trim().toUpperCase();

        try {
            Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currencyCode);
        }
    }
}
