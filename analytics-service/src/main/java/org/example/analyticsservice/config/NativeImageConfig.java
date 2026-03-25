package org.example.analyticsservice.config;

import com.example.cerps.common.converter.CurrencyAttributeConverter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image reachability hints for analytics-service.
 * Most types are auto-detected by Spring AOT; this covers the
 * cross-module auto-applied JPA converter from cerps-common.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        // Auto-applied JPA converter from cerps-common — cross-module,
        // detected by Hibernate during entity manager initialization
        hints.reflection().registerType(
                CurrencyAttributeConverter.class, MemberCategory.values());
    }
}
