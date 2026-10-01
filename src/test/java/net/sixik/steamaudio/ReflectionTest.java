package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты reflections-симуляции: параметрический реверб и convolution
 * реверб с микшером. Сцена — тот же замкнутый бокс, что и в
 * {@link SimulatorTest}.
 */
class ReflectionTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;

    /** Длительность IR reflections-симуляции, сек. */
    private static final float IR_DURATION = 0.2f;

    /** Ambisonic order 1 => 4 канала IR. */
    private static final int AMBISONIC_ORDER = 1;
    private static final int AMBISONIC_CHANNELS = (AMBISONIC_ORDER + 1) * (AMBISONIC_ORDER + 1);

    private static final float[] BOX_VERTICES = {
            -5, 0, -5, -5, 0, 5, 5, 0, 5, 5, 0, -5,
            -5, 3, -5, -5, 3, 5, 5, 3, 5, 5, 3, -5,
    };

    private static final int[] BOX_TRIANGLES = {
            0, 1, 2, 0, 2, 3,
            4, 6, 5, 4, 7, 6,
            0, 4, 5, 0, 5, 1,
            3, 2, 6, 3, 6, 7,
            1, 5, 6, 1, 6, 2,
            0, 3, 7, 0, 7, 4,
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

    private static int simulationFlags() {
        return Simulator.FLAGS_DIRECT | Simulator.FLAGS_REFLECTIONS;
    }

    private static void runSimulation(Context context, Scene scene, Simulator simulator, Source source) {
        simulator.setScene(scene);
        simulator.commit();

        simulator.setSharedInputs(simulationFlags(),
                new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                32, 2, IR_DURATION, AMBISONIC_ORDER, 1.0f);

        source.add();
        simulator.commit();

        source.setDirectInputs(Source.DIRECT_SIM_DISTANCE_ATTENUATION | Source.DIRECT_SIM_OCCLUSION,
                new Vector3(3, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
        source.setReflectionsInputs(new float[]{1.0f, 1.0f, 1.0f});

        simulator.runDirect();
        simulator.runReflections();
    }

    @Test
    void parametricReverbProducesTail() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES, new int[12],
                    new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, simulationFlags(), SAMPLING_RATE, FRAME_SIZE);
                 Source source = new Source(simulator, simulationFlags())) {
                runSimulation(context, scene, simulator, source);

                try (ReflectionEffect effect = new ReflectionEffect(context, SAMPLING_RATE, FRAME_SIZE,
                        ReflectionEffect.TYPE_PARAMETRIC, FRAME_SIZE, AMBISONIC_CHANNELS);
                     AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                     AudioBuffer out = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE)) {

                    in.deinterleaveFrom(sine(FRAME_SIZE));

                    int state = effect.apply(in, out, source, null);
                    assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                            || state == BinauralEffect.STATE_TAIL_COMPLETE);

                    float[] rendered = new float[AMBISONIC_CHANNELS * FRAME_SIZE];
                    out.interleaveTo(rendered);
                    assertTrue(energy(rendered) > 0.0f, "parametric reverb must produce audio");
                }
            }
        }
    }

    @Test
    void convolutionReverbWithMixerProducesAudio() {
        int irSize = (int) (SAMPLING_RATE * IR_DURATION);

        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES, new int[12],
                    new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, simulationFlags(), SAMPLING_RATE, FRAME_SIZE);
                 Source source = new Source(simulator, simulationFlags())) {
                runSimulation(context, scene, simulator, source);

                try (ReflectionEffect effect = new ReflectionEffect(context, SAMPLING_RATE, FRAME_SIZE,
                        ReflectionEffect.TYPE_CONVOLUTION, irSize, AMBISONIC_CHANNELS);
                     ReflectionMixer mixer = new ReflectionMixer(context, SAMPLING_RATE, FRAME_SIZE,
                             ReflectionEffect.TYPE_CONVOLUTION, irSize, AMBISONIC_CHANNELS);
                     AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                     AudioBuffer out = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE)) {

                    in.deinterleaveFrom(sine(FRAME_SIZE));

                    // Вклад источника накапливается в микшере...
                    int state = effect.apply(in, out, source, mixer);
                    assertTrue(state == BinauralEffect.STATE_TAIL_REMAINING
                            || state == BinauralEffect.STATE_TAIL_COMPLETE);

                    // ...и извлекается из него.
                    int mixerState = mixer.apply(out, source);
                    assertTrue(mixerState == BinauralEffect.STATE_TAIL_REMAINING
                            || mixerState == BinauralEffect.STATE_TAIL_COMPLETE);

                    float[] rendered = new float[AMBISONIC_CHANNELS * FRAME_SIZE];
                    out.interleaveTo(rendered);
                    assertTrue(energy(rendered) > 0.0f, "convolution reverb must produce audio");
                }
            }
        }
    }

    @Test
    void reverbTailIsDrained() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(BOX_VERTICES, BOX_TRIANGLES, new int[12],
                    new Material[]{Material.concrete()});
            scene.commit();

            try (Simulator simulator = new Simulator(context, simulationFlags(), SAMPLING_RATE, FRAME_SIZE);
                 Source source = new Source(simulator, simulationFlags())) {
                runSimulation(context, scene, simulator, source);

                try (ReflectionEffect effect = new ReflectionEffect(context, SAMPLING_RATE, FRAME_SIZE,
                        ReflectionEffect.TYPE_PARAMETRIC, FRAME_SIZE, AMBISONIC_CHANNELS);
                     AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
                     AudioBuffer out = new AudioBuffer(context, AMBISONIC_CHANNELS, FRAME_SIZE)) {

                    in.deinterleaveFrom(sine(FRAME_SIZE));
                    effect.apply(in, out, source, null);

                    int tailSize = effect.getTailSize();
                    assertTrue(tailSize >= 0);

                    // Дожимаем хвост: после остановки входа getTail должен
                    // вернуть TAILCOMPLETE за конечное число фреймов.
                    int state = BinauralEffect.STATE_TAIL_REMAINING;
                    int frames = 0;
                    while (state == BinauralEffect.STATE_TAIL_REMAINING && frames < 64) {
                        state = effect.getTail(out, null);
                        frames++;
                    }
                    assertEquals(BinauralEffect.STATE_TAIL_COMPLETE, state);
                }
            }
        }
    }
}
