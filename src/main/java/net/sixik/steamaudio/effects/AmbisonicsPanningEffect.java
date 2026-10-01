package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper around {@code IPLAmbisonicsPanningEffect} — renders ambisonic
 * audio by panning onto a standard speaker layout. Cannot be applied
 * in-place; generates no tail samples.
 */
public final class AmbisonicsPanningEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLAmbisonicsPanningEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates the effect via {@code iplAmbisonicsPanningEffectCreate}.
     *
     * @param context           Steam Audio context
     * @param samplingRate      sampling rate, Hz
     * @param frameSize         frame size in samples
     * @param speakerLayoutType speaker layout type
     *                          ({@code SpeakerLayout.MONO..SURROUND_7_1})
     * @param maxOrder          maximum order of the input buffer
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public AmbisonicsPanningEffect(Context context, int samplingRate, int frameSize,
                                   int speakerLayoutType, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, speakerLayoutType, maxOrder);
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
     * Pans an ambisonic buffer onto speakers
     * ({@code iplAmbisonicsPanningEffectApply}).
     *
     * @param in    input buffer with {@code (order + 1)^2} channels
     * @param out   output buffer with the number of channels of the speaker layout
     * @param order order of the input buffer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsPanningEffect or AudioBuffer is closed");
        }
        return nApply(peer, order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Releases the effect ({@code iplAmbisonicsPanningEffectRelease}). Calling
     * it again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int speakerLayoutType, int maxOrder);

    private static native int nApply(long effectPeer, int order, long inPeer, long outPeer);

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
            throw new IllegalStateException("AmbisonicsPanningEffect is closed");
        }
    }

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);
}
