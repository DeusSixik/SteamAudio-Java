package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;
import net.sixik.steamaudio.effects.DirectEffect;
import net.sixik.steamaudio.effects.ReflectionEffect;
import net.sixik.steamaudio.geometry.Material;

/**
 * Wrapper around {@code IPLSource} — a simulation source bound to a
 * specific {@link Simulator}. The source stores the position/orientation and
 * direct simulation parameters, and also produces simulation results
 * for subsequent rendering via {@link DirectEffect}.
 */
public final class Source implements AutoCloseable {

    /** Source direct simulation flag: distance attenuation
     * ({@code IPL_DIRECTSIMULATIONFLAGS_DISTANCEATTENUATION}). */
    public static final int DIRECT_SIM_DISTANCE_ATTENUATION = 1;

    /** Direct simulation flag: air absorption
     * ({@code IPL_DIRECTSIMULATIONFLAGS_AIRABSORPTION}). */
    public static final int DIRECT_SIM_AIR_ABSORPTION = 2;

    /** Direct simulation flag: directivity
     * ({@code IPL_DIRECTSIMULATIONFLAGS_DIRECTIVITY}). */
    public static final int DIRECT_SIM_DIRECTIVITY = 4;

    /** Direct simulation flag: occlusion
     * ({@code IPL_DIRECTSIMULATIONFLAGS_OCCLUSION}). */
    public static final int DIRECT_SIM_OCCLUSION = 8;

    /** Direct simulation flag: transmission; requires occlusion to be enabled
     * ({@code IPL_DIRECTSIMULATIONFLAGS_TRANSMISSION}). */
    public static final int DIRECT_SIM_TRANSMISSION = 16;

    /** Occlusion algorithm: a single ray from the listener to the source
     * ({@code IPL_OCCLUSIONTYPE_RAYCAST}). */
    public static final int OCCLUSION_RAYCAST = 0;

    /** Occlusion algorithm: volumetric, with partial occlusion
     * ({@code IPL_OCCLUSIONTYPE_VOLUMETRIC}). */
    public static final int OCCLUSION_VOLUMETRIC = 1;

    /** Opaque pointer to {@code IPLSource}; 0 means the source is closed. */
    private long peer;

    /** Opaque pointer of the parent simulator. */
    private final long simulatorPeer;

    /**
     * Creates a source via {@code iplSourceCreate}. After creation,
     * {@link #add()} and {@link Simulator#commit()} must be called.
     *
     * @param simulator       an open simulator
     * @param simulationFlags source simulation types ({@code Simulator.FLAGS_*})
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the simulator is closed
     */
    public Source(Simulator simulator, int simulationFlags) {
        if (simulator == null || !simulator.isOpen()) {
            throw new IllegalStateException("Simulator is closed");
        }
        this.simulatorPeer = simulator.peerForChildren();
        this.peer = nCreate(simulatorPeer, simulationFlags);
    }

    /**
     * Checks that the source is open.
     *
     * @return {@code true} if the source is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Adds the source to the simulator ({@code iplSourceAdd}); afterwards
     * {@link Simulator#commit()} is required.
     */
    public void add() {
        requireOpen();
        nAdd(peer, simulatorPeer);
    }

    /**
     * Removes the source from the simulator ({@code iplSourceRemove}); afterwards
     * {@link Simulator#commit()} is required.
     */
    public void remove() {
        requireOpen();
        nRemove(peer, simulatorPeer);
    }

