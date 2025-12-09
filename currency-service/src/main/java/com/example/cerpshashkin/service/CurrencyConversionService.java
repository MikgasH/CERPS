package com.example.cerpshashkin.service;

import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.exception.RateNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final String LOG_CONVERTING = "Converting {} {} to {}";
    private static final String LOG_CONVERSION_SUCCESS = "Conversion successful: {} {} = {} {} (rate: {})";
    private static final int SCALE = 6;

    private final ExchangeRateService exchangeRateService;

    public ConversionResponse convertCurrency(final ConversionRequest request) {
        log.info(LOG_CONVERTING, request.amount(), request.from(), request.to());

        final Currency fromCurrency = Currency.getInstance(request.from().toUpperCase());
        final Currency toCurrency = Currency.getInstance(request.to().toUpperCase());

        if (fromCurrency.equals(toCurrency)) {
            return createSameCurrencyResponse(request);
        }

        final BigDecimal rate = exchangeRateService
                .getExchangeRate(fromCurrency, toCurrency)
                .orElseThrow(() -> new RateNotAvailableException(request.from(), request.to()));

        return createConversionResponse(request, rate);
    }

    private ConversionResponse createSameCurrencyResponse(final ConversionRequest request) {
        return ConversionResponse.success(
                request.amount(),
                request.from(),
                request.to(),
                request.amount(),
                BigDecimal.ONE
        );
    }

    private ConversionResponse createConversionResponse(
            final ConversionRequest request,
            final BigDecimal rate) {

        final BigDecimal convertedAmount = request.amount()
                .multiply(rate)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info(LOG_CONVERSION_SUCCESS,
                request.amount(), request.from(), convertedAmount, request.to(), rate);

        return ConversionResponse.success(
                request.amount(),
                request.from(),
                request.to(),
                convertedAmount,
                rate
        );
    }
}
