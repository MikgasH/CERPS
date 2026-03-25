package com.example.cerpshashkin.unit.scheduler;

import com.example.cerpshashkin.scheduler.ExchangeRateScheduler;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "initRetryDelaySeconds", 0L);
    }

    @Test
    void initializeExchangeRates_WithSuccessfulRefresh_ShouldComplete() {
        doNothing().when(exchangeRateService).refreshRates();

        scheduler.initializeExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    void initializeExchangeRates_WithAllRetriesFailing_ShouldNotThrow() {
        doThrow(new RuntimeException("All providers failed"))
                .when(exchangeRateService).refreshRates();

        assertThatCode(() -> scheduler.initializeExchangeRates())
                .doesNotThrowAnyException();

        verify(exchangeRateService, times(3)).refreshRates();
    }

    @Test
    void initializeExchangeRates_WithSecondAttemptSucceeding_ShouldStopRetrying() {
        doThrow(new RuntimeException("First attempt failed"))
                .doNothing()
                .when(exchangeRateService).refreshRates();

        scheduler.initializeExchangeRates();

        verify(exchangeRateService, times(2)).refreshRates();
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
