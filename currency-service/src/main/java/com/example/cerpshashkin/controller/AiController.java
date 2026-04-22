package com.example.cerpshashkin.controller;

import com.example.cerpshashkin.dto.BankCommissionResponse;
import com.example.cerpshashkin.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "AI", description = "AI-powered bank data lookup")
public class AiController {

    private final AiService aiService;

    @GetMapping("/bank-commission")
    @Operation(
            summary = "Look up typical foreign-currency commission for a bank",
            description = "Uses Gemini AI to estimate the typical foreign currency transaction "
                    + "commission percentage for the given bank. Returns found=false if the model "
                    + "answers NOT_FOUND. Rate limited to 10 requests per minute per IP."
    )
    @ApiResponse(responseCode = "200", description = "Lookup completed (check 'found' flag)")
    @ApiResponse(responseCode = "400", description = "Invalid bank name")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "503", description = "Gemini AI service unavailable")
    public ResponseEntity<BankCommissionResponse> getBankCommission(
            @Parameter(description = "Bank name (letters, digits, spaces, hyphens, apostrophes; max 100 chars)",
                    example = "Revolut", required = true)
            @RequestParam final String bankName) {
        log.info("GET /api/v1/ai/bank-commission?bankName={}", bankName);

        aiService.validateBankName(bankName);
        final Double commission = aiService.getBankCommission(bankName);

        if (commission == null) {
            return ResponseEntity.ok(BankCommissionResponse.notFound());
        }
        return ResponseEntity.ok(BankCommissionResponse.found(commission));
    }
}
