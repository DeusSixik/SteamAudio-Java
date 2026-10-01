package net.sixik.steamaudio.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.geometry.Material;
import net.sixik.steamaudio.geometry.Scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the P2 {@link ProbeBatch} baked-data reads: data size, removal
 * and reverb times.
 */
class P2ProbeBatchTest {

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
    void emptyBatchHasNoData() {
        try (ProbeBatch batch = new ProbeBatch(context)) {
            batch.addProbe(0f, 1f, 0f, 1f);
            batch.commit();

            assertEquals(0, batch.getDataSize(ProbeBatch.DATA_TYPE_REFLECTIONS, ProbeBatch.VARIATION_REVERB),
                    "an empty batch must have no baked reverb data");
        }
    }

    @Test
    void removeDataIsNoOpForEmptyBatch() {
        try (ProbeBatch batch = new ProbeBatch(context)) {
            batch.addProbe(0f, 1f, 0f, 1f);
            batch.commit();

            batch.removeData(ProbeBatch.DATA_TYPE_PATHING, ProbeBatch.VARIATION_DYNAMIC);
            batch.commit();

            assertEquals(0, batch.getDataSize(ProbeBatch.DATA_TYPE_PATHING, ProbeBatch.VARIATION_DYNAMIC));
        }
    }

    @Test
    void bakedPathingDataHasNonZeroSize() {
        // Reuse the two-room scene from PathingTest: bake pathing data and
        // verify the layer reports a non-zero size.
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
                batch.addProbeArray(probeArray);
            }
            batch.commit();

            PathBaker.bake(context, scene, batch, 1, 0.0f, 0.3f, Float.MAX_VALUE, Float.MAX_VALUE, 1);

            long size = batch.getDataSize(ProbeBatch.DATA_TYPE_PATHING, ProbeBatch.VARIATION_DYNAMIC);
            assertTrue(size > 0, "baked pathing data must have a non-zero size");

            batch.removeData(ProbeBatch.DATA_TYPE_PATHING, ProbeBatch.VARIATION_DYNAMIC);
            batch.commit();

            assertEquals(0, batch.getDataSize(ProbeBatch.DATA_TYPE_PATHING, ProbeBatch.VARIATION_DYNAMIC),
                    "after removal the layer must be gone");
        } finally {
            scene.close();
        }
    }
}
