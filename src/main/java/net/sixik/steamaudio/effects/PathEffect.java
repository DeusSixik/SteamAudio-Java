package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.simulation.Source;

/**
 * Wrapper around {@code IPLPathEffect} — an effect that renders the results
 * of the pathing simulation: sound reaching the listener along indirect
 * paths (diffraction through doorways, corridors, etc.).
 * <p>
 * The output is an Ambisonic buffer with {@code (maxOrder + 1)^2} channels; the
 * {@code spatialize = false} version renders unrotated ambisonic audio,
 * which must be rotated to match the listener's orientation before
 * playback (rotation/decoding effects will be added later). The effect
 * cannot be applied in-place.
 */
public final class PathEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLPathEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates a path effect via {@code iplPathEffectCreate} in the mode of
     * rendering ambisonic audio without spatialization
     * ({@code spatialize = false}).
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param maxOrder     maximum Ambisonics order of the output buffer
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public PathEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, maxOrder);
    }

    /**
     * Checks that the effect is open.
     *
     * @return {@code true} if the effect is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Applies the effect to a buffer ({@code iplPathEffectApply}), taking
     * the parameters (EQ coefficients, Ambisonic SH coefficients) directly
     * from the pathing simulation results of the given source.
     *
     * @param in    input mono buffer
     * @param out   output buffer with {@code (maxOrder + 1)^2} channels
     * @param source source with an active pathing simulation
     * @param order Ambisonic order of processing (at most {@code maxOrder};
     *              lower values reduce CPU usage)
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException if the effect, source or buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, Source source, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("PathEffect, Source or AudioBuffer is closed");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must be non-negative");
        }
        return nApply(peer, in.peerForEffect(), out.peerForEffect(), source.peerForChildren(), order);
    }

    /**
     * Returns the number of remaining tail samples
     * ({@code iplPathEffectGetTailSize}).
     *
     * @return number of tail samples
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples
     * ({@code iplPathEffectGetTail}).
     *
     * @param out output buffer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, out.peerForEffect());
    }

    /**
     * Resets the internal processing state
     * ({@code iplPathEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Checks that the effect is open.
     *
     * @throws IllegalStateException if the effect is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("PathEffect is closed");
        }
    }

    /**
     * Releases the effect ({@code iplPathEffectRelease}). Calling it again
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
     * Creates the effect via {@code iplPathEffectCreate}
     * ({@code spatialize = false}).
     *
     * @param contextPeer  opaque pointer to the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param maxOrder     maximum Ambisonics order
     * @return opaque pointer to the created effect
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int maxOrder);

    /**
     * Applies the effect via {@code iplPathEffectApply}: the native side
     * calls {@code iplSourceGetOutputs} for {@code IPL_SIMULATIONFLAGS_PATHING}
     * and passes the {@code shCoeffs}/EQ from the simulation results
     * without copying.
     *
     * @param effectPeer opaque pointer to the effect
     * @param inPeer     pointer to the input buffer structure
     * @param outPeer    pointer to the output buffer structure
     * @param sourcePeer opaque pointer to the source
     * @param order      Ambisonic order of processing
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long inPeer, long outPeer,
                                     long sourcePeer, int order);

    /**
     * Resets the effect state via {@code iplPathEffectReset}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nReset(long effectPeer);

    /**
     * Returns the number of tail samples via {@code iplPathEffectGetTailSize}.
     *
     * @param effectPeer opaque pointer to the effect
     * @return number of tail samples
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Retrieves a frame of tail samples via {@code iplPathEffectGetTail}.
     *
     * @param effectPeer opaque pointer to the effect
     * @param outPeer    pointer to the output buffer structure
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Releases the effect via {@code iplPathEffectRelease}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nRelease(long effectPeer);
}
