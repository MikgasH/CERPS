package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.client.impl.FrankfurterClient;
import com.example.cerpshashkin.dto.HistoricalRatesResponse;
import com.example.cerpshashkin.entity.ExchangeRateEntity;
import com.example.cerpshashkin.exception.HistoricalRatesNotFoundException;
import com.example.cerpshashkin.model.CurrencyExchangeResponse;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import com.example.cerpshashkin.service.HistoricalRateService;
import com.example.cerpshashkin.service.SupportedCurrenciesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalRateServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency PLN = Currency.getInstance("PLN");
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 1, 15);
    private static final Set<String> SUPPORTED = Set.of("EUR", "USD", "PLN");

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private FrankfurterClient frankfurterClient;

    @Mock
    private SupportedCurrenciesService supportedCurrenciesService;

    @InjectMocks
    private HistoricalRateService historicalRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(historicalRateService, "baseCurrencyCode", "EUR");
    }

    @Test
    void getHistoricalRates_ShouldReturnDatabaseRates_WhenSnapshotExists() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        Instant snapshotTime = TEST_DATE.atTime(8, 0).toInstant(ZoneOffset.UTC);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of(
                        entity(USD, "1.0834", snapshotTime),
                        entity(PLN, "4.2891", snapshotTime)
                ));

        HistoricalRatesResponse result = historicalRateService.getHistoricalRates("EUR", TEST_DATE);

        assertThat(result.baseCurrency()).isEqualTo("EUR");
        assertThat(result.date()).isEqualTo(TEST_DATE);
        assertThat(result.source()).isEqualTo("DATABASE");
        assertThat(result.timestamp()).isEqualTo(snapshotTime);
        assertThat(result.rates())
                .containsEntry("USD", new BigDecimal("1.0834"))
                .containsEntry("PLN", new BigDecimal("4.2891"));
        verifyNoInteractions(frankfurterClient);
    }

    @Test
    void getHistoricalRates_ShouldQueryLookBackWindow_OfFourDays() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        Instant weekendSnapshot = TEST_DATE.minusDays(2).atTime(16, 0).toInstant(ZoneOffset.UTC);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of(entity(USD, "1.08", weekendSnapshot)));

        HistoricalRatesResponse result = historicalRateService.getHistoricalRates("EUR", TEST_DATE);

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(exchangeRateRepository)
                .findLatestPerTargetInWindow(eq("EUR"), startCaptor.capture(), endCaptor.capture());

        assertThat(startCaptor.getValue())
                .isEqualTo(TEST_DATE.minusDays(4).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(endCaptor.getValue())
                .isEqualTo(TEST_DATE.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        // The actual (older) snapshot timestamp is reported, not the requested date
        assertThat(result.timestamp()).isEqualTo(weekendSnapshot);
        assertThat(result.source()).isEqualTo("DATABASE");
    }

    @Test
    void getHistoricalRates_ShouldFallBackToFrankfurter_WhenDatabaseEmpty() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of());
        when(frankfurterClient.getHistoricalRates(eq(TEST_DATE), anySet()))
                .thenReturn(CurrencyExchangeResponse.success(
                        EUR, TEST_DATE,
                        Map.of(USD, new BigDecimal("1.0834"), PLN, new BigDecimal("4.2891")),
                        false));

        HistoricalRatesResponse result = historicalRateService.getHistoricalRates("EUR", TEST_DATE);

        assertThat(result.source()).isEqualTo("FRANKFURTER");
        assertThat(result.rates())
                .containsEntry("USD", new BigDecimal("1.0834"))
                .containsEntry("PLN", new BigDecimal("4.2891"));
    }

    @Test
    void getHistoricalRates_ShouldOmitUnsupportedCurrencies_FromFrankfurterResponse() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(Set.of("EUR", "USD"));
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of());
        when(frankfurterClient.getHistoricalRates(eq(TEST_DATE), anySet()))
                .thenReturn(CurrencyExchangeResponse.success(
                        EUR, TEST_DATE,
                        Map.of(USD, new BigDecimal("1.0834"), PLN, new BigDecimal("4.2891")),
                        false));

        HistoricalRatesResponse result = historicalRateService.getHistoricalRates("EUR", TEST_DATE);

        assertThat(result.rates()).containsOnlyKeys("USD");
    }

    @Test
    void getHistoricalRates_ShouldThrowBadRequest_WhenDateInFuture() {
        LocalDate future = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> historicalRateService.getHistoricalRates("EUR", future))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");

        verifyNoInteractions(exchangeRateRepository, frankfurterClient);
    }

    @Test
    void getHistoricalRates_ShouldThrowBadRequest_WhenDateBeforeEurozoneStart() {
        LocalDate tooOld = LocalDate.of(1998, 12, 31);

        assertThatThrownBy(() -> historicalRateService.getHistoricalRates("EUR", tooOld))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1999-01-04");

        verifyNoInteractions(exchangeRateRepository, frankfurterClient);
    }

    @Test
    void getHistoricalRates_ShouldThrowNotFound_WhenNeitherSourceHasData() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of());
        when(frankfurterClient.getHistoricalRates(eq(TEST_DATE), anySet()))
                .thenReturn(CurrencyExchangeResponse.success(EUR, TEST_DATE, Map.of(), false));

        assertThatThrownBy(() -> historicalRateService.getHistoricalRates("EUR", TEST_DATE))
                .isInstanceOf(HistoricalRatesNotFoundException.class)
                .hasMessageContaining(TEST_DATE.toString());
    }

    @Test
    void getHistoricalRates_WithNonEurBase_ShouldComputeCrossRates() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        Instant snapshotTime = TEST_DATE.atTime(8, 0).toInstant(ZoneOffset.UTC);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of(
                        entity(USD, "1.20", snapshotTime),
                        entity(PLN, "4.80", snapshotTime)
                ));

        HistoricalRatesResponse result = historicalRateService.getHistoricalRates("USD", TEST_DATE);

        assertThat(result.baseCurrency()).isEqualTo("USD");
        assertThat(result.rates().get("EUR"))
                .isEqualByComparingTo(new BigDecimal("0.833333"));
        assertThat(result.rates().get("PLN"))
                .isEqualByComparingTo(new BigDecimal("4.000000"));
        assertThat(result.rates()).doesNotContainKey("USD");
    }

    @Test
    void getHistoricalRates_WithNonEurBase_ShouldThrowNotFound_WhenBaseRateMissing() {
        when(supportedCurrenciesService.getSupportedCurrencyCodesAsSet()).thenReturn(SUPPORTED);
        Instant snapshotTime = TEST_DATE.atTime(8, 0).toInstant(ZoneOffset.UTC);
        when(exchangeRateRepository.findLatestPerTargetInWindow(eq("EUR"), any(), any()))
                .thenReturn(List.of(entity(PLN, "4.80", snapshotTime)));

        assertThatThrownBy(() -> historicalRateService.getHistoricalRates("USD", TEST_DATE))
                .isInstanceOf(HistoricalRatesNotFoundException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void getHistoricalRates_ShouldThrowBadRequest_WhenBaseCurrencyInvalid() {
        assertThatThrownBy(() -> historicalRateService.getHistoricalRates("XXX1", TEST_DATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ExchangeRateEntity entity(final Currency target, final String rate, final Instant timestamp) {
        return ExchangeRateEntity.builder()
                .id(UUID.randomUUID())
                .baseCurrency(EUR)
                .targetCurrency(target)
                .rate(new BigDecimal(rate))
                .source("AGGREGATED")
                .timestamp(timestamp)
                .build();
    }
}
