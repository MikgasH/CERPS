package com.example.cerpshashkin.config;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import com.example.cerps.common.converter.ResponseConverter;
import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.validation.CurrencyCodeValidator;
import com.example.cerps.common.validation.PeriodValidator;
import com.example.cerpshashkin.dto.BankCommissionResponse;
import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.dto.GeminiRequest;
import com.example.cerpshashkin.dto.GeminiResponse;
import com.example.cerpshashkin.repository.RateQueryResult;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image reachability hints for currency-service.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        registerExternalApiDtos(hints);
        registerCommonDtos(hints);
        registerGeminiDtos(hints);
        registerValidators(hints);
        registerJpaHints(hints);
        registerConverters(hints);
        registerResources(hints);
    }

    private void registerExternalApiDtos(final RuntimeHints hints) {
        register(hints,
                FixerioResponse.class,
                ExchangeRatesApiResponse.class,
                CurrencyApiRawResponse.class,
                CurrencyApiRawResponse.Meta.class,
                CurrencyApiRawResponse.CurrencyData.class);
    }

    private void registerGeminiDtos(final RuntimeHints hints) {
        register(hints,
                GeminiRequest.class,
                GeminiRequest.SystemInstruction.class,
                GeminiRequest.Content.class,
                GeminiRequest.Part.class,
                GeminiResponse.class,
                GeminiResponse.Candidate.class,
                GeminiResponse.Content.class,
                GeminiResponse.Part.class,
                BankCommissionResponse.class);
    }

    private void registerCommonDtos(final RuntimeHints hints) {
        register(hints,
                ConversionRequest.class,
                ConversionResponse.class,
                RateHistoryResponse.class,
                RatePoint.class);
    }

    private void registerValidators(final RuntimeHints hints) {
        register(hints,
                CurrencyCodeValidator.class,
                PeriodValidator.class);
    }

    private void registerJpaHints(final RuntimeHints hints) {
        register(hints,
                RateQueryResult.class,
                CurrencyAttributeConverter.class);
        hints.reflection().registerType(
                java.util.UUID[].class,
                MemberCategory.UNSAFE_ALLOCATED);
    }

    private void registerConverters(final RuntimeHints hints) {
        register(hints,
                ResponseConverter.CurrencyDeserializer.class,
                ResponseConverter.TimestampToInstantDeserializer.class);
    }

    private void registerResources(final RuntimeHints hints) {
        hints.resources().registerPattern("db/changelog/*");
        hints.resources().registerPattern("db/changelog/migrations/*");
    }

    private void register(final RuntimeHints hints, final Class<?>... types) {
        for (final Class<?> type : types) {
            hints.reflection().registerType(type, MemberCategory.values());
        }
    }
}
