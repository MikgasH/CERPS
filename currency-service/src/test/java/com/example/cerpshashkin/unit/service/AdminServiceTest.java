package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.dto.CreateProviderKeyRequest;
import com.example.cerpshashkin.dto.ProviderKeyResponse;
import com.example.cerpshashkin.dto.UpdateProviderKeyRequest;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.InvalidCurrencyException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.example.cerpshashkin.service.AdminService;
import com.example.cerpshashkin.service.ExchangeRateService;
import com.example.cerpshashkin.service.ProviderKeyManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private ProviderKeyManagementService providerKeyService;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void addCurrency_ShouldSaveNormalizedCode_WhenNewCurrencyPaddedLowercase() {
        when(supportedCurrencyRepository.existsByCurrencyCode("USD")).thenReturn(false);

        adminService.addCurrency(" usd ");

        ArgumentCaptor<SupportedCurrencyEntity> captor = ArgumentCaptor.forClass(SupportedCurrencyEntity.class);
        verify(supportedCurrencyRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void addCurrency_ShouldNotSaveDuplicate_WhenCurrencyAlreadyExists() {
        when(supportedCurrencyRepository.existsByCurrencyCode("USD")).thenReturn(true);

        adminService.addCurrency("USD");

        verify(supportedCurrencyRepository, never()).save(any());
    }

    @Test
    void addCurrency_ShouldThrowInvalidCurrency_WhenCodeNull() {
        assertThatThrownBy(() -> adminService.addCurrency(null))
                .isInstanceOf(InvalidCurrencyException.class);

        verifyNoInteractions(supportedCurrencyRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void addCurrency_ShouldThrowInvalidCurrency_WhenCodeBlank(final String code) {
        assertThatThrownBy(() -> adminService.addCurrency(code))
                .isInstanceOf(InvalidCurrencyException.class);

        verifyNoInteractions(supportedCurrencyRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "US", "USDX", "12$"})
    void addCurrency_ShouldThrowInvalidCurrency_WhenNotAnIsoCode(final String code) {
        assertThatThrownBy(() -> adminService.addCurrency(code))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining(code);

        verifyNoInteractions(supportedCurrencyRepository);
    }

    @Test
    void refreshExchangeRates_ShouldDelegateToExchangeRateService() {
        adminService.refreshExchangeRates();

        verify(exchangeRateService).refreshRates();
    }

    @Test
    void refreshExchangeRates_ShouldRethrow_WhenRefreshFails() {
        doThrow(new IllegalStateException("providers unavailable"))
                .when(exchangeRateService).refreshRates();

        assertThatThrownBy(() -> adminService.refreshExchangeRates())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("providers unavailable");
    }

    @Test
    void createProviderKey_ShouldDelegateAndReturnResponse() {
        CreateProviderKeyRequest request = new CreateProviderKeyRequest("Fixer.io", "secret-key");
        ProviderKeyResponse expected = keyResponse(1L);
        when(providerKeyService.createProviderKey(request)).thenReturn(expected);

        ProviderKeyResponse result = adminService.createProviderKey(request);

        assertThat(result).isEqualTo(expected);
        verify(providerKeyService).createProviderKey(request);
    }

    @Test
    void updateProviderKey_ShouldDelegateAndReturnResponse() {
        UpdateProviderKeyRequest request = new UpdateProviderKeyRequest("rotated-key");
        ProviderKeyResponse expected = keyResponse(7L);
        when(providerKeyService.updateProviderKey(7L, request)).thenReturn(expected);

        ProviderKeyResponse result = adminService.updateProviderKey(7L, request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void deleteProviderKey_ShouldDelegate() {
        adminService.deleteProviderKey(7L);

        verify(providerKeyService).deleteProviderKey(7L);
    }

    @Test
    void getAllProviderKeys_ShouldReturnActiveKeys() {
        List<ProviderKeyResponse> keys = List.of(keyResponse(1L), keyResponse(2L));
        when(providerKeyService.getAllActiveProviderKeys()).thenReturn(keys);

        assertThat(adminService.getAllProviderKeys()).isEqualTo(keys);
    }

    @Test
    void getProviderKey_ShouldReturnKeyById() {
        ProviderKeyResponse expected = keyResponse(3L);
        when(providerKeyService.getProviderKey(3L)).thenReturn(expected);

        assertThat(adminService.getProviderKey(3L)).isEqualTo(expected);
    }

    private ProviderKeyResponse keyResponse(final Long id) {
        return ProviderKeyResponse.builder()
                .id(id)
                .providerName("Fixer.io")
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
    }
}
