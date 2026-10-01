package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;

/**
 * Wrapper around {@code IPLAmbisonicsEncodeEffect} — encodes a mono (or
 * stereo) point source into an ambisonic buffer of a given order.
 * Steam Audio encodes in N3D (ACN, orthonormal harmonics).
 */
public final class AmbisonicsEncodeEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLAmbisonicsEncodeEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates the effect via {@code iplAmbisonicsEncodeEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param maxOrder     maximum encoding order
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public AmbisonicsEncodeEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
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
     * Encodes a buffer into ambisonics ({@code iplAmbisonicsEncodeEffectApply}).
     *
     * @param in            input buffer (1 or 2 channels)
     * @param out           output buffer with {@code (order + 1)^2} channels
     * @param direction     vector from the listener to the source (not
     *                      necessarily unit-length; a zero vector = omni)
     * @param order         encoding order (at most {@code maxOrder})
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, Vector3 direction, int order) {
        return apply(in, out, direction.x, direction.y, direction.z, order);
    }

    /**
     * Encodes a buffer into ambisonics — the primitive-based version for the hot path.
     *
     * @param in            input buffer (1 or 2 channels)
     * @param out           output buffer with {@code (order + 1)^2} channels
     * @param dirX dirY dirZ vector from the listener to the source
     * @param order         encoding order (at most {@code maxOrder})
     * @return effect state
     */
    public int apply(AudioBuffer in, AudioBuffer out, float dirX, float dirY, float dirZ, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsEncodeEffect or AudioBuffer is closed");
        }
        return nApply(peer, dirX, dirY, dirZ, order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Releases the effect ({@code iplAmbisonicsEncodeEffectRelease}). Calling
     * it again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int maxOrder);

    private static native int nApply(long effectPeer, float dirX, float dirY, float dirZ, int order,
                                     long inPeer, long outPeer);

    private static native void nRelease(long effectPeer);
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, out.peerForEffect());
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("AmbisonicsEncodeEffect is closed");
        }
    }

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);
}
