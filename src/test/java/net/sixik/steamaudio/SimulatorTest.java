package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Сквозной тест direct-пайплайна: геометрия → Simulator → Source →
 * iplSourceGetOutputs → DirectEffect → аудио.
 * <p>
 * Сцена — замкнутый бокс {@code [-5..5] x [0..3] x [-5..5]}, слушатель
 * внутри у пола, смотрит вдоль -z.
 */
class SimulatorTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;

    /** Замкнутый бокс: 8 вершин, 12 треугольников. */
    private static final float[] BOX_VERTICES = {
            -5, 0, -5, -5, 0, 5, 5, 0, 5, 5, 0, -5,
            -5, 3, -5, -5, 3, 5, 5, 3, 5, 5, 3, -5,
    };

    private static final int[] BOX_TRIANGLES = {
            0, 1, 2, 0, 2, 3,       // пол
            4, 6, 5, 4, 7, 6,       // потолок
            0, 4, 5, 0, 5, 1,       // стена x = -5
            3, 2, 6, 3, 6, 7,       // стена x = +5
            1, 5, 6, 1, 6, 2,       // стена z = +5
            0, 3, 7, 0, 7, 4,       // стена z = -5
    };

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static float sine(float[] out, int numSamples) {
        for (int i = 0; i < numSamples; i++) {
            out[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return 0;
    }

    private static float energy(float[] samples) {
        float result = 0.0f;
        for (float sample : samples) {
            result += sample * sample;
        }
        return result;
    }

    @Test
    void unoccludedSourceIsFullyAudible() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES,
                    new int[12], new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, Simulator.FLAGS_DIRECT, SAMPLING_RATE, FRAME_SIZE)) {
                simulator.setScene(scene);
                simulator.commit();

                simulator.setSharedInputs(Simulator.FLAGS_DIRECT,
                        new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                        1, 1, 0.5f, 1, 1.0f);

                try (Source source = new Source(simulator, Simulator.FLAGS_DIRECT)) {
                    source.add();
                    simulator.commit();

                    source.setDirectInputs(
                            Source.DIRECT_SIM_DISTANCE_ATTENUATION | Source.DIRECT_SIM_OCCLUSION,
                            new Vector3(3, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                            Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
                    simulator.runDirect();

                    float[] outputs = new float[9];
                    source.getDirectOutputsInto(outputs);

                    // Луч от слушателя к источнику не задет стенами: полная
                    // слышимость, затухание по дистанции в (0, 1).
                    assertEquals(1.0f, outputs[5], 1e-4f, "unoccluded source occlusion must be 1.0");
                    assertTrue(outputs[0] > 0.0f && outputs[0] < 1.0f,
                            "distance attenuation at 3 m must be in (0, 1), got " + outputs[0]);
                }
            }
        }
    }

    @Test
    void occludedSourceIsSilenced() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES,
                    new int[12], new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, Simulator.FLAGS_DIRECT, SAMPLING_RATE, FRAME_SIZE)) {
                simulator.setScene(scene);
                simulator.commit();

                simulator.setSharedInputs(Simulator.FLAGS_DIRECT,
                        new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                        1, 1, 0.5f, 1, 1.0f);

                try (Source source = new Source(simulator, Simulator.FLAGS_DIRECT)) {
                    source.add();
                    simulator.commit();

                    // Источник над потолком: луч listener→source задет
                    // геометрией бокса.
                    source.setDirectInputs(
                            Source.DIRECT_SIM_DISTANCE_ATTENUATION | Source.DIRECT_SIM_OCCLUSION,
                            new Vector3(0, 10, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                            Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
                    simulator.runDirect();

                    float[] outputs = new float[9];
                    source.getDirectOutputsInto(outputs);

                    assertEquals(0.0f, outputs[5], 1e-4f, "occluded source occlusion must be 0.0");

                    try (DirectEffect effect = new DirectEffect(context, SAMPLING_RATE, FRAME_SIZE, 1);
                         AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                         AudioBuffer out = new AudioBuffer(context, 1, FRAME_SIZE)) {

                        float[] input = new float[FRAME_SIZE];
                        sine(input, FRAME_SIZE);
                        in.deinterleaveFrom(input);

                        int state = effect.apply(in, out, outputs,
                                DirectEffect.APPLY_DISTANCE_ATTENUATION | DirectEffect.APPLY_OCCLUSION,
                                DirectEffect.TRANSMISSION_FREQ_INDEPENDENT);
                        assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                                || state == BinauralEffect.STATE_TAIL_COMPLETE);

                        float[] rendered = new float[FRAME_SIZE];
                        out.interleaveTo(rendered);
                        assertEquals(0.0f, energy(rendered), 1e-6f,
                                "fully occluded source must produce silence");
                    }
                }
            }
        }
    }

    @Test
    void unoccludedAudioIsAudibleButAttenuated() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES,
                    new int[12], new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, Simulator.FLAGS_DIRECT, SAMPLING_RATE, FRAME_SIZE)) {
                simulator.setScene(scene);
                simulator.commit();

                simulator.setSharedInputs(Simulator.FLAGS_DIRECT,
                        new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                        1, 1, 0.5f, 1, 1.0f);

                try (Source source = new Source(simulator, Simulator.FLAGS_DIRECT)) {
                    source.add();
                    simulator.commit();

                    source.setDirectInputs(
                            Source.DIRECT_SIM_DISTANCE_ATTENUATION | Source.DIRECT_SIM_OCCLUSION,
                            new Vector3(3, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                            Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
                    simulator.runDirect();

                    float[] outputs = new float[9];
                    source.getDirectOutputsInto(outputs);

                    try (DirectEffect effect = new DirectEffect(context, SAMPLING_RATE, FRAME_SIZE, 1);
                         AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                         AudioBuffer out = new AudioBuffer(context, 1, FRAME_SIZE)) {

                        float[] input = new float[FRAME_SIZE];
                        sine(input, FRAME_SIZE);
                        in.deinterleaveFrom(input);

                        effect.apply(in, out, outputs,
                                DirectEffect.APPLY_DISTANCE_ATTENUATION | DirectEffect.APPLY_OCCLUSION,
                                DirectEffect.TRANSMISSION_FREQ_INDEPENDENT);

                        float[] rendered = new float[FRAME_SIZE];
                        out.interleaveTo(rendered);

                        float inputEnergy = energy(input);
                        float outputEnergy = energy(rendered);

                        assertTrue(outputEnergy > 0.0f, "unoccluded source must produce audio");
                        // Затухание ≈ 1/3 на дистанции 3 м: энергия должна
                        // упасть примерно в 9 раз.
                        assertTrue(outputEnergy < inputEnergy,
                                "attenuated output must be quieter than input");
                    }
                }
            }
        }
    }
}
