package com.example.cerpshashkin.controller;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerpshashkin.dto.CurrentRatesResponse;
import com.example.cerpshashkin.service.CurrencyService;
import com.example.cerpshashkin.service.RateHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Currency", description = "Public currency operations")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final RateHistoryService rateHistoryService;

    @GetMapping("/currencies")
    @Operation(summary = "Get list of supported currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        log.info("GET /api/v1/currencies");
        return ResponseEntity.ok(currencyService.getSupportedCurrencies());
    }

    @PostMapping("/currencies/convert")
    @Operation(summary = "Convert currency amount")
    public ResponseEntity<ConversionResponse> convertCurrency(
            @Valid @RequestBody final ConversionRequest request) {
        log.info("POST /api/v1/currencies/convert - {} {} to {}",
                request.amount(), request.from(), request.to());
        return ResponseEntity.ok(currencyService.convertCurrency(request));
    }

    @GetMapping("/rates/current")
    @Operation(summary = "Get current exchange rates for base currency")
    public ResponseEntity<CurrentRatesResponse> getCurrentRates(
            @RequestParam(required = false, defaultValue = "EUR") final String base) {
        final String normalizedBase = base.trim().toUpperCase();
        log.info("GET /api/v1/rates/current?base={}", normalizedBase);
        return ResponseEntity.ok(currencyService.getCurrentRatesForBase(normalizedBase));
    }

    @GetMapping("/rates/history")
    @Operation(summary = "Get exchange rate history for a currency pair")
    public ResponseEntity<RateHistoryResponse> getRateHistory(
            @RequestParam final String from,
            @RequestParam final String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant endDate) {
        log.info("GET /api/v1/rates/history from={} to={} startDate={} endDate={}", from, to, startDate, endDate);
        return ResponseEntity.ok(rateHistoryService.getRateHistory(from, to, startDate, endDate));
    }
}
