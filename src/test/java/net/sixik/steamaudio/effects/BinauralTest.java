package net.sixik.steamaudio.effects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.Vector3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Integration tests for binaural processing: {@link HRTF} +
 * {@link BinauralEffect} + {@code iplCalculateRelativeDirection}.
 */
class BinauralTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;

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
    void relativeDirectionStraightAhead() {
        Vector3 relative = context.calculateRelativeDirection(
                new Vector3(0, 0, -5),   // source straight ahead
                new Vector3(0, 0, 0),    // listener at the origin
                new Vector3(0, 0, -1),   // listener looking along -z
                new Vector3(0, 1, 0));   // listener up: +y

        assertEquals(0.0f, relative.x, 1e-4f);
        assertEquals(0.0f, relative.y, 1e-4f);
        assertEquals(-1.0f, relative.z, 1e-4f);
    }

    @Test
    void relativeDirectionToTheRight() {
        Vector3 relative = context.calculateRelativeDirection(
                new Vector3(5, 0, 0),    // source to the right
                new Vector3(0, 0, 0),
                new Vector3(0, 0, -1),
                new Vector3(0, 1, 0));

        assertEquals(1.0f, relative.x, 1e-4f);
    }

    @Test
    void binauralRenderingProducesDifferentEars() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
             BinauralEffect effect = new BinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            float[] input = new float[FRAME_SIZE];
            for (int i = 0; i < FRAME_SIZE; i++) {
                input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
            }
            in.deinterleaveFrom(input);

            // Source to the right of the listener: the right ear must receive
            // more energy than the left ear.
            int state = effect.apply(in, out, new Vector3(1, 0, 0),
                    BinauralEffect.INTERPOLATION_NEAREST, 1.0f);
            assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                    || state == BinauralEffect.STATE_TAIL_COMPLETE);

            float[] rendered = new float[2 * FRAME_SIZE];
            out.interleaveTo(rendered);

            float leftEnergy = 0.0f;
            float rightEnergy = 0.0f;
            for (int i = 0; i < rendered.length; i += 2) {
                float left = rendered[i];
                float right = rendered[i + 1];
                assertTrue(Float.isFinite(left) && Float.isFinite(right), "non-finite output sample");
                leftEnergy += left * left;
                rightEnergy += right * right;
            }

            assertTrue(leftEnergy > 0.0f, "left ear has no signal");
            assertTrue(rightEnergy > 0.0f, "right ear has no signal");
            assertTrue(rightEnergy > leftEnergy, "source to the right must be louder in the right ear");
        }
    }

    @Test
    void resetAndTail() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
             BinauralEffect effect = new BinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            in.deinterleaveFrom(new float[FRAME_SIZE]);
            effect.apply(in, out, new Vector3(0, 0, -1),
                    BinauralEffect.INTERPOLATION_NEAREST, 1.0f);

            assertTrue(effect.getTailSize() >= 0);
            assertDoesNotThrow(effect::reset);

            int state = effect.getTail(out);
            assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                    || state == BinauralEffect.STATE_TAIL_COMPLETE);
        }
    }

    @Test
    void closedEffectIsNotUsable() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE)) {
            BinauralEffect effect = new BinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf);
            effect.close();
            assertFalse(effect.isOpen());

            try (AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                 AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {
                assertThrows(IllegalStateException.class,
                        () -> effect.apply(in, out, new Vector3(0, 0, -1),
                                BinauralEffect.INTERPOLATION_NEAREST, 1.0f));
            }
        }
    }
}
