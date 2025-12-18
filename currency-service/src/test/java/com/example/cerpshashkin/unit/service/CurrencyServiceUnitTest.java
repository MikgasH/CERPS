package com.example.cerpshashkin.unit.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.CurrencyNotSupportedException;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.CurrencyConversionService;
import com.example.cerpshashkin.service.CurrencyService;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceUnitTest {

    @Mock
    private CurrencyConversionService conversionService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void getSupportedCurrencies_ShouldReturnCurrenciesFromRepository() {
        List<SupportedCurrencyEntity> entities = List.of(
                createEntity(1L, "USD"),
                createEntity(2L, "EUR"),
                createEntity(3L, "GBP")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(entities);

        List<String> result = currencyService.getSupportedCurrencies();

        assertThat(result)
                .containsExactly("EUR", "GBP", "USD")
                .isSorted();
        verify(supportedCurrencyRepository).findAll();
    }

    @Test
    void getSupportedCurrencies_WithEmptyRepository_ShouldReturnEmptyList() {
        when(supportedCurrencyRepository.findAll()).thenReturn(List.of());

        List<String> result = currencyService.getSupportedCurrencies();

        assertThat(result).isEmpty();
        verify(supportedCurrencyRepository).findAll();
    }

    @Test
    void addCurrency_WithValidCurrency_ShouldSaveToRepository() {
        String currency = "NOK";

        when(supportedCurrencyRepository.existsByCurrencyCode("NOK")).thenReturn(false);
        when(supportedCurrencyRepository.save(any(SupportedCurrencyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        currencyService.addCurrency(currency);

        verify(supportedCurrencyRepository).existsByCurrencyCode("NOK");
        verify(supportedCurrencyRepository).save(any(SupportedCurrencyEntity.class));
    }

    @Test
    void addCurrency_WithLowercaseCurrency_ShouldNormalizeToUppercase() {
        String currency = "nok";

        when(supportedCurrencyRepository.existsByCurrencyCode("NOK")).thenReturn(false);
        when(supportedCurrencyRepository.save(any(SupportedCurrencyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        currencyService.addCurrency(currency);

        verify(supportedCurrencyRepository).existsByCurrencyCode("NOK");
        verify(supportedCurrencyRepository).save(any(SupportedCurrencyEntity.class));
    }

    @Test
    void addCurrency_WithWhitespaceCurrency_ShouldTrimWhitespace() {
        String currency = "  SEK  ";

        when(supportedCurrencyRepository.existsByCurrencyCode("SEK")).thenReturn(false);
        when(supportedCurrencyRepository.save(any(SupportedCurrencyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        currencyService.addCurrency(currency);

        verify(supportedCurrencyRepository).existsByCurrencyCode("SEK");
        verify(supportedCurrencyRepository).save(any(SupportedCurrencyEntity.class));
    }

    @Test
    void addCurrency_WithNullCurrency_ShouldThrowInvalidCurrencyException() {
        assertThatThrownBy(() -> currencyService.addCurrency(null))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency code cannot be null or empty");
    }

    @Test
    void addCurrency_WithEmptyCurrency_ShouldThrowInvalidCurrencyException() {
        assertThatThrownBy(() -> currencyService.addCurrency(""))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency code cannot be null or empty");
    }

    @Test
    void addCurrency_WithWhitespaceOnlyCurrency_ShouldThrowInvalidCurrencyException() {
        assertThatThrownBy(() -> currencyService.addCurrency("   "))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency code cannot be null or empty");
    }

    @Test
    void addCurrency_WithInvalidCurrency_ShouldThrowInvalidCurrencyException() {
        assertThatThrownBy(() -> currencyService.addCurrency("INVALID"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Invalid currency code: INVALID");
    }

    @Test
    void addCurrency_WithDuplicateCurrency_ShouldNotAddDuplicate() {
        String currency = "NOK";

        when(supportedCurrencyRepository.existsByCurrencyCode("NOK")).thenReturn(true);

        currencyService.addCurrency(currency);

        verify(supportedCurrencyRepository).existsByCurrencyCode("NOK");
        verify(supportedCurrencyRepository, times(0)).save(any());
    }

    @Test
    void convertCurrency_ShouldDelegateToConversionService() {
        ConversionRequest request = ConversionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .from("USD")
                .to("EUR")
                .build();

        ConversionResponse expectedResponse = ConversionResponse.success(
                BigDecimal.valueOf(100),
                "USD",
                "EUR",
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(0.85)
        );

        when(conversionService.convertCurrency(request)).thenReturn(expectedResponse);

        ConversionResponse result = currencyService.convertCurrency(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(conversionService, times(1)).convertCurrency(request);
    }

    @Test
    void refreshExchangeRates_ShouldDelegateToExchangeRateService() {
        doNothing().when(exchangeRateService).refreshRates();

        currencyService.refreshExchangeRates();

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    void validateSupportedCurrencies_WithBothSupported_ShouldNotThrow() {
        List<SupportedCurrencyEntity> entities = List.of(
                createEntity(1L, "USD"),
                createEntity(2L, "EUR")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(entities);

        currencyService.validateSupportedCurrencies("USD", "EUR");

        verify(supportedCurrencyRepository).findAll();
    }

    @Test
    void validateSupportedCurrencies_WithFromNotSupported_ShouldThrowException() {
        List<SupportedCurrencyEntity> entities = List.of(
                createEntity(1L, "EUR"),
                createEntity(2L, "GBP")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(entities);

        assertThatThrownBy(() -> currencyService.validateSupportedCurrencies("USD", "EUR"))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("Currency 'USD' is not supported");
    }

    @Test
    void validateSupportedCurrencies_WithToNotSupported_ShouldThrowException() {
        List<SupportedCurrencyEntity> entities = List.of(
                createEntity(1L, "USD"),
                createEntity(2L, "EUR")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(entities);

        assertThatThrownBy(() -> currencyService.validateSupportedCurrencies("USD", "GBP"))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("Currency 'GBP' is not supported");
    }

    @Test
    void validateSupportedCurrencies_WithLowercaseInput_ShouldNormalizeAndValidate() {
        List<SupportedCurrencyEntity> entities = List.of(
                createEntity(1L, "USD"),
                createEntity(2L, "EUR")
        );

        when(supportedCurrencyRepository.findAll()).thenReturn(entities);

        currencyService.validateSupportedCurrencies("usd", "eur");

        verify(supportedCurrencyRepository).findAll();
    }

    private SupportedCurrencyEntity createEntity(Long id, String code) {
        return SupportedCurrencyEntity.builder()
                .id(id)
                .currencyCode(code)
                .build();
    }
}
