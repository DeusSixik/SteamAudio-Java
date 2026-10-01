package net.sixik.steamaudio.audio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the P2 {@link AudioBuffer} operations: mix, downmix and
 * Ambisonics format conversion.
 */
class P2AudioBufferTest {

    private static final int FRAME_SIZE = 8;

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static float[] sine(int numSamples) {
        float[] input = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 48000.0);
        }
        return input;
    }

    @Test
    void mixAccumulatesTwoBuffers() {
        try (AudioBuffer a = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer b = new AudioBuffer(context, 1, FRAME_SIZE)) {

            float[] ones = new float[FRAME_SIZE];
            java.util.Arrays.fill(ones, 1.0f);
            a.deinterleaveFrom(ones);

            b.deinterleaveFrom(sine(FRAME_SIZE));

            a.mix(b);

            float[] result = new float[FRAME_SIZE];
            a.interleaveTo(result);
            for (int i = 0; i < FRAME_SIZE; i++) {
                assertEquals(1.0f + sine(FRAME_SIZE)[i], result[i], 1e-5f);
            }
        }
    }

    @Test
    void mixRejectsMismatchedBuffers() {
        try (AudioBuffer mono = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer stereo = new AudioBuffer(context, 2, FRAME_SIZE)) {
            assertThrows(IllegalArgumentException.class, () -> mono.mix(stereo));
        }
    }

    @Test
    void downmixAveragesChannels() {
        try (AudioBuffer stereo = new AudioBuffer(context, 2, FRAME_SIZE);
             AudioBuffer mono = new AudioBuffer(context, 1, FRAME_SIZE)) {

            float[] interleaved = new float[2 * FRAME_SIZE];
            for (int i = 0; i < FRAME_SIZE; i++) {
                interleaved[2 * i] = 1.0f;      // left = 1
                interleaved[2 * i + 1] = 0.5f;  // right = 0.5
            }
            stereo.deinterleaveFrom(interleaved);

            mono.downmixFrom(stereo);

            float[] result = new float[FRAME_SIZE];
            mono.interleaveTo(result);
            for (int i = 0; i < FRAME_SIZE; i++) {
                // (1.0 + 0.5) / 2 = 0.75
                assertEquals(0.75f, result[i], 1e-5f);
            }
        }
    }

    @Test
    void ambisonicsN3dToSn3dScalesNonWChannels() {
        // N3D and SN3D share ACN ordering but differ in normalization: SN3D
        // scales the order-1 coefficients by 1/sqrt(3) relative to N3D
        // (the W coefficient is 1 in both).
        try (AudioBuffer n3d = new AudioBuffer(context, 4, FRAME_SIZE);
             AudioBuffer sn3d = new AudioBuffer(context, 4, FRAME_SIZE)) {

            float[] interleaved = new float[4 * FRAME_SIZE];
            for (int i = 0; i < FRAME_SIZE; i++) {
                interleaved[4 * i] = 1.0f;                  // W
                interleaved[4 * i + 1] = 1.0f;              // Y
                interleaved[4 * i + 2] = 1.0f;              // Z
                interleaved[4 * i + 3] = 1.0f;              // X
            }
            n3d.deinterleaveFrom(interleaved);

            sn3d.convertAmbisonicsFrom(AmbisonicsType.N3D, AmbisonicsType.SN3D, n3d);

            float[] result = new float[4 * FRAME_SIZE];
            sn3d.interleaveTo(result);
            double scale = 1.0 / Math.sqrt(3.0);
            for (int i = 0; i < FRAME_SIZE; i++) {
                assertEquals(1.0f, result[4 * i], 1e-4f, "W must be unchanged");
                assertEquals((float) scale, result[4 * i + 1], 1e-4f, "Y must be scaled");
                assertEquals((float) scale, result[4 * i + 2], 1e-4f, "Z must be scaled");
                assertEquals((float) scale, result[4 * i + 3], 1e-4f, "X must be scaled");
            }
        }
    }
}
