package com.example.cerpshashkin.unit.config;

import com.example.cerpshashkin.config.NativeImageConfig;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

class NativeImageConfigTest {

    @Test
    void registerHints_ShouldRegisterCaffeineCacheClasses_ForNativeReflection() {
        RuntimeHints hints = new RuntimeHints();

        new NativeImageConfig().registerHints(hints, getClass().getClassLoader());

        // Caffeine instantiates both classes reflectively when building the
        // historicalRates cache; without these hints the native image fails
        // at startup with ClassNotFoundException: SSMSA.
        assertConstructorHint(hints, NativeImageConfig.CAFFEINE_CACHE_CLASS);
        assertConstructorHint(hints, NativeImageConfig.CAFFEINE_NODE_CLASS);
    }

    @Test
    void caffeineHintedClasses_ShouldExistOnClasspath() throws ClassNotFoundException {
        // Guards against a Caffeine upgrade renaming or dropping the
        // generated implementation classes the hints point at.
        assertThat(Class.forName(NativeImageConfig.CAFFEINE_CACHE_CLASS)).isNotNull();
        assertThat(Class.forName(NativeImageConfig.CAFFEINE_NODE_CLASS)).isNotNull();
    }

    private void assertConstructorHint(final RuntimeHints hints, final String className) {
        TypeHint typeHint = hints.reflection().getTypeHint(TypeReference.of(className));

        assertThat(typeHint).as("reflection hint for %s", className).isNotNull();
        assertThat(typeHint.getMemberCategories())
                .contains(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
