package com.example.cerpshashkin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportedCurrenciesService {

    // The cached lookup lives in its own bean so both views below go through
    // the @Cacheable proxy. A @Lazy self-proxy worked on the JVM but failed
    // GraalVM native-image AOT (circular self-injection cannot be instantiated).
    private final CurrencyCodeCacheService currencyCodeCacheService;

    public List<String> getSupportedCurrencyCodes() {
        return currencyCodeCacheService.getSupportedCurrencyCodes();
    }

    public Set<String> getSupportedCurrencyCodesAsSet() {
        return currencyCodeCacheService.getSupportedCurrencyCodes().stream()
                .collect(Collectors.toUnmodifiableSet());
    }
}
