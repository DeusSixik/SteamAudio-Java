package net.sixik.steamaudio.effects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.audio.SpeakerLayout;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.Vector3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the P1 audio-rendering additions: {@link PanningEffect},
 * {@link VirtualSurroundEffect}, {@link AmbisonicsDecodeEffect}, the
 * context-level compute helpers, and the lifecycle methods
 * (reset/getTailSize/getTail) of the ambisonics effects.
 */
class P1EffectsTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;
    private static final int AMBISONIC_ORDER = 1;
    private static final int AMBISONIC_CHANNELS = (AMBISONIC_ORDER + 1) * (AMBISONIC_ORDER + 1);

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
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return input;
    }

    private static double[] channelEnergies(AudioBuffer buffer, int numChannels) {
        double[] result = new double[numChannels];
        float[] rendered = new float[numChannels * FRAME_SIZE];
        buffer.interleaveTo(rendered);
        for (int i = 0; i < rendered.length; i++) {
            result[i % numChannels] += rendered[i] * rendered[i];
        }
        return result;
    }

    @Test
    void panningMovesEnergyBetweenSpeakers() {
        try (PanningEffect panning = new PanningEffect(context, SAMPLING_RATE, FRAME_SIZE, SpeakerLayout.STEREO);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, SpeakerLayout.numSpeakers(SpeakerLayout.STEREO), FRAME_SIZE)) {

            in.deinterleaveFrom(sine(FRAME_SIZE));

            // Panning crossfades from the previous frame's direction, so each
            // direction is applied twice: the first apply warms up, the second
            // is the steady state used for measurement.
            panning.apply(in, out, new Vector3(1, 0, 0));
            panning.apply(in, out, new Vector3(1, 0, 0));
            double[] right = channelEnergies(out, 2);

            panning.apply(in, out, new Vector3(-1, 0, 0));
            panning.apply(in, out, new Vector3(-1, 0, 0));
            double[] left = channelEnergies(out, 2);

            assertTrue(right[0] > 0 && right[1] > 0, "panning must produce both channels");
            assertTrue(right[1] > right[0], "source to the right must be louder in the right speaker");
            assertTrue(left[0] > left[1], "source to the left must be louder in the left speaker");
        }
    }

    @Test
    void virtualSurroundSpatializesStereo() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
             VirtualSurroundEffect virtualSurround = new VirtualSurroundEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     SpeakerLayout.STEREO, hrtf);
             AudioBuffer in = new AudioBuffer(context, 2, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            float[] interleaved = new float[2 * FRAME_SIZE];
            float[] sineInput = sine(FRAME_SIZE);
            for (int i = 0; i < FRAME_SIZE; i++) {
                interleaved[2 * i] = sineInput[i];
                interleaved[2 * i + 1] = sineInput[i];
            }
            in.deinterleaveFrom(interleaved);

            int state = virtualSurround.apply(in, out);
            assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                    || state == BinauralEffect.STATE_TAIL_COMPLETE);

            double[] ears = channelEnergies(out, 2);
            assertTrue(ears[0] > 0 && ears[1] > 0, "virtual surround must produce audio");
        }
    }

    @Test
    void ambisonicsDecodeToStereo() {
        try (AmbisonicsEncodeEffect encode = new AmbisonicsEncodeEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsDecodeEffect decode = new AmbisonicsDecodeEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     SpeakerLayout.STEREO, AMBISONIC_ORDER);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer encoded = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            in.deinterleaveFrom(sine(FRAME_SIZE));

            // Warm up the encoder crossfade, then encode a source to the right.
            encode.apply(in, encoded, new Vector3(1, 0, 0), AMBISONIC_ORDER);
            encode.apply(in, encoded, new Vector3(1, 0, 0), AMBISONIC_ORDER);

            decode.apply(encoded, out, AMBISONIC_ORDER);

            double[] speakers = channelEnergies(out, 2);
            assertTrue(speakers[0] > 0 && speakers[1] > 0, "decode must produce audio");
            assertTrue(speakers[1] > speakers[0], "source to the right must be louder in the right speaker");
        }
    }

    @Test
    void distanceAttenuationDecreasesWithDistance() {
        // Default model: inverse distance with a 1 m plateau.
        float near = context.calculateDistanceAttenuation(0, 1.0f,
                new Vector3(1, 0, 0), new Vector3(0, 0, 0));
        float far = context.calculateDistanceAttenuation(0, 1.0f,
                new Vector3(10, 0, 0), new Vector3(0, 0, 0));

        assertEquals(1.0f, near, 1e-4f, "within the 1 m plateau there must be no attenuation");
        assertTrue(far > 0.0f && far < near, "attenuation must decrease with distance");
    }

    @Test
    void airAbsorptionStrongerAtHighFrequencies() {
        float[] coefficients = context.calculateAirAbsorption(
                new Vector3(100, 0, 0), new Vector3(0, 0, 0));

        assertEquals(3, coefficients.length);
        for (float coefficient : coefficients) {
            assertTrue(coefficient > 0.0f && coefficient <= 1.0f, "coefficients must be in (0, 1]");
        }
        assertTrue(coefficients[2] < coefficients[0],
                "high frequencies must be absorbed more than low ones");
    }

    @Test
    void directivityDipoleRejectsRearDirection() {
        // The dipole is oriented along the z-axis of the source coordinate
        // system: it has two lobes (+z and -z) and a null plane at z = 0.
        // A listener on the z-axis hears the source; a listener on the x-axis
        // (in the null plane) does not.
        float onAxis = context.calculateDirectivity(1.0f, 1.0f,
                new Vector3(0, 0, 0), new Vector3(0, 0, 1), new Vector3(0, 1, 0),
                new Vector3(0, 0, 1));
        float inNullPlane = context.calculateDirectivity(1.0f, 1.0f,
                new Vector3(0, 0, 0), new Vector3(0, 0, 1), new Vector3(0, 1, 0),
                new Vector3(1, 0, 0));

        assertTrue(onAxis > inNullPlane, "dipole must be louder on-axis than in the null plane");
        assertTrue(onAxis > 0.0f, "on-axis direction must be audible");
    }

    @Test
    void ambisonicsEffectLifecycle() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
             AmbisonicsEncodeEffect encode = new AmbisonicsEncodeEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsRotationEffect rotate = new AmbisonicsRotationEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsPanningEffect panning = new AmbisonicsPanningEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     SpeakerLayout.STEREO, AMBISONIC_ORDER);
             AmbisonicsBinauralEffect binaural = new AmbisonicsBinauralEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     hrtf, AMBISONIC_ORDER);
             AmbisonicsDecodeEffect decode = new AmbisonicsDecodeEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     SpeakerLayout.STEREO, AMBISONIC_ORDER);
             AudioBuffer ambisonics = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer stereo = new AudioBuffer(context, 2, FRAME_SIZE)) {

                encode.reset();
                rotate.reset();
                panning.reset();
                binaural.reset();
                decode.reset();

                assertTrue(encode.getTailSize() >= 0);
                assertTrue(rotate.getTailSize() >= 0);
                assertTrue(panning.getTailSize() >= 0);
                assertTrue(binaural.getTailSize() >= 0);
                assertTrue(decode.getTailSize() >= 0);

                int encodeState = encode.getTail(ambisonics);
                int rotateState = rotate.getTail(ambisonics);
                int panningState = panning.getTail(stereo);
                int binauralState = binaural.getTail(stereo);
                int decodeState = decode.getTail(stereo);
                assertTrue(encodeState == BinauralEffect.STATE_TAIL_REMAINING
                        || encodeState == BinauralEffect.STATE_TAIL_COMPLETE);
                assertTrue(rotateState == BinauralEffect.STATE_TAIL_REMAINING
                        || rotateState == BinauralEffect.STATE_TAIL_COMPLETE);
                assertTrue(panningState == BinauralEffect.STATE_TAIL_REMAINING
                        || panningState == BinauralEffect.STATE_TAIL_COMPLETE);
                assertTrue(binauralState == BinauralEffect.STATE_TAIL_REMAINING
                        || binauralState == BinauralEffect.STATE_TAIL_COMPLETE);
                assertTrue(decodeState == BinauralEffect.STATE_TAIL_REMAINING
                        || decodeState == BinauralEffect.STATE_TAIL_COMPLETE);
        }
    }
}
