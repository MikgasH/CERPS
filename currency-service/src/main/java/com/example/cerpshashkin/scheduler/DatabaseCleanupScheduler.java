package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true")
public class DatabaseCleanupScheduler {

    private static final String LOG_CLEANUP_START = "Starting cleanup of old exchange rates";
    private static final String LOG_CLEANUP_SUCCESS = "Cleanup completed. Deleted {} old exchange rates";
    private static final String LOG_CLEANUP_FAILED = "Cleanup failed: {}";
    private static final int RETENTION_DAYS = 395;

    private final ExchangeRateRepository exchangeRateRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldRates() {
        log.info(LOG_CLEANUP_START);
        try {
            final Instant cutoffDate = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
            final int deleted = exchangeRateRepository.deleteByTimestampBefore(cutoffDate);
            log.info(LOG_CLEANUP_SUCCESS, deleted);
        } catch (Exception e) {
            log.error(LOG_CLEANUP_FAILED, e.getMessage(), e);
        }
    }
}
