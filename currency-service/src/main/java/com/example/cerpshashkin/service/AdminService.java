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
        log.info("Refreshing exchange rates");
        exchangeRateService.refreshRates();
    }

    public ProviderKeyResponse createProviderKey(final CreateProviderKeyRequest request) {
        return providerKeyService.createProviderKey(request);
    }

    public List<ProviderKeyResponse> getAllProviderKeys() {
        return providerKeyService.getAllActiveProviderKeys();
    }

    public ProviderKeyResponse getProviderKey(final Long id) {
        return providerKeyService.getProviderKey(id);
    }

    public ProviderKeyResponse updateProviderKey(final Long id, final UpdateProviderKeyRequest request) {
        return providerKeyService.updateProviderKey(id, request);
    }

    public void deleteProviderKey(final Long id) {
        providerKeyService.deleteProviderKey(id);
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
