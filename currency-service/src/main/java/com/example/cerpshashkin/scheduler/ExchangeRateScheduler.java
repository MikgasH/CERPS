package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.service.ExchangeRateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    private static final String LOG_SCHEDULED_UPDATE_START = "Starting scheduled exchange rates update";
    private static final String LOG_SCHEDULED_UPDATE_SUCCESS = "Scheduled exchange rates update completed successfully";
    private static final String LOG_SCHEDULED_UPDATE_FAILED = "Scheduled exchange rates update failed: {}";

    private final ExchangeRateService exchangeRateService;

    @PostConstruct
    public void initializeExchangeRates() {
        log.info(LOG_INIT_START);
        try {
            exchangeRateService.refreshRates();
            log.info(LOG_INIT_SUCCESS);
        } catch (Exception e) {
            log.error(LOG_INIT_FAILED, e.getMessage(), e);
        }
    }

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
