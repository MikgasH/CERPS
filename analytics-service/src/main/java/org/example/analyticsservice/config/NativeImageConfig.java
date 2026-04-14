package org.example.analyticsservice.config;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import com.example.cerps.common.converter.ResponseConverter;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import com.example.cerps.common.validation.CurrencyCodeValidator;
import com.example.cerps.common.validation.PeriodValidator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image reachability hints for analytics-service.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        registerDtos(hints);
        registerValidators(hints);
        registerConverters(hints);
    }

    private void registerDtos(final RuntimeHints hints) {
        register(hints,
                RateHistoryResponse.class,
                RatePoint.class,
                TrendsRequest.class,
                TrendsResponse.class);
    }

    private void registerValidators(final RuntimeHints hints) {
        register(hints,
                PeriodValidator.class,
                CurrencyCodeValidator.class);
    }

    private void registerConverters(final RuntimeHints hints) {
        register(hints,
                ResponseConverter.CurrencyDeserializer.class,
                ResponseConverter.TimestampToInstantDeserializer.class,
                CurrencyAttributeConverter.class);
    }

    private void register(final RuntimeHints hints, final Class<?>... types) {
        for (final Class<?> type : types) {
            hints.reflection().registerType(type, MemberCategory.values());
        }
    }
}
