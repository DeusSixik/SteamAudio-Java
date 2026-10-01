package net.sixik.steamaudio.simulation;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.effects.ReflectionEffect;
import net.sixik.steamaudio.geometry.Scene;

/**
 * Preset for real-time simulation on low-end CPUs (e.g. Intel i5-3470 class,
 * pre-AVX2, 4 cores): parametric reverb instead of convolution, a small ray
 * budget, and raycast occlusion recommendations.
 * <p>
 * Usage:
 * <pre>{@code
 *     Simulator simulator = LowSpecSimulator.create(context, 48000, 1024);
 *     simulator.setSharedInputs(...,
 *             LowSpecSimulator.NUM_RAYS, LowSpecSimulator.NUM_BOUNCES,
 *             0.5f, 1, 1.0f);
 *     // sources: Source.OCCLUSION_RAYCAST is the cheap occlusion algorithm
 * }</pre>
 * <p>
 * For rendering baked or simulated reflections use
 * {@code ReflectionEffect.TYPE_PARAMETRIC} (roughly an order of magnitude
 * cheaper than convolution); if convolution reverb is required, render it
 * through a {@code ReflectionMixer} on a separate thread rather than per
 * source.
 */
public final class LowSpecSimulator {

    /** Real-time ray budget that fits weak 4-core CPUs
     * (vs. the 4096-ray default). */
    public static final int NUM_RAYS = 64;

    /** Bounce count that keeps reflection simulation cheap. */
    public static final int NUM_BOUNCES = 2;

    /** Ambisonics order used for IRs: order 1 = 4 channels, the cheapest
     * directional representation. */
    public static final int AMBISONICS_ORDER = 1;

    private LowSpecSimulator() {
    }

    /**
     * Creates a simulator preset for low-end CPUs: DIRECT + REFLECTIONS
     * simulation flags with parametric reflections rendering, and a small
     * internal reflection budget.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size, samples (256..1024 recommended)
     * @return the configured simulator
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public static Simulator create(Context context, int samplingRate, int frameSize) {
        return new Simulator(context,
                Simulator.FLAGS_DIRECT | Simulator.FLAGS_REFLECTIONS,
                Scene.SCENE_TYPE_DEFAULT,
                ReflectionEffect.TYPE_PARAMETRIC,
                samplingRate, frameSize, null, null, null);
    }
}