    /**
     * Sets the source direct simulation parameters
     * ({@code iplSourceSetInputs} with {@code IPL_SIMULATIONFLAGS_DIRECT}).
     * <p>
     * Default settings are used: distance attenuation — the default
     * model (inverse distance with a 1 m plateau), air absorption —
     * the default model, directivity — omnidirectional
     * ({@code dipoleWeight = 0}). The {@code right} basis is computed
     * natively as {@code ahead × up}.
     *
     * @param directFlags         a combination of {@code DIRECT_SIM_*} flags
     * @param sourcePosition      source world coordinates
     * @param sourceAhead         source unit "ahead" vector
     * @param sourceUp            source unit "up" vector
     * @param occlusionType       the occlusion algorithm: {@link #OCCLUSION_RAYCAST}
     *                            or {@link #OCCLUSION_VOLUMETRIC}
     * @param occlusionRadius     source sphere radius for volumetric
     *                            occlusion, m
     * @param numOcclusionSamples number of point samples for volumetric occlusion
     * @param numTransmissionRays max number of surfaces for transmission
     */
    public void setDirectInputs(int directFlags, Vector3 sourcePosition, Vector3 sourceAhead, Vector3 sourceUp,
                                int occlusionType, float occlusionRadius, int numOcclusionSamples,
                                int numTransmissionRays) {
        setDirectInputs(directFlags,
                sourcePosition.x, sourcePosition.y, sourcePosition.z,
                sourceAhead.x, sourceAhead.y, sourceAhead.z,
                sourceUp.x, sourceUp.y, sourceUp.z,
                occlusionType, occlusionRadius, numOcclusionSamples, numTransmissionRays);
    }

    /**
     * Sets the direct simulation parameters without objects or allocations — the
     * primitive-based version for the hot path. See the full documentation
     * in the Vector3 overload.
     *
     * @param directFlags         a combination of {@code DIRECT_SIM_*} flags
     * @param sourceX sourceY sourceZ source position
     * @param aheadX aheadY aheadZ source "ahead" vector
     * @param upX upY upZ         source "up" vector
     * @param occlusionType       the occlusion algorithm ({@code IPLOcclusionType})
     * @param occlusionRadius     source sphere radius, m
     * @param numOcclusionSamples number of point samples for volumetric occlusion
     * @param numTransmissionRays max number of surfaces for transmission
     */
    public void setDirectInputs(int directFlags,
                                float sourceX, float sourceY, float sourceZ,
                                float aheadX, float aheadY, float aheadZ,
                                float upX, float upY, float upZ,
                                int occlusionType, float occlusionRadius, int numOcclusionSamples,
                                int numTransmissionRays) {
        requireOpen();
        nSetDirectInputs(peer, directFlags,
                sourceX, sourceY, sourceZ,
                aheadX, aheadY, aheadZ,
                upX, upY, upZ,
                occlusionType, occlusionRadius, numOcclusionSamples, numTransmissionRays);
    }

    /**
     * Sets the source reflections simulation parameters
     * ({@code iplSourceSetInputs} with {@code IPL_SIMULATIONFLAGS_REFLECTIONS}).
     * <p>
     * Real-time simulation (no baked data): the reverb decay scales
     * ({@code reverbScale}) are applied to the RT60 in each band; 1.0 —
     * use the simulated values unchanged. Default hybrid
     * settings: transition time 1.0 s, overlap 0.25.
     *
     * @param reverbScale an array of {@value Material#NUM_BANDS} RT60 scales
     */
    public void setReflectionsInputs(float[] reverbScale) {
        requireOpen();
        if (reverbScale.length < Material.NUM_BANDS) {
            throw new IllegalArgumentException("reverbScale must contain " + Material.NUM_BANDS + " values");
        }
        nSetReflectionsInputs(peer, reverbScale);
    }

    /**
     * Retrieves the direct simulation results
     * ({@code iplSourceGetOutputs} with {@code IPL_SIMULATIONFLAGS_DIRECT})
     * and writes them into an array without allocations. Array format:
     * {@code [distanceAttenuation, airAbs0, airAbs1, airAbs2, directivity,
     * occlusion, trans0, trans1, trans2]} — 9 floats.
     *
     * @param out an array of at least 9 elements
     * @throws IllegalStateException if the source is closed
     */
    public void getDirectOutputsInto(float[] out) {
        requireOpen();
        if (out.length < 9) {
            throw new IllegalArgumentException("out must contain at least 9 elements");
        }
        nGetDirectOutputs(peer, out);
    }

