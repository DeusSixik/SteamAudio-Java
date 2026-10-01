package net.sixik.steamaudio.simulation;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;
import net.sixik.steamaudio.effects.ReflectionEffect;
import net.sixik.steamaudio.geometry.Scene;

/**
 * Wrapper around {@code IPLSimulator} — the object that manages Steam Audio
 * simulations (direct/reflections/pathing) for all added sources.
 * <p>
 * The simulator is created with {@code FLAGS_*} flags; the scene type and
 * the reflections effect type are fixed for the lifetime of the object.
 */
public final class Simulator implements AutoCloseable {

    /** Enable the direct simulation ({@code IPL_SIMULATIONFLAGS_DIRECT}). */
    public static final int FLAGS_DIRECT = 1;

    /** Enable the reflections simulation ({@code IPL_SIMULATIONFLAGS_REFLECTIONS}). */
    public static final int FLAGS_REFLECTIONS = 2;

    /** Enable the pathing simulation ({@code IPL_SIMULATIONFLAGS_PATHING}). */
    public static final int FLAGS_PATHING = 4;

    /** Opaque pointer to {@code IPLSimulator}; 0 means the simulator is closed. */
    private long peer;

    /**
     * Creates a simulator via {@code iplSimulatorCreate} with settings
     * tuned for direct simulation.
     * <p>
     * Other settings are fixed: scene — built-in ray tracer
     * ({@code SCENE_TYPE_DEFAULT}), reflections type — convolution, up to 256
     * occlusion samples, 4096 rays, 2 s IR, Ambisonic order 1,
     * up to 8 sources, 1 thread.
     *
     * @param context         the Steam Audio context
     * @param simulationFlags simulation types ({@code FLAGS_*}); for
     *                        direct-only use {@link #FLAGS_DIRECT}
     * @param samplingRate    sampling rate, Hz
     * @param frameSize       frame size in samples
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public Simulator(Context context, int simulationFlags, int samplingRate, int frameSize) {
        this(context, simulationFlags, samplingRate, frameSize,
                ReflectionEffect.TYPE_CONVOLUTION);
    }

    /**
     * Creates a simulator via {@code iplSimulatorCreate} with an explicit
     * reflections effect type.
     *
     * @param context         the Steam Audio context
     * @param simulationFlags simulation types ({@code FLAGS_*})
     * @param samplingRate    sampling rate, Hz
     * @param frameSize       frame size in samples
     * @param reflectionType  the reflections effect type
     *                        ({@code ReflectionEffect.TYPE_*})
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public Simulator(Context context, int simulationFlags, int samplingRate, int frameSize, int reflectionType) {
        this(context, simulationFlags, Scene.SCENE_TYPE_DEFAULT, reflectionType,
                samplingRate, frameSize, null, null, null);
    }

    /**
     * Creates a simulator with explicit scene type and GPU devices via
     * {@code iplSimulatorCreate}. Other settings are fixed: up to 256
     * occlusion samples, 4096 rays, 2 s IR, Ambisonics order 1, 8 sources,
     * 1 thread.
     *
     * @param context         Steam Audio context
     * @param simulationFlags simulation types ({@code FLAGS_*})
     * @param sceneType       scene type used for simulation
     *                        ({@code Scene.SCENE_TYPE_*}); must match the
     *                        scene passed to {@link #setScene}
     * @param reflectionType  reflections effect type
     *                        ({@code ReflectionEffect.TYPE_*})
     * @param samplingRate    sampling rate, Hz
     * @param frameSize       frame size, samples
     * @param openCLDevice    OpenCL device; required for Radeon Rays scenes
     *                        and TAN reflections, {@code null} otherwise
     * @param radeonRaysDevice Radeon Rays device; required for Radeon Rays
     *                        scenes, {@code null} otherwise
     * @param tanDevice       TrueAudio Next device; required for TAN
     *                        reflections, {@code null} otherwise
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public Simulator(Context context, int simulationFlags, int sceneType, int reflectionType,
                     int samplingRate, int frameSize,
                     net.sixik.steamaudio.gpu.OpenCLDevice openCLDevice,
                     net.sixik.steamaudio.gpu.RadeonRaysDevice radeonRaysDevice,
                     net.sixik.steamaudio.gpu.TrueAudioNextDevice tanDevice) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), simulationFlags, sceneType, reflectionType,
                256, 4096, 32, 2.0f, 1, 8, 1, 8, 8, samplingRate, frameSize,
                openCLDevice == null ? 0 : openCLDevice.peerForChildren(),
                radeonRaysDevice == null ? 0 : radeonRaysDevice.peerForChildren(),
                tanDevice == null ? 0 : tanDevice.peerForChildren());
    }

    /**
     * Checks that the simulator is open.
     *
     * @return {@code true} if the simulator is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Sets the scene for simulations ({@code iplSimulatorSetScene}).
     * {@link #commit()} is required afterwards.
     *
     * @param scene an open scene
     */
    public void setScene(Scene scene) {
        requireOpen();
        if (scene == null || !scene.isOpen()) {
            throw new IllegalStateException("Scene is closed");
        }
        nSetScene(peer, scene.peerForChildren());
    }

