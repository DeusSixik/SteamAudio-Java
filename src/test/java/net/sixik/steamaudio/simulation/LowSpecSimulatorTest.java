package net.sixik.steamaudio.simulation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.Vector3;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the {@link LowSpecSimulator} preset: the simulator must be
 * functional for direct + parametric reflections simulation with a modest
 * ray budget.
 */
class LowSpecSimulatorTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context(Context.SIMD_LEVEL_AUTO, 0);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void presetRunsDirectSimulation() {
        try (Simulator simulator = LowSpecSimulator.create(context, 48000, 1024)) {
            assertTrue(simulator.isOpen());

            simulator.setSharedInputs(
                    Simulator.FLAGS_DIRECT | Simulator.FLAGS_REFLECTIONS,
                    new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                    LowSpecSimulator.NUM_RAYS, LowSpecSimulator.NUM_BOUNCES,
                    0.5f, LowSpecSimulator.AMBISONICS_ORDER, 1.0f);

            try (Source source = new Source(simulator, Simulator.FLAGS_DIRECT)) {
                source.add();
                simulator.commit();

                source.setDirectInputs(Source.DIRECT_SIM_DISTANCE_ATTENUATION
                                | Source.DIRECT_SIM_OCCLUSION,
                        new Vector3(3, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                        Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);
                simulator.runDirect();

                float[] outputs = new float[9];
                source.getDirectOutputsInto(outputs);
                assertTrue(outputs[0] > 0.0f && outputs[0] < 1.0f,
                        "distance attenuation must be applied");
            }
        }
    }
}
