package org.example.analyticsservice.unit.service;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.exception.ExternalServiceException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.analyticsservice.client.CurrencyServiceClient;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.exception.MinimumPeriodNotSupportedException;
import org.example.analyticsservice.service.HistoricalRatesService;
import org.example.analyticsservice.service.TrendsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the period-based routing introduced in Task 4 step 7: 1D stays on
 * the legacy currency-service path, 7D..1Y go through the historical store
 * with the legacy path as fallback, 2Y/3Y go through the historical store
 * with NO fallback (the legacy source retains only ~13 months, so a fallback
 * would silently truncate the series), and the existing exception taxonomy
 * (404/422/503) is preserved across the fallback chain.
 */
@ExtendWith(MockitoExtension.class)
class TrendsServiceRoutingTest {

    @Mock
    private CurrencyServiceClient currencyServiceClient;

    @Mock
    private HistoricalRatesService historicalRatesService;

    private TrendsService service;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new TrendsService(currencyServiceClient, historicalRatesService, new SimpleMeterRegistry());
        service.initMetrics();

        when(currencyServiceClient.getSupportedCurrencies())
                .thenReturn(List.of("USD", "EUR", "GBP", "BYN", "RUB"));
    }

    @Test
    void calculateTrends_ShouldRouteToLegacyPath_When1DPeriod() {
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", twoPoints()));

        TrendsService.TrendsResult result = service.calculateTrends(new TrendsRequest("USD", "EUR", "1D"));

        assertThat(result.response()).isNotNull();
        assertThat(result.fromFallback()).isFalse();
        verifyNoInteractions(historicalRatesService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"7D", "30D", "90D", "180D", "1Y", "2Y", "3Y"})
    void calculateTrends_ShouldRouteToHistoricalStore_WhenPeriod7DOrLonger(final String period) {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(twoPoints());

        TrendsService.TrendsResult result = service.calculateTrends(new TrendsRequest("USD", "EUR", period));

        assertThat(result.response()).isNotNull();
        assertThat(result.response().period()).isEqualTo(period);
        assertThat(result.fromFallback()).isFalse();
        verify(currencyServiceClient, never()).getRateHistory(any(), any(), any(), any());
    }

    @Test
    void calculateTrends_ShouldPassDailyWindow_WhenRoutingToHistoricalStore() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(twoPoints());

        service.calculateTrends(new TrendsRequest("USD", "EUR", "7D"));

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(historicalRatesService).getRatePoints(eq("USD"), eq("EUR"),
                startCaptor.capture(), endCaptor.capture());
        assertThat(ChronoUnit.DAYS.between(startCaptor.getValue(), endCaptor.getValue())).isEqualTo(7);
    }

    @ParameterizedTest
    @CsvSource({"2Y, 2", "3Y, 3"})
    void calculateTrends_ShouldPassMultiYearWindow_WhenRoutingToHistoricalStore(
            final String period, final int years) {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(twoPoints());

        service.calculateTrends(new TrendsRequest("USD", "EUR", period));

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(historicalRatesService).getRatePoints(eq("USD"), eq("EUR"),
                startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue()).isEqualTo(endCaptor.getValue().minusYears(years));
    }

    @Test
    void calculateTrends_ShouldFallBackToLegacyPath_WhenHistoricalStoreInsufficient() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(List.of(new RatePoint(now, new BigDecimal("1.10"))));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", twoPoints()));

        TrendsService.TrendsResult result = service.calculateTrends(new TrendsRequest("USD", "EUR", "30D"));

        assertThat(result.response()).isNotNull();
        assertThat(result.fromFallback()).isTrue();
        verify(currencyServiceClient).getRateHistory(eq("USD"), eq("EUR"), any(), any());
    }

    @Test
    void calculateTrends_ShouldFallBackToLegacyPath_WhenHistoricalStoreThrows() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenThrow(new ExternalServiceException("Frankfurter unavailable"));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", twoPoints()));

        TrendsService.TrendsResult result = service.calculateTrends(new TrendsRequest("USD", "EUR", "1Y"));

        assertThat(result.response()).isNotNull();
        assertThat(result.fromFallback()).isTrue();
    }

    @Test
    void calculateTrends_ShouldThrowExternalServiceException_WhenBothSourcesFail() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenThrow(new ExternalServiceException("Frankfurter unavailable"));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenThrow(new ExternalServiceException("currency-service unavailable"));

        assertThatThrownBy(() -> service.calculateTrends(new TrendsRequest("USD", "EUR", "30D")))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void calculateTrends_ShouldThrowInsufficientData_WhenHistoricalFailsAndLegacyEmpty() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenThrow(new ExternalServiceException("Frankfurter unavailable"));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", List.of()));

        assertThatThrownBy(() -> service.calculateTrends(new TrendsRequest("USD", "EUR", "30D")))
                .isInstanceOf(InsufficientDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2Y", "3Y"})
    void calculateTrends_ShouldThrowInsufficientData_WhenStoreFailsAndPeriodExceedsLegacyRetention(
            final String period) {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenThrow(new ExternalServiceException("Frankfurter unavailable"));

        // The legacy source retains only ~13 months: a 2Y/3Y fallback would be
        // silently truncated, so the request must fail instead of falling back.
        assertThatThrownBy(() -> service.calculateTrends(new TrendsRequest("USD", "EUR", period)))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining(period);
        verify(currencyServiceClient, never()).getRateHistory(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2Y", "3Y"})
    void calculateTrends_ShouldThrowInsufficientData_WhenStoreSparseAndPeriodExceedsLegacyRetention(
            final String period) {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(List.of(new RatePoint(now, new BigDecimal("1.10"))));

        assertThatThrownBy(() -> service.calculateTrends(new TrendsRequest("USD", "EUR", period)))
                .isInstanceOf(InsufficientDataException.class);
        verify(currencyServiceClient, never()).getRateHistory(any(), any(), any(), any());
    }

    @Test
    void calculateTrends_ShouldTrimCurrencyCodes_WhenInputPadded() {
        when(historicalRatesService.getRatePoints(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(twoPoints());

        // Padded codes pass bean validation (the validator trims), so the
        // service must trim too instead of rejecting " USD" as unsupported.
        TrendsService.TrendsResult result = service.calculateTrends(new TrendsRequest(" usd", "eur ", "7D"));

        assertThat(result.response().from()).isEqualTo("USD");
        assertThat(result.response().to()).isEqualTo("EUR");
    }

    @Test
    void calculateTrends_ShouldStillReturn422Gate_When1DFrankfurterOnlyPair() {
        assertThatThrownBy(() -> service.calculateTrends(new TrendsRequest("EUR", "BYN", "1D")))
                .isInstanceOf(MinimumPeriodNotSupportedException.class)
                .hasMessageContaining("Minimum period is 7D");

        verifyNoInteractions(historicalRatesService);
        verify(currencyServiceClient, never()).getRateHistory(any(), any(), any(), any());
    }

    private List<RatePoint> twoPoints() {
        return List.of(
                new RatePoint(now.minus(7, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.18"))
        );
    }
}
