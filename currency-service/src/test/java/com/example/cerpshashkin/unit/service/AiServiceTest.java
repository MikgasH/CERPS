package com.example.cerpshashkin.unit.service;

import com.example.cerpshashkin.client.GeminiClient;
import com.example.cerpshashkin.config.GeminiProperties;
import com.example.cerpshashkin.exception.GeminiApiException;
import com.example.cerpshashkin.exception.ServiceUnavailableException;
import com.example.cerpshashkin.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    private static final String SYSTEM_PROMPT = "system prompt";
    private static final String USER_PROMPT_TEMPLATE = "commission for {bankName}";

    @Mock
    private GeminiClient geminiClient;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        final GeminiProperties properties = new GeminiProperties(
                "test-api-key",
                "http://gemini.local/",
                "test-model",
                Map.of(
                        "system", SYSTEM_PROMPT,
                        "bank-commission", USER_PROMPT_TEMPLATE
                )
        );
        aiService = new AiService(geminiClient, properties);
    }

    @Test
    void validateBankName_blank_throwsException() {
        assertThatThrownBy(() -> aiService.validateBankName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> aiService.validateBankName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> aiService.validateBankName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void validateBankName_tooLong_throwsException() {
        final String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> aiService.validateBankName(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    @Test
    void validateBankName_invalidChars_throwsException() {
        assertThatThrownBy(() -> aiService.validateBankName("Bank@HQ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letters");

        assertThatThrownBy(() -> aiService.validateBankName("Bank!"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> aiService.validateBankName("Bank_Name"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> aiService.validateBankName("Bank.Name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateBankName_valid_noException() {
        aiService.validateBankName("Revolut");
        aiService.validateBankName("HSBC UK");
        aiService.validateBankName("Barclays-UK");
        aiService.validateBankName("O'Reilly Bank");
        aiService.validateBankName("Crédit Agricole");
        aiService.validateBankName("Bank123");
        aiService.validateBankName("a".repeat(100));
    }

    @Test
    void getBankCommission_returnsNumber_success() {
        when(geminiClient.generate(eq(SYSTEM_PROMPT), anyString())).thenReturn("0.5");

        final Double result = aiService.getBankCommission("Revolut");

        assertThat(result).isEqualTo(0.5);

        final ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generate(eq(SYSTEM_PROMPT), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).isEqualTo("commission for Revolut");
    }

    @Test
    void getBankCommission_returnsNumberWithSuffix_extractsFirstNumber() {
        when(geminiClient.generate(anyString(), anyString())).thenReturn("The answer is 3.0%");

        final Double result = aiService.getBankCommission("SomeBank");

        assertThat(result).isEqualTo(3.0);
    }

    @Test
    void getBankCommission_returnsNOT_FOUND_returnsNull() {
        when(geminiClient.generate(anyString(), anyString())).thenReturn("NOT_FOUND");

        final Double result = aiService.getBankCommission("UnknownBank");

        assertThat(result).isNull();
    }

    @Test
    void getBankCommission_geminiUnavailable_throwsServiceUnavailableException() {
        when(geminiClient.generate(anyString(), anyString()))
                .thenThrow(new GeminiApiException("Gemini unreachable or timed out"));

        assertThatThrownBy(() -> aiService.getBankCommission("Revolut"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Gemini")
                .hasCauseInstanceOf(GeminiApiException.class);
    }

    @Test
    void getBankCommission_noNumberAndNoMarker_returnsNull() {
        when(geminiClient.generate(anyString(), anyString())).thenReturn("nonsense text");

        final Double result = aiService.getBankCommission("Revolut");

        assertThat(result).isNull();
        verify(geminiClient).generate(anyString(), any());
    }
}
