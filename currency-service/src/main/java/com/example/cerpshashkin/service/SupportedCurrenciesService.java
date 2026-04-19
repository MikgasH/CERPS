package com.example.cerpshashkin.service;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportedCurrenciesService {

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

    public Set<String> getSupportedCurrencyCodesAsSet() {
        return getSupportedCurrencyCodes().stream().collect(Collectors.toUnmodifiableSet());
    }
}
