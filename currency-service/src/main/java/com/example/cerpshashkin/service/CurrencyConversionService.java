package com.example.cerpshashkin.service;

import com.example.cerps.common.CerpsConstants;
import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.exception.CurrencyNotSupportedException;
import com.example.cerpshashkin.exception.RateNotAvailableException;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final String LOG_CONVERTING = "Converting {} {} to {}";
    private static final String LOG_CONVERSION_SUCCESS = "Conversion successful: {} {} = {} {} (rate: {})";

    private final ExchangeRateService exchangeRateService;
    private final SupportedCurrencyRepository supportedCurrencyRepository;
    private final MeterRegistry meterRegistry;

    private Counter successCounter;
    private Counter failureCounter;
    private Timer conversionTimer;

    @PostConstruct
    public void initMetrics() {
        successCounter = Counter.builder("currency.conversions.success")
                .description("Number of successful currency conversions")
                .register(meterRegistry);

        failureCounter = Counter.builder("currency.conversions.failure")
                .description("Number of failed currency conversions")
                .register(meterRegistry);

        conversionTimer = Timer.builder("currency.conversion.time")
                .description("Time taken for currency conversion")
                .register(meterRegistry);
    }

    public ConversionResponse convertCurrency(final ConversionRequest request) {
        return conversionTimer.record(() -> {
            try {
                log.info(LOG_CONVERTING, request.amount(), request.from(), request.to());

                final String fromCode = request.from().toUpperCase();
                final String toCode = request.to().toUpperCase();

                // Single DB query: fetch all supported currencies, then validate both
                final List<String> supportedCurrencies = supportedCurrencyRepository.findAll()
                        .stream()
                        .map(SupportedCurrencyEntity::getCurrencyCode)
                        .sorted()
                        .toList();

                if (!supportedCurrencies.contains(fromCode)) {
                    throw new CurrencyNotSupportedException(fromCode, supportedCurrencies);
                }
                if (!supportedCurrencies.contains(toCode)) {
                    throw new CurrencyNotSupportedException(toCode, supportedCurrencies);
                }

                final Currency fromCurrency = Currency.getInstance(fromCode);
                final Currency toCurrency = Currency.getInstance(toCode);

                if (fromCurrency.equals(toCurrency)) {
                    successCounter.increment();
                    return createSameCurrencyResponse(request);
                }

                final BigDecimal rate = exchangeRateService
                        .getExchangeRate(fromCurrency, toCurrency)
                        .orElseThrow(() -> new RateNotAvailableException(request.from(), request.to()));

                successCounter.increment();
                return createConversionResponse(request, rate);
            } catch (Exception e) {
                failureCounter.increment();
                throw e;
            }
        });
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
                .setScale(CerpsConstants.CALCULATION_SCALE, RoundingMode.HALF_UP);

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