    /**
     * Sets shared simulation inputs not tied to specific sources
     * ({@code iplSimulatorSetSharedInputs}).
     * <p>
     * The listener position and orientation are specified by three vectors; the
     * {@code right} basis is computed natively as {@code ahead × up}.
     * Parameters unrelated to the direct simulation (rays/bounces/duration/order)
     * are used by the reflections simulation.
     *
     * @param flags                 simulation types ({@code FLAGS_*})
     * @param listenerPosition      listener world coordinates
     * @param listenerAhead         listener unit "ahead" vector
     * @param listenerUp            listener unit "up" vector
     * @param numRays               number of rays (reflections)
     * @param numBounces            number of ray bounces (reflections)
     * @param duration              IR duration in seconds (reflections)
     * @param order                 IR Ambisonic order (reflections)
     * @param irradianceMinDistance minimum distance for surface
     *                              irradiance calculation (reflections)
     */
    public void setSharedInputs(int flags, Vector3 listenerPosition, Vector3 listenerAhead, Vector3 listenerUp,
                                int numRays, int numBounces, float duration, int order,
                                float irradianceMinDistance) {
        setSharedInputs(flags,
                listenerPosition.x, listenerPosition.y, listenerPosition.z,
                listenerAhead.x, listenerAhead.y, listenerAhead.z,
                listenerUp.x, listenerUp.y, listenerUp.z,
                numRays, numBounces, duration, order, irradianceMinDistance);
    }

    /**
     * Sets shared simulation inputs without objects or allocations — the
     * primitive-based version for the hot path. See the full documentation
     * in the Vector3 overload.
     *
     * @param flags                 simulation types ({@code FLAGS_*})
     * @param listenerX listenerY listenerZ listener position
     * @param aheadX aheadY aheadZ  listener "ahead" vector
     * @param upX upY upZ           listener "up" vector
     * @param numRays               number of rays (reflections)
     * @param numBounces            number of ray bounces (reflections)
     * @param duration              IR duration in seconds (reflections)
     * @param order                 IR Ambisonic order (reflections)
     * @param irradianceMinDistance minimum irradiance distance (reflections)
     */
    public void setSharedInputs(int flags,
                                float listenerX, float listenerY, float listenerZ,
                                float aheadX, float aheadY, float aheadZ,
                                float upX, float upY, float upZ,
                                int numRays, int numBounces, float duration, int order,
                                float irradianceMinDistance) {
        requireOpen();
        nSetSharedInputs(peer, flags,
                listenerX, listenerY, listenerZ,
                aheadX, aheadY, aheadZ,
                upX, upY, upZ,
                numRays, numBounces, duration, order, irradianceMinDistance);
    }

    /**
     * Commits scene/source list changes
     * ({@code iplSimulatorCommit}).
     */
    public void commit() {
        requireOpen();
        nCommit(peer);
    }

    /**
     * Runs the direct simulation for all sources
     * ({@code iplSimulatorRunDirect}).
     * <p>
     * Do not call this method from the audio thread if occlusion
     * and/or transmission are enabled.
     */
    public void runDirect() {
        requireOpen();
        nRunDirect(peer);
    }

    /**
     * Runs the reflections simulation for all sources
     * ({@code iplSimulatorRunReflections}).
     * <p>
     * CPU-intensive operation; call it from a dedicated thread, not from
     * the audio thread or the game update thread. The simulation requires:
     * a set scene, shared inputs (numRays/numBounces/duration/order)
     * and source reflections inputs (see
     * {@link Source#setReflectionsInputs}).
     */
    public void runReflections() {
        requireOpen();
        nRunReflections(peer);
    }

