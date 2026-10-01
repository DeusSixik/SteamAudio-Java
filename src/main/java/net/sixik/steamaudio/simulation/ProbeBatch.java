package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SerializedObject;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper around {@code IPLProbeBatch} — a collection of probes that stores
 * baked data layers (reflections, pathing). In real-time mode, simulations
 * use the data from the probe batch attached to the {@link Simulator}.
 */
public final class ProbeBatch implements AutoCloseable {

    /** Opaque pointer to {@code IPLProbeBatch}; 0 means the object is closed. */
    private long peer;

    /**
     * Creates an empty probe batch via {@code iplProbeBatchCreate}.
     *
     * @param context the Steam Audio context
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public ProbeBatch(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Checks that the batch is open.
     *
     * @return {@code true} if the batch is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Adds all probes of the array to the batch
     * ({@code iplProbeBatchAddProbeArray}).
     *
     * @param probeArray an open probe array
     */
    public void addProbeArray(ProbeArray probeArray) {
        requireOpen();
        if (probeArray == null || !probeArray.isOpen()) {
            throw new IllegalStateException("ProbeArray is closed");
        }
        nAddProbeArray(peer, probeArray.peerForChildren());
    }

    /**
     * Adds a single probe to the batch ({@code iplProbeBatchAddProbe}).
     *
     * @param centerX centerY centerZ probe center coordinates
     * @param radius  probe sphere of influence radius, m
     */
    public void addProbe(float centerX, float centerY, float centerZ, float radius) {
        requireOpen();
        nAddProbe(peer, centerX, centerY, centerZ, radius);
    }

    /**
     * Commits batch changes ({@code iplProbeBatchCommit}); mandatory
     * after adding/removing probes and before baking.
     */
    public void commit() {
        requireOpen();
        nCommit(peer);
    }

    /**
     * Returns the number of probes in the batch ({@code iplProbeBatchGetNumProbes}).
     *
     * @return the number of probes
     */
    public int getNumProbes() {
        requireOpen();
        return nGetNumProbes(peer);
    }

    /**
     * Saves the probe batch (including baked data) into a serialized
     * object ({@code iplProbeBatchSave}).
     *
     * @param destination an open serialized object
     */
    public void save(SerializedObject destination) {
        requireOpen();
        if (!destination.isOpen()) {
            throw new IllegalStateException("SerializedObject is closed");
        }
        nSave(peer, destination.peerForChildren());
    }

    /**
     * Loads a probe batch from a serialized object
     * ({@code iplProbeBatchLoad}).
     *
     * @param context the Steam Audio context
     * @param source  serialized object with the batch data
     * @return the loaded probe batch
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public static ProbeBatch load(Context context, SerializedObject source) {
        if (!context.isOpen() || !source.isOpen()) {
            throw new IllegalStateException("Context or SerializedObject is closed");
        }
        return new ProbeBatch(nLoad(context.peerForChildren(), source.peerForChildren()));
    }

    /**
     * Checks that the batch is open.
     *
     * @throws IllegalStateException if the batch is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
    }

    /**
     * Releases the batch ({@code iplProbeBatchRelease}). Calling again
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
     * Returns the batch opaque pointer for child objects of the package.
     *
     * @return an opaque pointer to {@code IPLProbeBatch}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Creates a wrapper around an existing handle (used
     * in {@link #load}).
     *
     * @param peer an opaque pointer to {@code IPLProbeBatch}
     */
    private ProbeBatch(long peer) {
        this.peer = peer;
    }

    /**
     * Creates an empty batch via {@code iplProbeBatchCreate}.
     *
     * @param contextPeer the context opaque pointer
     * @return an opaque pointer to the created batch
     */
    private static native long nCreate(long contextPeer);

    /**
     * Adds a probe array via {@code iplProbeBatchAddProbeArray}.
     *
     * @param peer           the batch opaque pointer
     * @param probeArrayPeer the probe array opaque pointer
     */
    private static native void nAddProbeArray(long peer, long probeArrayPeer);

    /**
     * Adds a single probe via {@code iplProbeBatchAddProbe}.
     *
     * @param peer the batch opaque pointer
     * @param centerX centerY centerZ probe center
     * @param radius sphere of influence radius, m
     */
    private static native void nAddProbe(long peer, float centerX, float centerY, float centerZ, float radius);

    /**
     * Commits the batch via {@code iplProbeBatchCommit}.
     *
     * @param peer the batch opaque pointer
     */
    private static native void nCommit(long peer);

    /**
     * Returns the number of probes via {@code iplProbeBatchGetNumProbes}.
     *
     * @param peer the batch opaque pointer
     * @return the number of probes
     */
    private static native int nGetNumProbes(long peer);

    /**
     * Saves the batch via {@code iplProbeBatchSave}.
     *
     * @param peer             the batch opaque pointer
     * @param destinationPeer  the serialized object opaque pointer
     */
    private static native void nSave(long peer, long destinationPeer);

    /**
     * Loads the batch via {@code iplProbeBatchLoad}.
     *
     * @param contextPeer the context opaque pointer
     * @param sourcePeer  the serialized object opaque pointer
     * @return an opaque pointer to the loaded batch
     */
    private static native long nLoad(long contextPeer, long sourcePeer);

    /**
     * Releases the batch via {@code iplProbeBatchRelease}.
     *
     * @param peer the batch opaque pointer
     */
    private static native void nRelease(long peer);
}
