package org.example.analyticsservice.unit.service;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.analyticsservice.client.CurrencyServiceClient;
import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.service.TrendsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendsServiceTest {

    @Mock
    private CurrencyServiceClient currencyServiceClient;

    private TrendsService service;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new TrendsService(currencyServiceClient, new SimpleMeterRegistry());
        service.initMetrics();

        when(currencyServiceClient.getSupportedCurrencies())
                .thenReturn(List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD"));
    }

    @Test
    void calculateTrends_WithSufficientData_ShouldReturnTrend() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(7, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.18"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.from()).isEqualTo("USD");
        assertThat(response.to()).isEqualTo("EUR");
        assertThat(response.changePercentage()).isEqualByComparingTo("7.27");
    }

    @Test
    void calculateTrends_WithInsufficientData_ShouldThrowException() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", List.of()));

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("No exchange rate data available");
    }

    @Test
    void calculateTrends_WithPositiveChange_ShouldReturnPositivePercentage() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(1, ChronoUnit.DAYS), new BigDecimal("1.00")),
                new RatePoint(now, new BigDecimal("1.25"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.changePercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void calculateTrends_WithNegativeChange_ShouldReturnNegativePercentage() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(1, ChronoUnit.DAYS), new BigDecimal("2.00")),
                new RatePoint(now, new BigDecimal("1.50"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.changePercentage()).isEqualByComparingTo("-25.00");
    }

    @Test
    void calculateTrends_WithInvalidPeriod_ShouldThrowException() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7X");

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid period unit");
    }

    @Test
    void calculateTrends_WithUnsupportedCurrency_ShouldThrowException() {
        TrendsRequest request = new TrendsRequest("USD", "XYZ", "7D");
        when(currencyServiceClient.getSupportedCurrencies())
                .thenReturn(List.of("USD", "EUR", "GBP"));

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("XYZ");
    }
}