    /**
     * Adds a probe batch for use in simulations
     * ({@code iplSimulatorAddProbeBatch}); {@link #commit()} is required
     * afterwards. Needed for reflections with baked data and for
     * pathing simulation.
     *
     * @param probeBatch an open probe batch
     */
    public void addProbeBatch(ProbeBatch probeBatch) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nAddProbeBatch(peer, probeBatch.peerForChildren());
    }

    /**
     * Removes a probe batch from the simulator ({@code iplSimulatorRemoveProbeBatch});
     * {@link #commit()} is required afterwards.
     *
     * @param probeBatch an open probe batch
     */
    public void removeProbeBatch(ProbeBatch probeBatch) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nRemoveProbeBatch(peer, probeBatch.peerForChildren());
    }

    /**
     * Runs the pathing simulation for all sources
     * ({@code iplSimulatorRunPathing}).
     * <p>
     * CPU-intensive operation; call it from a dedicated thread. Requires:
     * a scene, a probe batch (see {@link #addProbeBatch}) and source pathing
     * inputs (see {@link Source#setPathingInputs}).
     */
    public void runPathing() {
        requireOpen();
        nRunPathing(peer);
    }

    /**
     * Releases the simulator ({@code iplSimulatorRelease}). Calling
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
     * Returns the simulator opaque pointer for child objects of the package.
     *
     * @return an opaque pointer to {@code IPLSimulator}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Checks that the simulator is open.
     *
     * @throws IllegalStateException if the simulator is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("Simulator is closed");
        }
    }

    /**
     * Creates a simulator via {@code iplSimulatorCreate}.
     *
     * @param contextPeer            the context opaque pointer
     * @param flags                  simulation types
     * @param sceneType              scene type ({@code IPLSceneType})
     * @param reflectionType         reflections effect type
     * @param maxNumOcclusionSamples max number of occlusion samples
     * @param maxNumRays             max number of rays
     * @param numDiffuseSamples      number of diffuse samples
     * @param maxDuration            max IR duration, s
     * @param maxOrder               max Ambisonic order
     * @param maxNumSources          max number of reflections sources
     * @param numThreads             number of simulation threads
     * @param rayBatchSize           ray batch size (custom ray tracer)
     * @param numVisSamples          number of pathing visibility samples
     * @param samplingRate           sampling rate, Hz
     * @param frameSize              frame size, samples
     * @return an opaque pointer to the created simulator
     */
    private static native long nCreate(long contextPeer, int flags, int sceneType, int reflectionType,
                                       int maxNumOcclusionSamples, int maxNumRays, int numDiffuseSamples,
                                       float maxDuration, int maxOrder, int maxNumSources, int numThreads,
                                       int rayBatchSize, int numVisSamples, int samplingRate, int frameSize,
                                       long openCLDevicePeer, long radeonRaysDevicePeer, long tanDevicePeer);

    /**
     * Sets the scene via {@code iplSimulatorSetScene}.
     *
     * @param peer      the simulator opaque pointer
     * @param scenePeer the scene opaque pointer
     */
    private static native void nSetScene(long peer, long scenePeer);

    /**
     * Sets shared inputs via {@code iplSimulatorSetSharedInputs}.
     *
     * @param peer                  the simulator opaque pointer
     * @param flags                 simulation types
     * @param listenerX listenerY listenerZ listener position
     * @param aheadX aheadY aheadZ listener "ahead" vector
     * @param upX upY upZ listener "up" vector
     * @param numRays               number of rays
     * @param numBounces            number of bounces
     * @param duration              IR duration, s
     * @param order                 Ambisonic order
     * @param irradianceMinDistance minimum irradiance distance
     */
    private static native void nSetSharedInputs(long peer, int flags,
                                                float listenerX, float listenerY, float listenerZ,
                                                float aheadX, float aheadY, float aheadZ,
                                                float upX, float upY, float upZ,
                                                int numRays, int numBounces, float duration,
                                                int order, float irradianceMinDistance);

    /**
     * Commits the simulator via {@code iplSimulatorCommit}.
     *
     * @param peer the simulator opaque pointer
     */
    private static native void nCommit(long peer);

    /**
     * Runs the direct simulation via {@code iplSimulatorRunDirect}.
     *
     * @param peer the simulator opaque pointer
     */
    private static native void nRunDirect(long peer);

    /**
     * Runs the reflections simulation via {@code iplSimulatorRunReflections}.
     *
     * @param peer the simulator opaque pointer
     */
    private static native void nRunReflections(long peer);

    /**
     * Adds a probe batch via {@code iplSimulatorAddProbeBatch}.
     *
     * @param peer           the simulator opaque pointer
     * @param probeBatchPeer the probe batch opaque pointer
     */
    private static native void nAddProbeBatch(long peer, long probeBatchPeer);

    /**
     * Removes a probe batch via {@code iplSimulatorRemoveProbeBatch}.
     *
     * @param peer           the simulator opaque pointer
     * @param probeBatchPeer the probe batch opaque pointer
     */
    private static native void nRemoveProbeBatch(long peer, long probeBatchPeer);

    /**
     * Runs the pathing simulation via {@code iplSimulatorRunPathing}.
     *
     * @param peer the simulator opaque pointer
     */
    private static native void nRunPathing(long peer);

    /**
     * Releases the simulator via {@code iplSimulatorRelease}.
     *
     * @param peer the simulator opaque pointer
     */
    private static native void nRelease(long peer);
}
