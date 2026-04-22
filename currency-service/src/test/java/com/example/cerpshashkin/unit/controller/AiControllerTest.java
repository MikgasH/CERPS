package com.example.cerpshashkin.unit.controller;

import com.example.cerpshashkin.controller.AiController;
import com.example.cerpshashkin.exception.GlobalExceptionHandler;
import com.example.cerpshashkin.exception.ServiceUnavailableException;
import com.example.cerpshashkin.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private AiController aiController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bankCommission_validName_returns200() throws Exception {
        when(aiService.getBankCommission("Revolut")).thenReturn(0.5);

        mockMvc.perform(get("/api/v1/ai/bank-commission")
                        .param("bankName", "Revolut")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commission").value(0.5))
                .andExpect(jsonPath("$.found").value(true));

        verify(aiService).validateBankName("Revolut");
        verify(aiService).getBankCommission("Revolut");
    }

    @Test
    void bankCommission_blankName_returns400() throws Exception {
        doThrow(new IllegalArgumentException("bankName must not be blank"))
                .when(aiService).validateBankName("   ");

        mockMvc.perform(get("/api/v1/ai/bank-commission")
                        .param("bankName", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid argument"))
                .andExpect(jsonPath("$.detail").value("bankName must not be blank"));

        verify(aiService).validateBankName("   ");
        verify(aiService, never()).getBankCommission(anyString());
    }

    @Test
    void bankCommission_notFound_returns200WithFoundFalse() throws Exception {
        when(aiService.getBankCommission("UnknownBank")).thenReturn(null);

        mockMvc.perform(get("/api/v1/ai/bank-commission")
                        .param("bankName", "UnknownBank")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commission").value(nullValue()))
                .andExpect(jsonPath("$.found").value(false));

        verify(aiService).validateBankName("UnknownBank");
        verify(aiService).getBankCommission("UnknownBank");
    }

    @Test
    void bankCommission_geminiDown_returns503() throws Exception {
        when(aiService.getBankCommission("Revolut"))
                .thenThrow(new ServiceUnavailableException("Gemini AI service unavailable"));

        mockMvc.perform(get("/api/v1/ai/bank-commission")
                        .param("bankName", "Revolut")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service unavailable"))
                .andExpect(jsonPath("$.detail").value("Gemini AI service unavailable"));

        verify(aiService).validateBankName("Revolut");
        verify(aiService).getBankCommission(eq("Revolut"));
    }

    @Test
    void bankCommission_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/ai/bank-commission")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiService);
    }
}
