package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;

/**
 * Wrapper around {@code IPLAmbisonicsRotationEffect} — rotates an
 * ambisonic buffer from "world coordinates" into the listener's
 * coordinate system. Can be applied in-place.
 */
public final class AmbisonicsRotationEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLAmbisonicsRotationEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates the effect via {@code iplAmbisonicsRotationEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param maxOrder     maximum processing order
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public AmbisonicsRotationEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
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
     * Rotates a buffer to match the listener's orientation
     * ({@code iplAmbisonicsRotationEffectApply}).
     *
     * @param in            input buffer with {@code (order + 1)^2} channels
     * @param out           output buffer (may be the same as the input)
     * @param listenerPosition      world coordinates of the listener
     * @param listenerAhead         unit "ahead" vector of the listener
     * @param listenerUp            unit "up" vector of the listener
     * @param order         processing order (at most {@code maxOrder})
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out,
                     Vector3 listenerPosition, Vector3 listenerAhead, Vector3 listenerUp, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsRotationEffect or AudioBuffer is closed");
        }
        return nApply(peer, listenerPosition.x, listenerPosition.y, listenerPosition.z,
                listenerAhead.x, listenerAhead.y, listenerAhead.z,
                listenerUp.x, listenerUp.y, listenerUp.z,
                order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Releases the effect ({@code iplAmbisonicsRotationEffectRelease}). Calling
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

    private static native int nApply(long effectPeer,
                                     float listenerX, float listenerY, float listenerZ,
                                     float aheadX, float aheadY, float aheadZ,
                                     float upX, float upY, float upZ,
                                     int order, long inPeer, long outPeer);

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
            throw new IllegalStateException("AmbisonicsRotationEffect is closed");
        }
    }

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);
}
