package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper around {@code IPLAmbisonicsBinauralEffect} — renders ambisonic
 * audio binaurally via HRTF (more immersive than panning, slightly more
 * CPU-expensive). The input is an ambisonic buffer, the output is stereo.
 */
public final class AmbisonicsBinauralEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLAmbisonicsBinauralEffect}; 0 means the effect is closed. */
    private long peer;

    /** Opaque pointer of the parent context. */
    private final long contextPeer;

    /** HRTF used at creation; prevents its GC. */
    private final HRTF hrtf;

    /**
     * Creates the effect via {@code iplAmbisonicsBinauralEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param hrtf         HRTF used by the effect; must remain open
     *                     for the entire lifetime of the effect
     * @param maxOrder     maximum order of the input buffer
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public AmbisonicsBinauralEffect(Context context, int samplingRate, int frameSize, HRTF hrtf, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.hrtf = hrtf;
        peer = nCreate(contextPeer, samplingRate, frameSize, hrtf.peerForChildren(), maxOrder);
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
     * Renders an ambisonic buffer binaurally
     * ({@code iplAmbisonicsBinauralEffectApply}).
     *
     * @param in    input ambisonic buffer with {@code (order + 1)^2} channels
     * @param out   output stereo buffer
     * @param order order of the input buffer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsBinauralEffect or AudioBuffer is closed");
        }
        if (!hrtf.isOpen()) {
            throw new IllegalStateException("HRTF is closed");
        }
        return nApply(peer, contextPeer, hrtf.peerForChildren(), order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Releases the effect ({@code iplAmbisonicsBinauralEffectRelease}). Calling
     * it again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, long hrtfPeer, int maxOrder);

    private static native int nApply(long effectPeer, long contextPeer, long hrtfPeer, int order,
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
            throw new IllegalStateException("AmbisonicsBinauralEffect is closed");
        }
    }

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);
}
