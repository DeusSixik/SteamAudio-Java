package net.sixik.steamaudio.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the automatic SIMD level selection used by
 * {@link Context#SIMD_LEVEL_AUTO}.
 */
class SimdLevelTest {

    private String original;

    @BeforeEach
    void rememberProperty() {
        original = System.getProperty(Context.SIMD_LEVEL_PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (original == null) {
            System.clearProperty(Context.SIMD_LEVEL_PROPERTY);
        } else {
            System.setProperty(Context.SIMD_LEVEL_PROPERTY, original);
        }
    }

    @Test
    void defaultResolutionIsAvx() {
        System.clearProperty(Context.SIMD_LEVEL_PROPERTY);
        assertEquals(Context.SIMD_LEVEL_AVX, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));
    }

    @Test
    void namedPropertiesResolveToLevels() {
        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "sse2");
        assertEquals(Context.SIMD_LEVEL_SSE2, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));

        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "AVX");
        assertEquals(Context.SIMD_LEVEL_AVX, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));

        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "avx512");
        assertEquals(Context.SIMD_LEVEL_AVX512, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));
    }

    @Test
    void integerPropertiesResolveToLevels() {
        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "0");
        assertEquals(Context.SIMD_LEVEL_SSE2, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));

        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "3");
        assertEquals(Context.SIMD_LEVEL_AVX2, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));
    }

    @Test
    void invalidPropertiesFallBackToAvx() {
        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "nonsense");
        assertEquals(Context.SIMD_LEVEL_AVX, Context.resolveSimdLevel(Context.SIMD_LEVEL_AUTO));
    }

    @Test
    void explicitLevelsBypassTheProperty() {
        // An explicit request is never overridden by the property.
        System.setProperty(Context.SIMD_LEVEL_PROPERTY, "sse2");
        assertEquals(Context.SIMD_LEVEL_AVX2, Context.resolveSimdLevel(Context.SIMD_LEVEL_AVX2));
    }
}
