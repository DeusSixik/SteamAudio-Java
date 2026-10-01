package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты амбисонических эффектов: полный пайплайн
 * encode → rotate → (binaural | panning), а также граничный случай
 * нулевого направления (омни-кодирование).
 */
class AmbisonicsTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;
    private static final int AMBISONIC_ORDER = 1;
    private static final int AMBISONIC_CHANNELS = (AMBISONIC_ORDER + 1) * (AMBISONIC_ORDER + 1);

    /** Раскладка динамиков Stereo: (1, 0, 0) и (-1, 0, 0). */
    private static final int SPEAKER_LAYOUT_STEREO = 1;

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static float energy(float[] samples) {
        float result = 0.0f;
        for (float sample : samples) {
            result += sample * sample;
        }
        return result;
    }

    private static float[] earEnergies(AudioBuffer stereo) {
        float[] rendered = new float[2 * FRAME_SIZE];
        stereo.interleaveTo(rendered);

        float left = 0.0f;
        float right = 0.0f;
        for (int i = 0; i < rendered.length; i += 2) {
            assertTrue(Float.isFinite(rendered[i]) && Float.isFinite(rendered[i + 1]),
                    "non-finite output sample");
            left += rendered[i] * rendered[i];
            right += rendered[i + 1] * rendered[i + 1];
        }
        return new float[]{left, right};
    }

    private static float[] sine(int numSamples) {
        float[] input = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return input;
    }

    @Test
    void encodeRotateBinauralPipeline() {
        try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
             AmbisonicsEncodeEffect encode = new AmbisonicsEncodeEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsRotationEffect rotate = new AmbisonicsRotationEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsBinauralEffect binaural = new AmbisonicsBinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf, AMBISONIC_ORDER);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer encoded = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer rotated = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            in.deinterleaveFrom(sine(FRAME_SIZE));

            // Источник справа от слушателя. Первый apply прогревает
            // crossfade-направление кодера: после создания/reset он начинает
            // с нулевого направления, второй apply — это steady state.
            encode.apply(in, encoded, new Vector3(1, 0, 0), AMBISONIC_ORDER);
            encode.apply(in, encoded, new Vector3(1, 0, 0), AMBISONIC_ORDER);

            // Слушатель смотрит вдоль -z: бинауральный рендер сохраняет
            // источник справа.
            rotate.apply(encoded, rotated,
                    new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0), AMBISONIC_ORDER);

            binaural.apply(rotated, out, AMBISONIC_ORDER);

            float[] ears = earEnergies(out);
            assertTrue(ears[0] > 0.0f && ears[1] > 0.0f, "pipeline must produce audio");
            assertTrue(ears[1] > ears[0], "source to the right must be louder in the right ear");
        }
    }

    @Test
    void encodeRotatePanningPipeline() {
        try (AmbisonicsEncodeEffect encode = new AmbisonicsEncodeEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsRotationEffect rotate = new AmbisonicsRotationEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AmbisonicsPanningEffect panning = new AmbisonicsPanningEffect(context, SAMPLING_RATE, FRAME_SIZE,
                     SPEAKER_LAYOUT_STEREO, AMBISONIC_ORDER);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer encoded = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer rotated = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            in.deinterleaveFrom(sine(FRAME_SIZE));

            encode.apply(in, encoded, new Vector3(0, 0, -1), AMBISONIC_ORDER); // источник прямо впереди
            encode.apply(in, encoded, new Vector3(0, 0, -1), AMBISONIC_ORDER); // прогрев crossfade
            rotate.apply(encoded, rotated,
                    new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0), AMBISONIC_ORDER);
            panning.apply(rotated, out, AMBISONIC_ORDER);

            float[] ears = earEnergies(out);
            assertTrue(ears[0] > 0.0f && ears[1] > 0.0f, "pipeline must produce audio");
            // Прямо впереди: уши симметричны — громкость совпадает в пределах 1%.
            assertEquals(ears[0], ears[1], 0.01f * ears[0],
                    "straight-ahead source must be symmetric between ears");
        }
    }

    @Test
    void zeroDirectionEncodesOmnidirectional() {
        try (AmbisonicsEncodeEffect encode = new AmbisonicsEncodeEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer encoded = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE)) {

            in.deinterleaveFrom(sine(FRAME_SIZE));

            // Обнуляем выходной буфер: iplAudioBufferAllocate не инициализирует
            // данные, и в омни-режиме phonon перезаписывает только канал W.
            encoded.deinterleaveFrom(new float[AMBISONIC_CHANNELS * FRAME_SIZE]);

            encode.apply(in, encoded, new Vector3(0, 0, 0), AMBISONIC_ORDER);
            encode.apply(in, encoded, new Vector3(0, 0, 0), AMBISONIC_ORDER); // прогрев crossfade

            float[] rendered = new float[AMBISONIC_CHANNELS * FRAME_SIZE];
            encoded.interleaveTo(rendered);

            // Порядок 0 (омни): энергия только в первом (W) канале;
            // остальные каналы должны оставаться нулевыми (прогрев окончен).
            float[] energies = new float[AMBISONIC_CHANNELS];
            for (int i = 0; i < rendered.length; i++) {
                energies[i % AMBISONIC_CHANNELS] += rendered[i] * rendered[i];
            }
            assertTrue(energies[0] > 0.0f, "W channel must carry the signal");
            for (int channel = 1; channel < AMBISONIC_CHANNELS; channel++) {
                assertEquals(0.0f, energies[channel], 1e-5f,
                        "order 0 output must be silent in non-W channels");
            }
        }
    }
}
