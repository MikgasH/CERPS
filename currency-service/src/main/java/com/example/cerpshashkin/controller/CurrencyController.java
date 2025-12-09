package com.example.cerpshashkin.controller;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerps.common.validation.ValidCurrency;
import com.example.cerpshashkin.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Currency", description = "Currency exchange operations")
public class CurrencyController {

    private static final String LOG_GET_CURRENCIES = "GET /api/v1/currencies - getting supported currencies";
    private static final String LOG_RETURN_CURRENCIES = "Returning {} supported currencies";
    private static final String LOG_ADD_CURRENCY = "POST /api/v1/currencies - adding currency: {}";
    private static final String LOG_ADDED_SUCCESSFULLY = "Currency added: {}";
    private static final String LOG_CONVERSION_REQUEST = "POST /api/v1/currencies/convert - converting {} {} to {}";
    private static final String LOG_CONVERSION_SUCCESS = "Conversion successful: {} {} = {} {}";
    private static final String LOG_REFRESH_RATES = "POST /api/v1/currencies/refresh - refreshing exchange rates";
    private static final String LOG_RATES_REFRESHED = "Exchange rates refreshed successfully";

    private static final String MESSAGE_CURRENCY_ADDED = "Currency %s added successfully";
    private static final String MESSAGE_RATES_UPDATED = "Exchange rates updated successfully";
    private static final String MESSAGE_CURRENCY_PARAM_REQUIRED = "Currency parameter is required";

    private final CurrencyService currencyService;

    @GetMapping
    @Operation(
            summary = "Get list of supported currencies",
            description = "Returns all currencies available for exchange rate operations. "
                    + "Available for: USER, PREMIUM_USER, ADMIN"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<String>> getCurrencies() {
        log.info(LOG_GET_CURRENCIES);
        final List<String> currencies = currencyService.getSupportedCurrencies();
        log.info(LOG_RETURN_CURRENCIES, currencies.size());
        return ResponseEntity.ok(currencies);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add a new currency (ADMIN only)",
            description = "Adds a new currency to the list of supported currencies for exchange rate tracking. "
                    + "Only administrators can add new currencies. Available for: ADMIN only"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<String> addCurrency(
            @RequestParam
            @NotBlank(message = MESSAGE_CURRENCY_PARAM_REQUIRED)
            @ValidCurrency
            final String currency) {

        log.info(LOG_ADD_CURRENCY, currency);
        currencyService.addCurrency(currency);

        final String message = String.format(MESSAGE_CURRENCY_ADDED, currency.toUpperCase());
        log.info(LOG_ADDED_SUCCESSFULLY, currency);

        return ResponseEntity.ok(message);
    }

    @PostMapping("/convert")
    @Operation(
            summary = "Convert currency amount",
            description = "Converts an amount from one currency to another using current exchange rates. "
                    + "Available for: USER, PREMIUM_USER, ADMIN"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ConversionResponse> convertCurrency(
            @Valid @RequestBody final ConversionRequest request) {

        log.info(LOG_CONVERSION_REQUEST, request.amount(), request.from(), request.to());

        final ConversionResponse response = currencyService.convertCurrency(request);

        log.info(LOG_CONVERSION_SUCCESS, request.amount(), request.from(),
                response.convertedAmount(), request.to());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Refresh exchange rates (ADMIN only)",
            description = "Manually triggers an update of exchange rates from external providers. "
                    + "Available for: ADMIN only"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<String> refreshRates() {
        log.info(LOG_REFRESH_RATES);
        currencyService.refreshExchangeRates();
        log.info(LOG_RATES_REFRESHED);
        return ResponseEntity.ok(MESSAGE_RATES_UPDATED);
    }
}
