package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseCleanupSchedulerTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private DatabaseCleanupScheduler scheduler;

    @Test
    void cleanupOldRates_ShouldDeleteRatesOlderThan395Days() {
        when(exchangeRateRepository.deleteByTimestampBefore(any(Instant.class)))
                .thenReturn(42);

        scheduler.cleanupOldRates();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(exchangeRateRepository).deleteByTimestampBefore(cutoffCaptor.capture());

        Instant capturedCutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = Instant.now().minus(395, ChronoUnit.DAYS);

        assertThat(capturedCutoff).isCloseTo(expectedCutoff, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void cleanupOldRates_WithSuccessfulDeletion_ShouldLogCount() {
        when(exchangeRateRepository.deleteByTimestampBefore(any(Instant.class)))
                .thenReturn(100);

        scheduler.cleanupOldRates();

        verify(exchangeRateRepository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    void cleanupOldRates_WithNoDeletions_ShouldComplete() {
        when(exchangeRateRepository.deleteByTimestampBefore(any(Instant.class)))
                .thenReturn(0);

        scheduler.cleanupOldRates();

        verify(exchangeRateRepository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    void cleanupOldRates_WithException_ShouldCatchAndLog() {
        doThrow(new RuntimeException("Database error"))
                .when(exchangeRateRepository).deleteByTimestampBefore(any(Instant.class));

        scheduler.cleanupOldRates();

        verify(exchangeRateRepository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    void cleanupOldRates_ShouldUseCorrectRetentionPeriod() {
        when(exchangeRateRepository.deleteByTimestampBefore(any(Instant.class)))
                .thenReturn(5);

        scheduler.cleanupOldRates();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(exchangeRateRepository).deleteByTimestampBefore(cutoffCaptor.capture());

        Instant capturedCutoff = cutoffCaptor.getValue();
        long daysDifference = ChronoUnit.DAYS.between(capturedCutoff, Instant.now());

        assertThat(daysDifference).isBetween(394L, 396L);
    }
}
