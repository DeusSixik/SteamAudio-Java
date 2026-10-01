package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;

/**
 * Wrapper around {@code IPLBinauralEffect} — an effect for HRTF-based
 * binaural spatialization of a point source.
 * <p>
 * The input buffer must be 1- or 2-channel; the output must be 2-channel
 * (stereo). The effect cannot be applied in-place: input and output are
 * distinct buffers. The direction must be a unit vector in the listener's
 * coordinate system (see {@link Context#calculateRelativeDirection}).
 */
public final class BinauralEffect implements AutoCloseable {

    /** HRTF interpolation: nearest neighbor ({@code IPL_HRTFINTERPOLATION_NEAREST}).
     * The cheapest option; use as the default. */
    public static final int INTERPOLATION_NEAREST = 0;

    /** HRTF interpolation: bilinear ({@code IPL_HRTFINTERPOLATION_BILINEAR}).
     * More CPU-expensive; useful for broadband noise-like sounds. */
    public static final int INTERPOLATION_BILINEAR = 1;

    /** Tail samples remain in the effect's internal buffers
     * ({@code IPL_AUDIOEFFECTSTATE_TAILREMAINING}). */
    public static final int STATE_TAIL_REMAINING = 0;

    /** No tail samples remain ({@code IPL_AUDIOEFFECTSTATE_TAILCOMPLETE}). */
    public static final int STATE_TAIL_COMPLETE = 1;

    /** Opaque pointer to {@code IPLBinauralEffect}; 0 means the effect is closed. */
    private long peer;

    /** Peer pointer of the context used when creating the effect. */
    private final long contextPeer;

    /** HRTF used when creating the effect; prevents its GC. */
    private final HRTF hrtf;

    /**
     * Creates a binaural effect via {@code iplBinauralEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param hrtf         HRTF used by the effect; must remain open
     *                     for the entire lifetime of the effect
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public BinauralEffect(Context context, int samplingRate, int frameSize, HRTF hrtf) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.hrtf = hrtf;
        this.peer = nCreate(contextPeer, samplingRate, frameSize, hrtf.peerForChildren());
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
     * Applies the effect to a buffer ({@code iplBinauralEffectApply}).
     * <p>
     * Input and output are distinct buffers (not in-place). The input has
     * 1 or 2 channels, the output has 2 channels, with the same number of samples.
     *
     * @param in            input buffer
     * @param out           output stereo buffer
     * @param direction     unit vector from the listener to the source
     * @param interpolation HRTF interpolation: {@link #INTERPOLATION_NEAREST}
     *                      or {@link #INTERPOLATION_BILINEAR}
     * @param spatialBlend  0.0 — no spatialization, 1.0 — full
     * @return effect state: {@link #STATE_TAIL_REMAINING} or
     *         {@link #STATE_TAIL_COMPLETE}
     * @throws IllegalStateException if the effect, context, input or
     *                              output buffer is closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, Vector3 direction,
                     int interpolation, float spatialBlend) {
        return apply(in, out, direction.x, direction.y, direction.z, interpolation, spatialBlend);
    }

    /**
     * Applies the effect to a buffer without objects or allocations —
     * the primitive-based version for the hot path. See the full
     * documentation in the Vector3 overload.
     *
     * @param in            input buffer
     * @param out           output stereo buffer
     * @param dirX dirY dirZ unit vector from the listener to the source
     * @param interpolation HRTF interpolation: {@link #INTERPOLATION_NEAREST}
     *                      or {@link #INTERPOLATION_BILINEAR}
     * @param spatialBlend  0.0 — no spatialization, 1.0 — full
     * @return effect state: {@link #STATE_TAIL_REMAINING} or
     *         {@link #STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out,
                     float dirX, float dirY, float dirZ,
                     int interpolation, float spatialBlend) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("BinauralEffect, Context, input or output AudioBuffer is closed");
        }
        if (!hrtf.isOpen()) {
            throw new IllegalStateException("HRTF is closed");
        }
        return nApply(peer, contextPeer, hrtf.peerForChildren(),
                dirX, dirY, dirZ, interpolation, spatialBlend,
                inPeerOf(in), outPeerOf(out));
    }

    /**
     * Returns the number of remaining tail samples
     * ({@code iplBinauralEffectGetTailSize}).
     *
     * @return number of tail samples
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples into an external buffer
     * ({@code iplBinauralEffectGetTail}); call this instead of
     * {@link #apply} after the input has stopped, while
     * {@link #STATE_TAIL_REMAINING} is returned.
     *
     * @param out output stereo buffer
     * @return effect state: {@link #STATE_TAIL_REMAINING} or
     *         {@link #STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, outPeerOf(out));
    }

    /**
     * Resets the internal processing state
     * ({@code iplBinauralEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Releases the effect ({@code iplBinauralEffectRelease}). Calling
     * it again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Returns the peer pointer of a buffer without requiring public access
     * to the {@link AudioBuffer} field.
     *
     * @param buffer open buffer
     * @return pointer to the buffer's native structure
     */
    private static long inPeerOf(AudioBuffer buffer) {
        return buffer.peerForEffect();
    }

    /**
     * Returns the peer pointer of a buffer without requiring public access
     * to the {@link AudioBuffer} field.
     *
     * @param buffer open buffer
     * @return pointer to the buffer's native structure
     */
    private static long outPeerOf(AudioBuffer buffer) {
        return buffer.peerForEffect();
    }

    /**
     * Checks that the effect is open.
     *
     * @throws IllegalStateException if the effect is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("BinauralEffect is closed");
        }
    }

    /**
     * Creates the effect via {@code iplBinauralEffectCreate}.
     *
     * @param contextPeer  opaque pointer to the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param hrtfPeer     opaque pointer to the HRTF
     * @return opaque pointer to the created effect
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, long hrtfPeer);

    /**
     * Applies the effect via {@code iplBinauralEffectApply}.
     *
     * @param effectPeer    opaque pointer to the effect
     * @param contextPeer   opaque pointer to the context
     * @param hrtfPeer      opaque pointer to the HRTF
     * @param dirX dirY dirZ unit vector from the listener to the source
     * @param interpolation HRTF interpolation ({@code IPLHRTFInterpolation})
     * @param spatialBlend  spatialization degree, 0.0..1.0
     * @param inPeer        pointer to the input native buffer structure
     * @param outPeer       pointer to the output native buffer structure
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long contextPeer, long hrtfPeer,
                                     float dirX, float dirY, float dirZ,
                                     int interpolation, float spatialBlend,
                                     long inPeer, long outPeer);

    /**
     * Resets the effect state via {@code iplBinauralEffectReset}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nReset(long effectPeer);

    /**
     * Returns the number of tail samples via {@code iplBinauralEffectGetTailSize}.
     *
     * @param effectPeer opaque pointer to the effect
     * @return number of tail samples
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Retrieves a frame of tail samples via {@code iplBinauralEffectGetTail}.
     *
     * @param effectPeer opaque pointer to the effect
     * @param outPeer    pointer to the output native buffer structure
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Releases the effect via {@code iplBinauralEffectRelease}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nRelease(long effectPeer);
}
