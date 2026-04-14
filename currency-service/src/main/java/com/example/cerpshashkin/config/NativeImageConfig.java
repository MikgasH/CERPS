package com.example.cerpshashkin.config;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import com.example.cerps.common.converter.ResponseConverter;
import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.repository.RateQueryResult;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image reachability hints for types that Spring AOT
 * cannot auto-detect (deserialized via RestClient, cross-module converters,
 * interface projections, and classpath resources).
 * Liquibase hints are covered by META-INF/native-image/reflect-config.json.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        registerExternalApiDtos(hints);
        registerJacksonDeserializers(hints);
        registerJpaHints(hints);
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

    private void registerJacksonDeserializers(final RuntimeHints hints) {
        register(hints,
                ResponseConverter.CurrencyDeserializer.class,
                ResponseConverter.TimestampToInstantDeserializer.class);
    }

    private void registerJpaHints(final RuntimeHints hints) {
        register(hints, RateQueryResult.class);
        register(hints, CurrencyAttributeConverter.class);
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
