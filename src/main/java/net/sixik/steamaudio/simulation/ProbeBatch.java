package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SerializedObject;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.geometry.Material;

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
     * Baked data layer identifier: reflections
     * ({@code IPL_BAKEDDATATYPE_REFLECTIONS}).
     */
    public static final int DATA_TYPE_REFLECTIONS = 0;

    /**
     * Baked data layer identifier: pathing ({@code IPL_BAKEDDATATYPE_PATHING}).
     */
    public static final int DATA_TYPE_PATHING = 1;

    /**
     * Baked data variation: reverb ({@code IPL_BAKEDDATAVARIATION_REVERB}).
     */
    public static final int VARIATION_REVERB = 0;

    /**
     * Baked data variation: static source
     * ({@code IPL_BAKEDDATAVARIATION_STATICSOURCE}).
     */
    public static final int VARIATION_STATIC_SOURCE = 1;

    /**
     * Baked data variation: static listener
     * ({@code IPL_BAKEDDATAVARIATION_STATICLISTENER}).
     */
    public static final int VARIATION_STATIC_LISTENER = 2;

    /**
     * Baked data variation: dynamic ({@code IPL_BAKEDDATAVARIATION_DYNAMIC}).
     */
    public static final int VARIATION_DYNAMIC = 3;

    /**
     * Returns the size in bytes of a specific baked data layer
     * ({@code iplProbeBatchGetDataSize}). Layers are identified by
     * {@code (dataType, variation)}; the endpoint influence sphere is unused
     * for the reverb and dynamic variations and left at its default.
     *
     * @param dataType  {@link #DATA_TYPE_REFLECTIONS} or {@link #DATA_TYPE_PATHING}
     * @param variation {@link #VARIATION_REVERB}, {@link #VARIATION_STATIC_SOURCE},
     *                  {@link #VARIATION_STATIC_LISTENER} or {@link #VARIATION_DYNAMIC}
     * @return size of the baked data layer, in bytes (0 if absent)
     */
    public long getDataSize(int dataType, int variation) {
        requireOpen();
        return nGetDataSize(peer, dataType, variation);
    }

    /**
     * Removes a baked data layer from the batch
     * ({@code iplProbeBatchRemoveData}); call {@link #commit()} afterwards.
     *
     * @param dataType  {@link #DATA_TYPE_REFLECTIONS} or {@link #DATA_TYPE_PATHING}
     * @param variation one of the {@code VARIATION_*} constants
     */
    public void removeData(int dataType, int variation) {
        requireOpen();
        nRemoveData(peer, dataType, variation);
    }

    /**
     * Retrieves the parametric reverb times (RT60 per frequency band) stored
     * at a probe for a baked layer ({@code iplProbeBatchGetReverb}). Only
     * meaningful for baked reflections data.
     *
     * @param dataType  {@link #DATA_TYPE_REFLECTIONS} or {@link #DATA_TYPE_PATHING}
     * @param variation one of the {@code VARIATION_*} constants
     * @param probeIndex index of the probe within the batch
     * @param out array of at least {@code Material.NUM_BANDS} elements;
     *            receives the RT60 values
     */
    public void getReverb(int dataType, int variation, int probeIndex, float[] out) {
        requireOpen();
        if (out.length < Material.NUM_BANDS) {
            throw new IllegalArgumentException("out must contain at least "
                    + Material.NUM_BANDS + " elements");
        }
        nGetReverb(peer, dataType, variation, probeIndex, out);
    }

    /**
     * Retrieves the baked energy field stored at a probe for a baked layer
     * ({@code iplProbeBatchGetEnergyField}); the data is copied into the
     * destination energy field, which must have matching dimensions.
     *
     * @param dataType   {@link #DATA_TYPE_REFLECTIONS} or {@link #DATA_TYPE_PATHING}
     * @param variation  one of the {@code VARIATION_*} constants
     * @param probeIndex index of the probe within the batch
     * @param out        the energy field to copy into
     */
    public void getEnergyField(int dataType, int variation, int probeIndex, EnergyField out) {
        requireOpen();
        if (out == null || !out.isOpen()) {
            throw new IllegalStateException("Destination EnergyField is closed");
        }
        nGetEnergyField(peer, dataType, variation, probeIndex, out.peerForChildren());
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

    /**
     * Returns the baked layer size via {@code iplProbeBatchGetDataSize}.
     *
     * @param peer      opaque pointer of the batch
     * @param dataType  baked data type ({@code IPLBakedDataType})
     * @param variation baked data variation ({@code IPLBakedDataVariation})
     * @return size in bytes
     */
    private static native long nGetDataSize(long peer, int dataType, int variation);

    /**
     * Removes a baked layer via {@code iplProbeBatchRemoveData}.
     *
     * @param peer      opaque pointer of the batch
     * @param dataType  baked data type ({@code IPLBakedDataType})
     * @param variation baked data variation ({@code IPLBakedDataVariation})
     */
    private static native void nRemoveData(long peer, int dataType, int variation);

    /**
     * Retrieves reverb times via {@code iplProbeBatchGetReverb}.
     *
     * @param peer       opaque pointer of the batch
     * @param dataType   baked data type ({@code IPLBakedDataType})
     * @param variation  baked data variation ({@code IPLBakedDataVariation})
     * @param probeIndex index of the probe
     * @param out        destination array (at least {@code IPL_NUM_BANDS})
     */
    private static native void nGetReverb(long peer, int dataType, int variation, int probeIndex, float[] out);

    /**
     * Retrieves a baked energy field via {@code iplProbeBatchGetEnergyField}.
     *
     * @param peer            opaque pointer of the batch
     * @param dataType        baked data type ({@code IPLBakedDataType})
     * @param variation       baked data variation ({@code IPLBakedDataVariation})
     * @param probeIndex      index of the probe
     * @param energyFieldPeer opaque pointer of the destination energy field
     */
    private static native void nGetEnergyField(long peer, int dataType, int variation, int probeIndex,
                                               long energyFieldPeer);
}
