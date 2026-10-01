package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.geometry.Scene;

/**
 * Pathing data baker: computes paths between all pairs of probes
 * in a probe batch against the scene geometry and stores the result in the batch
 * ({@code iplPathBakerBake}). Baking is an expensive non-real-time operation;
 * only one bake can run at a time per context.
 */
public final class PathBaker {

    private PathBaker() {
    }

    /**
     * Bakes a pathing data layer into the probe batch. If the batch already
     * contains data with this identifier, it will be overwritten.
     * <p>
     * Layer identifier: {@code type = PATHING, variation = DYNAMIC} —
     * the standard layout for real-time pathing simulation.
     *
     * @param context    the Steam Audio context
     * @param scene      an open committed scene
     * @param probeBatch an open probe batch with probes
     * @param numSamples the number of point samples around a probe when checking
     *                   mutual visibility (numSamples × numSamples rays
     *                   per pair of probes)
     * @param radius     probe sphere radius during visibility checks, m
     * @param threshold  threshold on the fraction of unblocked rays for a pair
     *                   of probes to be considered mutually visible (0..1)
     * @param visRange   maximum mutual visibility distance between probes, m
     * @param pathRange  maximum path length between probes, m
     * @param numThreads number of baking threads
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context, scene or batch is closed
     */
    public static void bake(Context context, Scene scene, ProbeBatch probeBatch,
                            int numSamples, float radius, float threshold,
                            float visRange, float pathRange, int numThreads) {
        if (!context.isOpen() || scene == null || !scene.isOpen()
                || probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("Context, Scene or ProbeBatch is closed");
        }
        nBake(context.peerForChildren(), scene.peerForChildren(), probeBatch.peerForChildren(),
                numSamples, radius, threshold, visRange, pathRange, numThreads);
    }

    /**
     * Cancels a running pathing data bake
     * ({@code iplPathBakerCancelBake}).
     *
     * @param context the Steam Audio context
     */
    public static void cancelBake(Context context) {
        nCancelBake(context.peerForChildren());
    }

    /**
     * Bakes pathing data via {@code iplPathBakerBake}.
     *
     * @param contextPeer    the context opaque pointer
     * @param scenePeer      the scene opaque pointer
     * @param probeBatchPeer the probe batch opaque pointer
     * @param numSamples     number of visibility point samples
     * @param radius         probe sphere radius, m
     * @param threshold      visibility threshold (0..1)
     * @param visRange       mutual visibility distance, m
     * @param pathRange      maximum path length, m
     * @param numThreads     number of threads
     */
    private static native void nBake(long contextPeer, long scenePeer, long probeBatchPeer,
                                     int numSamples, float radius, float threshold,
                                     float visRange, float pathRange, int numThreads);

    /**
     * Cancels the bake via {@code iplPathBakerCancelBake}.
     *
     * @param contextPeer the context opaque pointer
     */
    private static native void nCancelBake(long contextPeer);
}
