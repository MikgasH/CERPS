package com.example.cerpshashkin.service;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns the cached supported-currency lookup. Keeping the {@code @Cacheable}
 * method on its own bean (rather than a {@code @Lazy} self-proxy in
 * {@link SupportedCurrenciesService}) lets the cache advice fire for every
 * caller — including the Set view — while staying compatible with GraalVM
 * native-image AOT, which cannot instantiate circular self-injected beans.
 */
@Service
@RequiredArgsConstructor
public class CurrencyCodeCacheService {

    private final SupportedCurrencyRepository supportedCurrencyRepository;

    @Cacheable("supportedCurrencies")
    @Transactional(readOnly = true)
    public List<String> getSupportedCurrencyCodes() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .sorted()
                .toList();
    }
}
