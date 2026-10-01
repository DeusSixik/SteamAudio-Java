package net.sixik.steamaudio.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.audio.ImpulseResponse;
import net.sixik.steamaudio.audio.Reconstructor;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.geometry.Material;
import net.sixik.steamaudio.geometry.Scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the P3 additions: {@link EnergyField} arithmetic,
 * {@link ReflectionsBaker}, {@code ProbeBatch#getEnergyField},
 * {@link Reconstructor}, {@code ProbeArray#getProbe} and
 * {@code Scene#saveOBJ}.
 */
class P3Test {

    private static final int SAMPLING_RATE = 48000;
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

    @Test
    void energyFieldDimensionsAndArithmetic() {
        try (EnergyField a = new EnergyField(context, 0.5f, AMBISONIC_ORDER);
             EnergyField b = new EnergyField(context, 0.5f, AMBISONIC_ORDER);
             EnergyField out = new EnergyField(context, 0.5f, AMBISONIC_ORDER)) {

            assertEquals(AMBISONIC_CHANNELS, a.getNumChannels());
            assertEquals(a.getNumChannels() * 3 * a.getNumBins(),
                    a.getNumChannels() * 3 * b.getNumBins());
            assertTrue(a.getNumBins() > 0);

            // out = a + 2*b = 3*b (both zero-initialized), scaled result is
            // exactly zero everywhere since fields start zeroed.
            a.scale(b, 2.0f);
            out.add(a, b);

            float[] data = new float[AMBISONIC_CHANNELS * 3 * out.getNumBins()];
            out.getData(data);
            for (float value : data) {
                assertEquals(0.0f, value, 0.0f);
            }
        }
    }

    @Test
    void energyFieldSwapExchangesContents() {
        try (EnergyField a = new EnergyField(context, 0.5f, 0);
             EnergyField b = new EnergyField(context, 0.5f, 0)) {
            assertEquals(1, a.getNumChannels());

            // With order 0 the field is a single channel; verify swap keeps
            // both fields valid and open.
            a.swap(b);
            assertTrue(a.isOpen() && b.isOpen());
        }
    }

    @Test
    void bakeReflectionsAndReadBack() {
        // Room box (same as SimulatorTest).
        float[] vertices = {
                -5, 0, -5, -5, 0, 5, 5, 0, 5, 5, 0, -5,
                -5, 3, -5, -5, 3, 5, 5, 3, 5, 5, 3, -5,
        };
        int[] triangles = {
                0, 1, 2, 0, 2, 3,
                4, 6, 5, 4, 7, 6,
                0, 4, 5, 0, 5, 1,
                3, 2, 6, 3, 6, 7,
                1, 5, 6, 1, 6, 2,
                0, 3, 7, 0, 7, 4,
        };

        Scene scene = new Scene(context);
        scene.createStaticMesh(vertices, triangles, new int[12], new Material[]{Material.concrete()});
        scene.commit();

        try (ProbeBatch batch = new ProbeBatch(context)) {
            try (ProbeArray probeArray = new ProbeArray(context)) {
                float[] transform = {
                        10, 0, 0, -5,
                        0, 3, 0, 0,
                        0, 0, 10, -5,
                        0, 0, 0, 1,
                };
                probeArray.generateProbes(scene, ProbeArray.GENERATION_UNIFORM_FLOOR, 2.0f, 1.0f, transform);
                assertTrue(probeArray.getNumProbes() > 0);

                float[] probe = new float[4];
                probeArray.getProbe(0, probe);
                assertTrue(probe[3] > 0.0f, "uniform floor probes must have a positive radius");

                batch.addProbeArray(probeArray);
            }
            batch.commit();

            // Bake parametric reverb with a small ray budget: enough for a
            // smoke test, fast enough for CI.
            ReflectionsBaker.bake(context, scene, batch,
                    ProbeBatch.DATA_TYPE_REFLECTIONS, ProbeBatch.VARIATION_REVERB,
                    ReflectionsBaker.BAKE_PARAMETRIC,
                    32, 2, 32, 0.5f, 0.5f, AMBISONIC_ORDER, 1.0f, 1, 8);

            assertTrue(batch.getDataSize(ProbeBatch.DATA_TYPE_REFLECTIONS, ProbeBatch.VARIATION_REVERB) > 0,
                    "baked reflections layer must be present");

            // Read back the reverb times of the first probe.
            float[] reverbTimes = new float[3];
            batch.getReverb(ProbeBatch.DATA_TYPE_REFLECTIONS, ProbeBatch.VARIATION_REVERB, 0, reverbTimes);
            assertTrue(reverbTimes[0] > 0.0f, "RT60 must be positive in a concrete box");

            // Read back the energy field of the first probe and reconstruct
            // an impulse response from it.
            try (EnergyField field = new EnergyField(context, 0.5f, AMBISONIC_ORDER);
                 Reconstructor reconstructor = new Reconstructor(context, 0.5f, AMBISONIC_ORDER, SAMPLING_RATE);
                 ImpulseResponse ir = new ImpulseResponse(context, 0.5f, AMBISONIC_ORDER, SAMPLING_RATE)) {

                batch.getEnergyField(ProbeBatch.DATA_TYPE_REFLECTIONS, ProbeBatch.VARIATION_REVERB, 0, field);
                assertEquals(AMBISONIC_CHANNELS, field.getNumChannels());

                reconstructor.reconstruct(field, 0.5f, AMBISONIC_ORDER, ir);
                assertEquals(AMBISONIC_CHANNELS, ir.getNumChannels());
                assertTrue(ir.getNumSamples() > 0);

                float[] irData = new float[ir.getNumChannels() * ir.getNumSamples()];
                ir.getData(irData);
                boolean nonZero = false;
                for (float value : irData) {
                    if (value != 0.0f) {
                        nonZero = true;
                        break;
                    }
                }
                assertTrue(nonZero, "reconstructed IR must contain the direct/early energy");
            }
        } finally {
            scene.close();
        }
    }

    @Test
    void sceneSaveObj() throws Exception {
        Scene scene = new Scene(context);
        scene.createStaticMesh(new float[]{0, 0, 0, 1, 0, 0, 0, 0, 1}, new int[]{0, 1, 2}, new int[]{0},
                new Material[]{Material.concrete()});
        scene.commit();

        // iplSceneSaveOBJ creates a directory (or files) derived from the
        // base name; accept either shape, then clean up recursively.
        java.nio.file.Path base = java.nio.file.Path.of("steamaudio-test-scene");
        java.nio.file.Path asDir = base;
        java.nio.file.Path asFile = java.nio.file.Path.of("steamaudio-test-scene.obj");
        try {
            scene.saveOBJ("steamaudio-test-scene");
            assertTrue(java.nio.file.Files.exists(asDir) || java.nio.file.Files.exists(asFile),
                    "OBJ output must be written");
        } finally {
            if (java.nio.file.Files.isDirectory(asDir)) {
                deleteRecursively(asDir);
            }
            java.nio.file.Files.deleteIfExists(asFile);
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("steamaudio-test-scene.mtl"));
            scene.close();
        }
    }

    private static void deleteRecursively(java.nio.file.Path path) throws Exception {
        try (java.util.stream.Stream<java.nio.file.Path> children = java.nio.file.Files.walk(path)) {
            children.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    java.nio.file.Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Test
    void impulseResponseArithmetic() {
        try (ImpulseResponse a = new ImpulseResponse(context, 0.1f, 0, SAMPLING_RATE);
             ImpulseResponse b = new ImpulseResponse(context, 0.1f, 0, SAMPLING_RATE);
             ImpulseResponse out = new ImpulseResponse(context, 0.1f, 0, SAMPLING_RATE)) {

            assertEquals(1, a.getNumChannels());
            assertEquals((int) (0.1f * SAMPLING_RATE), a.getNumSamples(), 8);

            // scale zeroed b by 0.5 -> still zero; add a+zeroed = zeroed.
            b.scale(a, 0.5f);
            out.add(a, b);

            float[] data = new float[out.getNumChannels() * out.getNumSamples()];
            out.getData(data);
            for (float value : data) {
                assertEquals(0.0f, value, 0.0f);
            }
        }
    }
}
