package com.example.cerpshashkin.config;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import com.example.cerps.common.converter.ResponseConverter;
import com.example.cerps.common.dto.ConversionRequest;
import com.example.cerps.common.dto.ConversionResponse;
import com.example.cerps.common.dto.FrankfurterRateEntry;
import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerps.common.validation.CurrencyCodeValidator;
import com.example.cerps.common.validation.PeriodValidator;
import com.example.cerpshashkin.dto.BankCommissionResponse;
import com.example.cerpshashkin.dto.CurrencyApiRawResponse;
import com.example.cerpshashkin.dto.ExchangeRatesApiResponse;
import com.example.cerpshashkin.dto.FixerioResponse;
import com.example.cerpshashkin.dto.GeminiRequest;
import com.example.cerpshashkin.dto.HistoricalRatesResponse;
import com.example.cerpshashkin.dto.GeminiResponse;
import com.example.cerpshashkin.repository.RateQueryResult;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM Native Image reachability hints for currency-service.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    // Caffeine cache implementation classes are chosen reflectively at
    // runtime (LocalCacheFactory/NodeFactory Class.forName by a name encoding
    // the builder options). The historicalRates cache combines maximumSize
    // with a variable Expiry - a combination the shipped GraalVM reachability
    // metadata for caffeine does NOT cover (it lists SSMS/SSMSW/..., not
    // SSMSA) - so its classes must be registered here or the native image
    // dies at startup with ClassNotFoundException: SSMSA. CacheConfigTest
    // pins these names to the cache actually built, so a Caffeine upgrade or
    // builder-option change that invalidates them fails the JVM test run.
    public static final String CAFFEINE_CACHE_CLASS = "com.github.benmanes.caffeine.cache.SSMSA";
    // Variable-expiry nodes reuse the write-time node layout, hence PSWMS
    // (not PSAMS); the shipped metadata happens to cover PSWMS conditionally,
    // but it is registered here anyway so the cache's needs are explicit and
    // not dependent on the metadata repository being applied.
    public static final String CAFFEINE_NODE_CLASS = "com.github.benmanes.caffeine.cache.PSWMS";

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        registerExternalApiDtos(hints);
        registerCommonDtos(hints);
        registerGeminiDtos(hints);
        registerValidators(hints);
        registerJpaHints(hints);
        registerConverters(hints);
        registerCaffeineCacheHints(hints);
        registerResources(hints);
    }

    private void registerCaffeineCacheHints(final RuntimeHints hints) {
        // Constructor-only registration, mirroring the entries the official
        // reachability metadata uses for the covered combinations: the cache
        // class is instantiated via (Caffeine, AsyncCacheLoader, boolean),
        // the node prototype via its no-arg constructor; field access in the
        // generated classes goes through Unsafe offsets and is covered by the
        // metadata's conditional entries once the types are reachable.
        hints.reflection().registerType(
                TypeReference.of(CAFFEINE_CACHE_CLASS),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        hints.reflection().registerType(
                TypeReference.of(CAFFEINE_NODE_CLASS),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }

    private void registerExternalApiDtos(final RuntimeHints hints) {
        register(hints,
                FixerioResponse.class,
                FrankfurterRateEntry.class,
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
                HistoricalRatesResponse.class,
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
