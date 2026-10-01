package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.geometry.Scene;

/**
 * Wrapper around {@code IPLProbeArray} — a temporary array of probes
 * generated from the scene geometry. Probes are spheres of influence that
 * store baked data. The array is added to a {@link ProbeBatch}
 * for subsequent reflections/pathing baking.
 */
public final class ProbeArray implements AutoCloseable {

    /** A probe at the centroid of every triangle of the geometry
     * ({@code IPL_PROBEGENERATIONTYPE_CENTROID}). */
    public static final int GENERATION_CENTROID = 0;

    /** A uniform grid of probes over a horizontal surface
     * ({@code IPL_PROBEGENERATIONTYPE_UNIFORMFLOOR}). */
    public static final int GENERATION_UNIFORM_FLOOR = 1;

    /** Opaque pointer to {@code IPLProbeArray}; 0 means the object is closed. */
    private long peer;

    /**
     * Creates an empty probe array via {@code iplProbeArrayCreate}.
     *
     * @param context the Steam Audio context
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public ProbeArray(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Checks that the array is open.
     *
     * @return {@code true} if the array is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Generates probes from the scene geometry
     * ({@code iplProbeArrayGenerateProbes}).
     *
     * @param scene     an open committed scene
     * @param type      the generation algorithm: {@link #GENERATION_CENTROID}
     *                  or {@link #GENERATION_UNIFORM_FLOOR}
     * @param spacing   distance between neighboring probes, m (only
     *                  {@code GENERATION_UNIFORM_FLOOR})
     * @param height    height above the floor, m (only
     *                  {@code GENERATION_UNIFORM_FLOOR})
     * @param transform a row-major 4x4 matrix mapping the unit
     *                  cube {@code [0..1]^3} to the generation volume; {@code null} —
     *                  the identity matrix
     * @throws IllegalStateException if the array or the scene is closed
     */
    public void generateProbes(Scene scene, int type, float spacing, float height, float[] transform) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("ProbeArray or Scene is closed");
        }
        if (transform != null && transform.length < 16) {
            throw new IllegalArgumentException("transform must contain 16 elements");
        }
        nGenerateProbes(peer, scene.peerForChildren(), type, spacing, height, transform);
    }

    /**
     * Returns the number of generated probes
     * ({@code iplProbeArrayGetNumProbes}).
     *
     * @return the number of probes
     */
    public int getNumProbes() {
        requireOpen();
        return nGetNumProbes(peer);
    }

    /**
     * Returns the sphere (center + radius) of the probe at the given index
     * ({@code iplProbeArrayGetProbe}).
     *
     * @param index index of the probe
     * @param out   array of at least 4 elements; receives
     *              {@code (centerX, centerY, centerZ, radius)}
     */
    public void getProbe(int index, float[] out) {
        requireOpen();
        if (out.length < 4) {
            throw new IllegalArgumentException("out must contain at least 4 elements");
        }
        nGetProbe(peer, index, out);
    }

    /**
     * Checks that the array is open.
     *
     * @throws IllegalStateException if the array is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ProbeArray is closed");
        }
    }

    /**
     * Releases the array ({@code iplProbeArrayRelease}). Calling again
     * is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Returns the array opaque pointer for child objects of the package.
     *
     * @return an opaque pointer to {@code IPLProbeArray}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Creates an empty array via {@code iplProbeArrayCreate}.
     *
     * @param contextPeer the context opaque pointer
     * @return an opaque pointer to the created array
     */
    private static native long nCreate(long contextPeer);

    /**
     * Generates probes via {@code iplProbeArrayGenerateProbes}.
     *
     * @param peer        the array opaque pointer
     * @param scenePeer   the scene opaque pointer
     * @param type        the generation algorithm ({@code IPLProbeGenerationType})
     * @param spacing     distance between probes, m
     * @param height      height above the floor, m
     * @param transform   an array of 16 floats (row-major 4x4) or {@code null}
     */
    private static native void nGenerateProbes(long peer, long scenePeer, int type,
                                               float spacing, float height, float[] transform);

    /**
     * Returns the number of probes via {@code iplProbeArrayGetNumProbes}.
     *
     * @param peer the array opaque pointer
     * @return the number of probes
     */
    private static native int nGetNumProbes(long peer);

    /**
     * Returns a probe sphere via {@code iplProbeArrayGetProbe}.
     *
     * @param peer  opaque pointer of the array
     * @param index probe index
     * @param out   destination array (at least 4 elements)
     */
    private static native void nGetProbe(long peer, int index, float[] out);

    /**
     * Releases the array via {@code iplProbeArrayRelease}.
     *
     * @param peer the array opaque pointer
     */
    private static native void nRelease(long peer);
}
