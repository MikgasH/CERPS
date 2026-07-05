package com.example.cerpshashkin.unit.controller;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerpshashkin.controller.CurrencyController;
import com.example.cerpshashkin.service.CurrencyService;
import com.example.cerpshashkin.service.HistoricalRateService;
import com.example.cerpshashkin.service.RateHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CurrencyControllerTest {

    private static final String HISTORY_URL = "/api/v1/rates/history";

    @Mock
    private CurrencyService currencyService;

    @Mock
    private RateHistoryService rateHistoryService;

    @Mock
    private HistoricalRateService historicalRateService;

    @InjectMocks
    private CurrencyController currencyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(currencyController).build();
        when(rateHistoryService.getRateHistory(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(new RateHistoryResponse("EUR", "USD", List.of()));
    }

    @Test
    void getRateHistory_ShouldDefaultEndDateToNow_WhenOnlyStartDateProvided() throws Exception {
        final Instant startDate = Instant.parse("2026-06-01T00:00:00Z");
        final Instant before = Instant.now();

        mockMvc.perform(get(HISTORY_URL)
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("startDate", startDate.toString()))
                .andExpect(status().isOk());

        final ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(rateHistoryService).getRateHistory(eq("EUR"), eq("USD"), eq(startDate), endCaptor.capture());
        // Open-ended range: the missing end bound closes at "now", so the
        // query's BETWEEN never evaluates to NULL and silently matches nothing.
        assertThat(endCaptor.getValue()).isBetween(before, Instant.now());
    }

    @Test
    void getRateHistory_ShouldDefaultStartDateToEpoch_WhenOnlyEndDateProvided() throws Exception {
        final Instant endDate = Instant.parse("2026-06-01T00:00:00Z");

        mockMvc.perform(get(HISTORY_URL)
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(rateHistoryService).getRateHistory("EUR", "USD", Instant.EPOCH, endDate);
    }

    @Test
    void getRateHistory_ShouldPassBoundsThrough_WhenBothProvided() throws Exception {
        final Instant startDate = Instant.parse("2026-05-01T00:00:00Z");
        final Instant endDate = Instant.parse("2026-06-01T00:00:00Z");

        mockMvc.perform(get(HISTORY_URL)
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(rateHistoryService).getRateHistory("EUR", "USD", startDate, endDate);
    }

    @Test
    void getRateHistory_ShouldPassNulls_WhenNoBoundsProvided() throws Exception {
        mockMvc.perform(get(HISTORY_URL)
                        .param("from", "EUR")
                        .param("to", "USD"))
                .andExpect(status().isOk());

        verify(rateHistoryService).getRateHistory("EUR", "USD", null, null);
    }
}
