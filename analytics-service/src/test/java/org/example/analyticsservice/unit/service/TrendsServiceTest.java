package org.example.analyticsservice.unit.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.analyticsservice.entity.ExchangeRateEntity;
import org.example.analyticsservice.exception.CurrencyNotSupportedException;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.repository.ExchangeRateRepository;
import org.example.analyticsservice.repository.SupportedCurrencyRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendsServiceTest {

    @Mock
    private ExchangeRateRepository repository;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    private TrendsService service;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new TrendsService(repository, supportedCurrencyRepository, new SimpleMeterRegistry());
        service.initMetrics();

        when(supportedCurrencyRepository.existsByCurrencyCode(any())).thenReturn(true);
    }

    @Test
    void calculateTrends_WithSufficientData_ShouldReturnTrend() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        List<Object[]> mockData = List.of(
                createMockRow(now.minus(7, ChronoUnit.DAYS), "1.10"),
                createMockRow(now, "1.18")
        );
        when(repository.findRatesWithCrossSupport(any(), any(), any(), any())).thenReturn(mockData);

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.from()).isEqualTo("USD");
        assertThat(response.to()).isEqualTo("EUR");
        assertThat(response.changePercentage()).isEqualByComparingTo("7.27");
    }

    @Test
    void calculateTrends_WithInsufficientData_ShouldThrowException() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        when(repository.findRatesWithCrossSupport(any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("No exchange rate data available");
    }

    @Test
    void calculateTrends_WithPositiveChange_ShouldReturnPositivePercentage() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        List<Object[]> mockData = List.of(
                createMockRow(now.minus(1, ChronoUnit.DAYS), "1.00"),
                createMockRow(now, "1.25")
        );
        when(repository.findRatesWithCrossSupport(any(), any(), any(), any())).thenReturn(mockData);

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.changePercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void calculateTrends_WithNegativeChange_ShouldReturnNegativePercentage() {
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        List<Object[]> mockData = List.of(
                createMockRow(now.minus(1, ChronoUnit.DAYS), "2.00"),
                createMockRow(now, "1.50")
        );
        when(repository.findRatesWithCrossSupport(any(), any(), any(), any())).thenReturn(mockData);

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
        when(supportedCurrencyRepository.existsByCurrencyCode("USD")).thenReturn(true);
        when(supportedCurrencyRepository.existsByCurrencyCode("XYZ")).thenReturn(false);

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("XYZ");
    }

    private Object[] createMockRow(Instant timestamp, String rate) {
        return new Object[]{
                UUID.randomUUID(),
                "USD",
                "EUR",
                new BigDecimal(rate),
                "TEST",
                timestamp
        };
    }
}
