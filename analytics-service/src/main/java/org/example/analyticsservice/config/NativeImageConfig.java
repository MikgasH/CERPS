package org.example.analyticsservice.config;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image reachability hints for analytics-service.
 * Registers DTOs deserialized from currency-service REST responses.
 */
public class NativeImageConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerType(RateHistoryResponse.class, MemberCategory.values());
        hints.reflection().registerType(RatePoint.class, MemberCategory.values());
    }
}
