package org.example.analyticsservice.unit.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.analyticsservice.client.CurrencyServiceClient;
import org.example.analyticsservice.exception.InsufficientDataException;
import org.example.analyticsservice.service.TrendsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendsServiceDownsampleTest {

    @Mock
    private CurrencyServiceClient currencyServiceClient;

    private TrendsService service;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new TrendsService(currencyServiceClient, new SimpleMeterRegistry());
        service.initMetrics();
    }

    private void stubSupportedCurrencies() {
        when(currencyServiceClient.getSupportedCurrencies())
                .thenReturn(List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD"));
    }

    // === Downsampling static method tests ===

    @SuppressWarnings("unchecked")
    private <T> List<T> invokeDownsample(List<T> data, int maxPoints) {
        return ReflectionTestUtils.invokeMethod(service, "downsample", data, maxPoints);
    }

    private int invokeGetMaxPointsForPeriod(String period) {
        return ReflectionTestUtils.invokeMethod(service, "getMaxPointsForPeriod", period);
    }

    @Test
    void downsample_WithExactMaxPoints_ShouldReturnSameData() {
        List<String> data = List.of("A", "B", "C", "D", "E");

        List<String> result = invokeDownsample(data, 5);

        assertThat(result).isEqualTo(data);
        assertThat(result).hasSize(5);
    }

    @Test
    void downsample_WithFewerThanMaxPoints_ShouldReturnSameData() {
        List<String> data = List.of("A", "B", "C");

        List<String> result = invokeDownsample(data, 10);

        assertThat(result).isEqualTo(data);
        assertThat(result).hasSize(3);
    }

    @Test
    void downsample_WithMoreThanMaxPoints_ShouldReduceToMaxPoints() {
        List<Integer> data = IntStream.range(0, 100).boxed().toList();

        List<Integer> result = invokeDownsample(data, 10);

        assertThat(result).hasSize(10);
    }

    @Test
    void downsample_ShouldPreserveFirstAndLastElements() {
        List<Integer> data = IntStream.range(0, 100).boxed().toList();

        List<Integer> result = invokeDownsample(data, 10);

        assertThat(result.getFirst()).isEqualTo(0);
        assertThat(result.getLast()).isEqualTo(99);
    }

    @Test
    void downsample_WithLargeDataset_ShouldDistributeEvenly() {
        List<Integer> data = IntStream.range(0, 1000).boxed().toList();

        List<Integer> result = invokeDownsample(data, 5);

        assertThat(result).hasSize(5);
        assertThat(result.getFirst()).isEqualTo(0);
        assertThat(result.getLast()).isEqualTo(999);
        // Inner points should be spaced evenly
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i)).isGreaterThan(result.get(i - 1));
        }
    }

    @Test
    void downsample_WithTwoMaxPoints_ShouldReturnFirstAndLast() {
        List<Integer> data = IntStream.range(0, 50).boxed().toList();

        List<Integer> result = invokeDownsample(data, 2);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isEqualTo(0);
        assertThat(result.getLast()).isEqualTo(49);
    }

    // === getMaxPointsForPeriod tests ===

    @Test
    void getMaxPointsForPeriod_1D_ShouldReturn24() {
        assertThat(invokeGetMaxPointsForPeriod("1D")).isEqualTo(CerpsConstants.MAX_POINTS_1D);
    }

    @Test
    void getMaxPointsForPeriod_7D_ShouldReturn56() {
        assertThat(invokeGetMaxPointsForPeriod("7D")).isEqualTo(CerpsConstants.MAX_POINTS_7D);
    }

    @Test
    void getMaxPointsForPeriod_30D_ShouldReturn90() {
        assertThat(invokeGetMaxPointsForPeriod("30D")).isEqualTo(CerpsConstants.MAX_POINTS_30D);
    }

    @Test
    void getMaxPointsForPeriod_90D_ShouldReturn180() {
        assertThat(invokeGetMaxPointsForPeriod("90D")).isEqualTo(CerpsConstants.MAX_POINTS_90D);
    }

    @Test
    void getMaxPointsForPeriod_180D_ShouldReturn270() {
        assertThat(invokeGetMaxPointsForPeriod("180D")).isEqualTo(CerpsConstants.MAX_POINTS_180D);
    }

    @Test
    void getMaxPointsForPeriod_UnknownPeriod_ShouldReturnDefault() {
        assertThat(invokeGetMaxPointsForPeriod("1Y")).isEqualTo(CerpsConstants.MAX_POINTS_180D);
    }

    @Test
    void getMaxPointsForPeriod_CaseInsensitive_ShouldWork() {
        assertThat(invokeGetMaxPointsForPeriod("  7d  ")).isEqualTo(CerpsConstants.MAX_POINTS_7D);
    }

    // === Period calculation tests ===

    @Test
    void calculateTrends_WithPeriod1D_ShouldQueryLastDay() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(12, ChronoUnit.HOURS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.12"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response).isNotNull();
        assertThat(response.from()).isEqualTo("USD");
        assertThat(response.to()).isEqualTo("EUR");
        assertThat(response.period()).isEqualTo("1D");
    }

    @Test
    void calculateTrends_WithPeriod7D_ShouldReturnCorrectPeriod() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(7, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.18"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.period()).isEqualTo("7D");
        assertThat(response.dataPoints()).isEqualTo(2);
    }

    @Test
    void calculateTrends_WithPeriod30D_ShouldReturnCorrectPeriod() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "30D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(30, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.12"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.period()).isEqualTo("30D");
    }

    @Test
    void calculateTrends_WithPeriod90D_ShouldWork() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "90D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(90, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.15"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.period()).isEqualTo("90D");
    }

    @Test
    void calculateTrends_WithPeriod180D_ShouldWork() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "180D");
        List<RatePoint> points = List.of(
                new RatePoint(now.minus(180, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now, new BigDecimal("1.20"))
        );
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", points));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.period()).isEqualTo("180D");
    }

    // === Fallback behavior with fewer than 2 data points ===

    @Test
    void calculateTrends_WithSingleDataPoint_ShouldReturnZeroChange() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        List<RatePoint> singlePoint = List.of(new RatePoint(now, new BigDecimal("1.18")));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", singlePoint));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.changePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.oldRate()).isEqualByComparingTo(response.newRate());
        assertThat(response.dataPoints()).isEqualTo(1);
    }

    @Test
    void calculateTrends_WithNoData_ShouldThrowInsufficientDataException() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", List.of()));

        assertThatThrownBy(() -> service.calculateTrends(request))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("No exchange rate data available");
    }

    @Test
    void calculateTrends_WithFewerThan2Points_ShouldFallbackToAllData() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");

        // First call (with date range) returns 1 point
        List<RatePoint> singlePoint = List.of(new RatePoint(now, new BigDecimal("1.18")));
        // Second call (all data, null dates) returns 3 points
        List<RatePoint> allData = List.of(
                new RatePoint(now.minus(30, ChronoUnit.DAYS), new BigDecimal("1.10")),
                new RatePoint(now.minus(15, ChronoUnit.DAYS), new BigDecimal("1.14")),
                new RatePoint(now, new BigDecimal("1.18"))
        );

        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(Instant.class), any(Instant.class)))
                .thenReturn(new RateHistoryResponse("USD", "EUR", singlePoint));
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), eq(null), eq(null)))
                .thenReturn(new RateHistoryResponse("USD", "EUR", allData));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.dataPoints()).isEqualTo(3);
        assertThat(response.changePercentage()).isNotEqualByComparingTo(BigDecimal.ZERO);
    }

    // === Downsampling integration with calculateTrends ===

    @Test
    void calculateTrends_WithMorePointsThanMax_ShouldDownsample() {
        stubSupportedCurrencies();
        TrendsRequest request = new TrendsRequest("USD", "EUR", "1D");
        // Generate more than MAX_POINTS_1D (24) data points
        List<RatePoint> ratePoints = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ratePoints.add(new RatePoint(
                    now.minus(50 - i, ChronoUnit.HOURS),
                    new BigDecimal(String.valueOf(1.10 + i * 0.001))
            ));
        }
        when(currencyServiceClient.getRateHistory(eq("USD"), eq("EUR"), any(), any()))
                .thenReturn(new RateHistoryResponse("USD", "EUR", ratePoints));

        TrendsResponse response = service.calculateTrends(request);

        assertThat(response.points()).hasSizeLessThanOrEqualTo(CerpsConstants.MAX_POINTS_1D);
        assertThat(response.dataPoints()).isEqualTo(50);
    }
}
