package net.sixik.steamaudio.simulation;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.geometry.Scene;

/**
 * Bakes reflections data (convolution and/or parametric reverb) into a
 * probe batch over the geometry of a scene ({@code iplReflectionsBakerBake}).
 * <p>
 * Baking is an expensive offline operation; only one bake can be in progress
 * at any time per context. Uses the built-in CPU ray tracer.
 */
public final class ReflectionsBaker {

    /** Bake flag: bake impulse responses for convolution reverb
     * ({@code IPL_REFLECTIONSBAKEFLAGS_BAKECONVOLUTION}). */
    public static final int BAKE_CONVOLUTION = 1;

    /** Bake flag: bake parametric reverb data
     * ({@code IPL_REFLECTIONSBAKEFLAGS_BAKEPARAMETRIC}). */
    public static final int BAKE_PARAMETRIC = 2;

    private ReflectionsBaker() {
    }

    /**
     * Bakes a reflections layer into the probe batch. If the batch already
     * contains data with this identifier, it is overwritten. The identifier
     * is composed of {@code (dataType, variation)}; the endpoint influence
     * sphere is left at its default (unused for the reverb variation).
     * <p>
     * Uses Steam Audio's built-in CPU ray tracer
     * ({@code Scene.SCENE_TYPE_DEFAULT}).
     *
     * @param context             Steam Audio context
     * @param scene               committed scene containing the geometry
     * @param probeBatch          probe batch with committed probes
     * @param dataType            {@link ProbeBatch#DATA_TYPE_REFLECTIONS} (the
     *                            only data type reflections baking supports)
     * @param variation           typically {@link ProbeBatch#VARIATION_REVERB}
     * @param bakeFlags           combination of {@link #BAKE_CONVOLUTION} and
     *                            {@link #BAKE_PARAMETRIC}
     * @param numRays             number of rays traced from each probe
     * @param numBounces          number of times each ray bounces
     * @param numDiffuseSamples   number of diffuse sampling directions
     * @param simulatedDuration   IR duration used during simulation, seconds
     * @param savedDuration       IR duration saved into the batch, seconds
     * @param order               Ambisonics order of the baked IRs
     * @param irradianceMinDistance minimum source-to-surface distance used
     *                            for energy calculations, meters
     * @param numThreads          number of bake threads
     * @param rayBatchSize        rays per batch for custom ray tracers
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context, scene or batch is closed
     */
    public static void bake(Context context, Scene scene, ProbeBatch probeBatch,
                            int dataType, int variation, int bakeFlags,
                            int numRays, int numBounces, int numDiffuseSamples,
                            float simulatedDuration, float savedDuration, int order,
                            float irradianceMinDistance, int numThreads, int rayBatchSize) {
        if (!context.isOpen() || scene == null || !scene.isOpen()
                || probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("Context, Scene or ProbeBatch is closed");
        }
        nBake(context.peerForChildren(), scene.peerForChildren(), probeBatch.peerForChildren(),
                dataType, variation, bakeFlags,
                numRays, numBounces, numDiffuseSamples,
                simulatedDuration, savedDuration, order,
                irradianceMinDistance, numThreads, rayBatchSize);
    }

    /**
     * Cancels a running reflections bake ({@code iplReflectionsBakerCancelBake}).
     *
     * @param context Steam Audio context
     */
    public static void cancelBake(Context context) {
        nCancelBake(context.peerForChildren());
    }

    /**
     * Bakes reflections via {@code iplReflectionsBakerBake}. A no-op progress
     * callback is always passed (upstream bug #523 also applies here).
     *
     * @param contextPeer          opaque pointer of the context
     * @param scenePeer            opaque pointer of the scene
     * @param probeBatchPeer       opaque pointer of the probe batch
     * @param dataType             baked data type
     * @param variation            baked data variation
     * @param bakeFlags            {@code IPLReflectionsBakeFlags}
     * @param numRays              number of rays
     * @param numBounces           number of bounces
     * @param numDiffuseSamples    number of diffuse samples
     * @param simulatedDuration    simulated IR duration, seconds
     * @param savedDuration        saved IR duration, seconds
     * @param order                Ambisonics order
     * @param irradianceMinDistance minimum irradiance distance, meters
     * @param numThreads           number of threads
     * @param rayBatchSize         ray batch size
     */
    private static native void nBake(long contextPeer, long scenePeer, long probeBatchPeer,
                                     int dataType, int variation, int bakeFlags,
                                     int numRays, int numBounces, int numDiffuseSamples,
                                     float simulatedDuration, float savedDuration, int order,
                                     float irradianceMinDistance, int numThreads, int rayBatchSize);

    /**
     * Cancels baking via {@code iplReflectionsBakerCancelBake}.
     *
     * @param contextPeer opaque pointer of the context
     */
    private static native void nCancelBake(long contextPeer);
}
