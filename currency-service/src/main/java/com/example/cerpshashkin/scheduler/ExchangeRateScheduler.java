package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.service.ExchangeRateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true")
public class ExchangeRateScheduler {

    private static final String LOG_INIT_START = "Initializing exchange rates on application startup";
    private static final String LOG_INIT_SUCCESS = "Exchange rates initialized successfully";
    private static final String LOG_INIT_FAILED = "Failed to initialize exchange rates: {}";
    private static final String LOG_INIT_RETRY = "Retrying exchange rate initialization (attempt {}/{}) in {}s";
    private static final String LOG_INIT_EXHAUSTED = "All {} startup attempts failed. Service starts with empty cache.";

    private static final String LOG_SCHEDULED_UPDATE_START = "Starting scheduled exchange rates update";
    private static final String LOG_SCHEDULED_UPDATE_SUCCESS = "Scheduled exchange rates update completed successfully";
    private static final String LOG_SCHEDULED_UPDATE_FAILED = "Scheduled exchange rates update failed: {}";

    private static final int MAX_INIT_ATTEMPTS = 3;

    @Value("${scheduling.exchange-rates.init-retry-delay-seconds:30}")
    private long initRetryDelaySeconds;

    private final ExchangeRateService exchangeRateService;

    @PostConstruct
    public void initializeExchangeRates() {
        log.info(LOG_INIT_START);
        for (int attempt = 1; attempt <= MAX_INIT_ATTEMPTS; attempt++) {
            try {
                exchangeRateService.refreshRates();
                log.info(LOG_INIT_SUCCESS);
                return;
            } catch (Exception e) {
                log.error(LOG_INIT_FAILED, e.getMessage(), e);
                if (attempt < MAX_INIT_ATTEMPTS) {
                    log.warn(LOG_INIT_RETRY, attempt + 1, MAX_INIT_ATTEMPTS, initRetryDelaySeconds);
                    try {
                        Thread.sleep(initRetryDelaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Startup retry interrupted");
                        return;
                    }
                }
            }
        }
        log.error(LOG_INIT_EXHAUSTED, MAX_INIT_ATTEMPTS);
    }

    @CacheEvict(value = "currentRates", allEntries = true)
    @Scheduled(fixedRateString = "${scheduling.exchange-rates.rate:3600000}")
    public void updateExchangeRates() {
        log.info(LOG_SCHEDULED_UPDATE_START);
        try {
            exchangeRateService.refreshRates();
            log.info(LOG_SCHEDULED_UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(LOG_SCHEDULED_UPDATE_FAILED, e.getMessage(), e);
        }
    }
}
