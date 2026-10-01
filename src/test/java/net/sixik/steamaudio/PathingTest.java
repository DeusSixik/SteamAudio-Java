package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты pathing-симуляции: запекание данных проб, звук через дверной
 * проем между комнатами.
 * <p>
 * Сцена — замкнутый бокс {@code [-5..5] x [0..3] x [-5..5]} с внутренней
 * стеной по x = 0 и дверным проемом (z от -1 до 1, во всю высоту).
 * Слушатель в комнате x &lt; 0 (у стены z = 5), источник в комнате x &gt; 0
 * напротив: прямой луч заблокирован панелью стены (z от 1 до 5), но
 * pathing должен найти путь через проем.
 */
class PathingTest {

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

    private static float energy(float[] samples) {
        float result = 0.0f;
        for (float sample : samples) {
            result += sample * sample;
        }
        return result;
    }

    private static float[] sine(int numSamples) {
        float[] input = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return input;
    }

    /**
     * Собирает сцену: внешние стены бокса + внутренняя стена с проемом.
     * Возвращает массив треугольников фиксированной длины 20.
     */
    private static Scene buildScene(Context context) {
        Scene scene = new Scene(context);

        float[] vertices = {
                // внешний бокс
                -5, 0, -5, -5, 0, 5, 5, 0, 5, 5, 0, -5,
                -5, 3, -5, -5, 3, 5, 5, 3, 5, 5, 3, -5,
                // панель стены z от -5 до -1
                0, 0, -5, 0, 0, -1, 0, 3, -1, 0, 3, -5,
                // панель стены z от 1 до 5
                0, 0, 1, 0, 0, 5, 0, 3, 5, 0, 3, 1,
        };

        int[] triangles = {
                // внешний бокс
                0, 1, 2, 0, 2, 3,       // пол
                4, 6, 5, 4, 7, 6,       // потолок
                0, 4, 5, 0, 5, 1,       // стена x = -5
                3, 2, 6, 3, 6, 7,       // стена x = +5
                1, 5, 6, 1, 6, 2,       // стена z = +5
                0, 3, 7, 0, 7, 4,       // стена z = -5
                // панели внутренней стены
                8, 9, 10, 8, 10, 11,
                12, 13, 14, 12, 14, 15,
        };

        int[] materialIndices = new int[triangles.length / 3];

        scene.createStaticMesh(vertices, triangles, materialIndices, new Material[]{Material.concrete()});
        scene.commit();
        return scene;
    }

    private static ProbeBatch buildAndBakeProbes(Context context, Scene scene) {
        ProbeBatch probeBatch = new ProbeBatch(context);
        try (ProbeArray probeArray = new ProbeArray(context)) {
            // Единичный куб [0..1]^3 -> объем x[-5..5], y[0..3], z[-5..5].
            float[] transform = {
                    10, 0, 0, -5,
                    0, 3, 0, 0,
                    0, 0, 10, -5,
                    0, 0, 0, 1,
            };
            probeArray.generateProbes(scene, ProbeArray.GENERATION_UNIFORM_FLOOR, 2.0f, 1.0f, transform);
            assertTrue(probeArray.getNumProbes() > 0, "probes must be generated");

            probeBatch.addProbeArray(probeArray);
        }
        probeBatch.commit();

        PathBaker.bake(context, scene, probeBatch,
                2 /* numSamples */, 0.5f /* radius */, 0.3f /* threshold */,
                10.0f /* visRange */, 100.0f /* pathRange */, 1 /* numThreads */);

        return probeBatch;
    }

    private static int simulationFlags() {
        return Simulator.FLAGS_DIRECT | Simulator.FLAGS_PATHING;
    }

    @Test
    void soundTravelsThroughDoorway() {
        try (Scene scene = buildScene(context);
             ProbeBatch probeBatch = buildAndBakeProbes(context, scene);
             Simulator simulator = new Simulator(context, simulationFlags(), SAMPLING_RATE, FRAME_SIZE)) {

            simulator.setScene(scene);
            simulator.addProbeBatch(probeBatch);
            simulator.commit();

            simulator.setSharedInputs(simulationFlags(),
                    new Vector3(-4, 1, 3.5f), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                    32, 2, 0.2f, AMBISONIC_ORDER, 1.0f);

            try (Source source = new Source(simulator, simulationFlags())) {
                source.add();
                simulator.commit();

                source.setDirectInputs(
                        Source.DIRECT_SIM_DISTANCE_ATTENUATION | Source.DIRECT_SIM_OCCLUSION,
                        new Vector3(4, 1, 3.5f), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                        Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
                source.setPathingInputs(probeBatch, AMBISONIC_ORDER,
                        0.5f /* visRadius */, 0.3f /* visThreshold */, 10.0f /* visRange */);

                simulator.runDirect();
                simulator.runPathing();

                // Прямой луч заблокирован стеной...
                float[] directOutputs = new float[9];
                source.getDirectOutputsInto(directOutputs);
                assertEquals(0.0f, directOutputs[5], 1e-4f,
                        "direct ray must be occluded by the interior wall");

                // ...но звук должен прийти по пути через проем.
                try (PathEffect effect = new PathEffect(context, SAMPLING_RATE, FRAME_SIZE, AMBISONIC_ORDER);
                     AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                     AudioBuffer out = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE)) {

                    in.deinterleaveFrom(sine(FRAME_SIZE));

                    int state = effect.apply(in, out, source, AMBISONIC_ORDER);
                    assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                            || state == BinauralEffect.STATE_TAIL_COMPLETE);

                    float[] rendered = new float[AMBISONIC_CHANNELS * FRAME_SIZE];
                    out.interleaveTo(rendered);
                    assertTrue(energy(rendered) > 0.0f,
                            "pathing must deliver audio through the doorway");
                }
            }
        }
    }

    @Test
    void probeBatchSerializationRoundtrip() {
        Scene scene = buildScene(context);
        ProbeBatch probeBatch = buildAndBakeProbes(context, scene);

        try (SerializedObject serialized = new SerializedObject(context)) {
            probeBatch.save(serialized);
            assertTrue(serialized.getSize() > 0, "serialized probe batch must not be empty");

            try (ProbeBatch restored = ProbeBatch.load(context, serialized)) {
                assertTrue(restored.isOpen());
                assertEquals(probeBatch.getNumProbes(), restored.getNumProbes(),
                        "restored probe batch must have the same number of probes");

                try (SerializedObject second = new SerializedObject(context)) {
                    restored.save(second);
                    assertEquals(serialized.getSize(), second.getSize(),
                            "re-serialized probe batch must have the same size");
                }
            }
        } finally {
            probeBatch.close();
            scene.close();
        }
    }
}