    /**
     * Checks that the source is open.
     *
     * @throws IllegalStateException if the source is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("Source is closed");
        }
    }

    /**
     * Returns the source opaque pointer for child objects of the package
     * (for example, {@code ReflectionEffect}).
     *
     * @return an opaque pointer to {@code IPLSource}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the source ({@code iplSourceRelease}). Before this, call
     * {@link #remove()} if the source was added to the simulator. Calling
     * again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Creates a source via {@code iplSourceCreate}.
     *
     * @param simulatorPeer   the simulator opaque pointer
     * @param simulationFlags simulation types ({@code IPLSimulationFlags})
     * @return an opaque pointer to the created source
     */
    private static native long nCreate(long simulatorPeer, int simulationFlags);

    /**
     * Adds the source to the simulator via {@code iplSourceAdd}.
     *
     * @param peer          the source opaque pointer
     * @param simulatorPeer the simulator opaque pointer
     */
    private static native void nAdd(long peer, long simulatorPeer);

    /**
     * Removes the source from the simulator via {@code iplSourceRemove}.
     *
     * @param peer          the source opaque pointer
     * @param simulatorPeer the simulator opaque pointer
     */
    private static native void nRemove(long peer, long simulatorPeer);

    /**
     * Sets the direct inputs via {@code iplSourceSetInputs}.
     *
     * @param peer                the source opaque pointer
     * @param directFlags         direct simulation flags
     * @param sourceX sourceY sourceZ source position
     * @param aheadX aheadY aheadZ source "ahead" vector
     * @param upX upY upZ source "up" vector
     * @param occlusionType       the occlusion algorithm ({@code IPLOcclusionType})
     * @param occlusionRadius     source sphere radius
     * @param numOcclusionSamples number of occlusion samples
     * @param numTransmissionRays number of transmission rays
     */
    private static native void nSetDirectInputs(long peer, int directFlags,
                                                float sourceX, float sourceY, float sourceZ,
                                                float aheadX, float aheadY, float aheadZ,
                                                float upX, float upY, float upZ,
                                                int occlusionType, float occlusionRadius,
                                                int numOcclusionSamples, int numTransmissionRays);

    /**
     * Sets the reflections inputs via {@code iplSourceSetInputs}.
     *
     * @param peer        the source opaque pointer
     * @param reverbScale an array of 3 RT60 scales
     */
    private static native void nSetReflectionsInputs(long peer, float[] reverbScale);

    /**
     * Sets the pathing inputs via {@code iplSourceSetInputs}.
     *
     * @param peer          the source opaque pointer
     * @param probeBatchPeer the probe batch opaque pointer
     * @param pathingOrder  Ambisonic order of the paths
     * @param visRadius     visibility sphere radius, m
     * @param visThreshold  visibility threshold (0..1)
     * @param visRange      maximum visibility distance, m
     */
    private static native void nSetPathingInputs(long peer, long probeBatchPeer, int pathingOrder,
                                                 float visRadius, float visThreshold, float visRange);

    /**
     * Sets the source pathing simulation parameters
     * ({@code iplSourceSetInputs} with {@code IPL_SIMULATIONFLAGS_PATHING}).
     * <p>
     * Uses the data baked into the probe batch; when validation is enabled,
     * the baked paths are checked for visibility, and blocked paths
     * are rebuilt in real time.
     *
     * @param probeBatch   an open probe batch with baked pathing data
     * @param pathingOrder Ambisonic order of the path directivity representation
     * @param visRadius    probe visibility sphere radius, m
     * @param visThreshold threshold on the fraction of unblocked visibility rays (0..1)
     * @param visRange     maximum distance between mutually visible
     *                     probes, m
     */
    public void setPathingInputs(ProbeBatch probeBatch, int pathingOrder,
                                 float visRadius, float visThreshold, float visRange) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nSetPathingInputs(peer, probeBatch.peerForChildren(), pathingOrder, visRadius, visThreshold, visRange);
    }

    /**
     * Retrieves the direct results via {@code iplSourceGetOutputs} and
     * copies them into an array.
     *
     * @param peer the source opaque pointer
     * @param out  the destination array (at least 9 elements)
     */
    private static native void nGetDirectOutputs(long peer, float[] out);

    /**
     * Releases the source via {@code iplSourceRelease}.
     *
     * @param peer the source opaque pointer
     */
    private static native void nRelease(long peer);
}
