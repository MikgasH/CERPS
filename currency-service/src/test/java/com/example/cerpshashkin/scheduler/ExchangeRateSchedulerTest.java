package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateSchedulerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateScheduler scheduler;

    @Test
    void initializeExchangeRates_WithSuccessfulRefresh_ShouldComplete() {
        doNothing().when(exchangeRateService).refreshRates();

        scheduler.initializeExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    void initializeExchangeRates_WithFailure_ShouldLogErrorAndNotThrow() {
        doThrow(new RuntimeException("All providers failed"))
                .when(exchangeRateService).refreshRates();

        scheduler.initializeExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    void updateExchangeRates_WithSuccessfulRefresh_ShouldComplete() {
        doNothing().when(exchangeRateService).refreshRates();

        scheduler.updateExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    void updateExchangeRates_WithFailure_ShouldLogErrorAndNotThrow() {
        doThrow(new RuntimeException("Update failed"))
                .when(exchangeRateService).refreshRates();

        scheduler.updateExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }
}
