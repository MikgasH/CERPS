package com.example.cerpshashkin.service;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupportedCurrenciesService {

    private final SupportedCurrencyRepository supportedCurrencyRepository;

    // Self-reference resolved through the Spring proxy. The Set view must call
    // getSupportedCurrencyCodes() via this proxy so the @Cacheable advice fires;
    // a plain this.getSupportedCurrencyCodes() would bypass the proxy and run
    // findAll() on every invocation (the self-invocation cache trap).
    private final SupportedCurrenciesService self;

    public SupportedCurrenciesService(final SupportedCurrencyRepository supportedCurrencyRepository,
                                      @Lazy final SupportedCurrenciesService self) {
        this.supportedCurrencyRepository = supportedCurrencyRepository;
        this.self = self;
    }

    @Cacheable("supportedCurrencies")
    @Transactional(readOnly = true)
    public List<String> getSupportedCurrencyCodes() {
        return supportedCurrencyRepository.findAll()
                .stream()
                .map(SupportedCurrencyEntity::getCurrencyCode)
                .sorted()
                .toList();
    }

    public Set<String> getSupportedCurrencyCodesAsSet() {
        return self.getSupportedCurrencyCodes().stream().collect(Collectors.toUnmodifiableSet());
    }
}
