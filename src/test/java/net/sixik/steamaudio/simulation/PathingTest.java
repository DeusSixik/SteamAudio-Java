package net.sixik.steamaudio.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SerializedObject;
import net.sixik.steamaudio.core.Vector3;
import net.sixik.steamaudio.effects.BinauralEffect;
import net.sixik.steamaudio.effects.PathEffect;
import net.sixik.steamaudio.geometry.Material;
import net.sixik.steamaudio.geometry.Scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Pathing simulation tests: baking probe data, sound through a doorway
 * between rooms.
 * <p>
 * The scene is a closed box {@code [-5..5] x [0..3] x [-5..5]} with an interior
 * wall at x = 0 and a doorway (z from -1 to 1, full height).
 * The listener is in the room x &lt; 0 (near the wall z = 5), the source is in the room x &gt; 0
 * opposite: the direct ray is blocked by the wall panel (z from 1 to 5), but
 * pathing must find a path through the doorway.
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
     * Builds the scene: outer walls of the box + interior wall with a doorway.
     * Returns a fixed-length triangle array of 20.
     */
    private static Scene buildScene(Context context) {
        Scene scene = new Scene(context);

        float[] vertices = {
                // outer box
                -5, 0, -5, -5, 0, 5, 5, 0, 5, 5, 0, -5,
                -5, 3, -5, -5, 3, 5, 5, 3, 5, 5, 3, -5,
                // wall panel z from -5 to -1
                0, 0, -5, 0, 0, -1, 0, 3, -1, 0, 3, -5,
                // wall panel z from 1 to 5
                0, 0, 1, 0, 0, 5, 0, 3, 5, 0, 3, 1,
        };

        int[] triangles = {
                // outer box
                0, 1, 2, 0, 2, 3,       // floor
                4, 6, 5, 4, 7, 6,       // ceiling
                0, 4, 5, 0, 5, 1,       // wall x = -5
                3, 2, 6, 3, 6, 7,       // wall x = +5
                1, 5, 6, 1, 6, 2,       // wall z = +5
                0, 3, 7, 0, 7, 4,       // wall z = -5
                // interior wall panels
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
            // Unit cube [0..1]^3 -> volume x[-5..5], y[0..3], z[-5..5].
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

                // The direct ray is blocked by the wall...
                float[] directOutputs = new float[9];
                source.getDirectOutputsInto(directOutputs);
                assertEquals(0.0f, directOutputs[5], 1e-4f,
                        "direct ray must be occluded by the interior wall");

                // ...but the sound must arrive along a path through the doorway.
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
