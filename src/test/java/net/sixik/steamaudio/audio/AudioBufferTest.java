package net.sixik.steamaudio.audio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for the lifecycle and interleaving of {@link AudioBuffer}.
 */
class AudioBufferTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void interleavedRoundtrip() {
        try (AudioBuffer buffer = new AudioBuffer(context, 2, 4)) {
            assertTrue(buffer.isOpen());

            float[] interleaved = {1.0f, 10.0f, 2.0f, 20.0f, 3.0f, 30.0f, 4.0f, 40.0f};
            buffer.deinterleaveFrom(interleaved);

            float[] result = new float[8];
            buffer.interleaveTo(result);

            assertArrayEquals(interleaved, result, 1e-6f);
        }
    }

    @Test
    void closedBufferIsNotUsable() {
        AudioBuffer buffer = new AudioBuffer(context, 1, 8);
        buffer.close();
        assertFalse(buffer.isOpen());
        assertThrows(IllegalStateException.class, () -> buffer.interleaveTo(new float[8]));
        assertThrows(IllegalStateException.class, () -> buffer.deinterleaveFrom(new float[8]));
        assertDoesNotThrow(buffer::close);
    }

    @Test
    void tooSmallArrayRejected() {
        try (AudioBuffer buffer = new AudioBuffer(context, 2, 8)) {
            assertThrows(IllegalArgumentException.class, () -> buffer.interleaveTo(new float[15]));
            assertThrows(IllegalArgumentException.class, () -> buffer.deinterleaveFrom(new float[15]));
        }
    }

    @Test
    void closedContextRejected() {
        context.close();
        assertThrows(IllegalStateException.class, () -> new AudioBuffer(context, 1, 8));
    }
}