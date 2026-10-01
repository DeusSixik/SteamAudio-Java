package net.sixik.steamaudio.effects;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLAmbisonicsDecodeEffect}: renders Ambisonic audio by
 * panning it to a standard speaker layout, calculating the signals to emit
 * from each speaker so as to approximate the Ambisonic sound field.
 * <p>
 * The input buffer must have {@code (order + 1)^2} channels; the output must
 * have as many channels as the speaker layout configured at creation. This
 * effect cannot be applied in-place and generates no tail samples.
 */
public final class AmbisonicsDecodeEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLAmbisonicsDecodeEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates the effect via {@code iplAmbisonicsDecodeEffectCreate}.
     *
     * @param context           Steam Audio context
     * @param samplingRate      sampling rate, Hz
     * @param frameSize         frame size, samples
     * @param speakerLayoutType speaker layout for the output buffers
     *                          ({@code SpeakerLayout.MONO..SURROUND_7_1})
     * @param maxOrder          maximum Ambisonics order of the input buffers
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public AmbisonicsDecodeEffect(Context context, int samplingRate, int frameSize,
                                  int speakerLayoutType, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, speakerLayoutType, maxOrder);
    }

    /**
     * Checks whether the effect is open.
     *
     * @return {@code true} if the effect is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Decodes an Ambisonic buffer to speakers ({@code iplAmbisonicsDecodeEffectApply}).
     *
     * @param in    input buffer with {@code (order + 1)^2} channels
     * @param out   output buffer with as many channels as the speaker layout
     * @param order Ambisonic order of the input buffer (not more than
     *              {@code maxOrder}; lower values reduce CPU usage)
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING} or
     *         {@link BinauralEffect#STATE_TAIL_COMPLETE} (this effect always
     *         reports no tail)
     * @throws IllegalStateException if the effect or the buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsDecodeEffect or AudioBuffer is closed");
        }
        return nApply(peer, order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Resets the internal processing state ({@code iplAmbisonicsDecodeEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Returns the number of tail samples remaining
     * ({@code iplAmbisonicsDecodeEffectGetTailSize}).
     *
     * @return number of tail samples remaining
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples
     * ({@code iplAmbisonicsDecodeEffectGetTail}).
     *
     * @param out output buffer with as many channels as the speaker layout
     * @return effect state
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, out.peerForEffect());
    }

    /**
     * Releases the effect ({@code iplAmbisonicsDecodeEffectRelease}). Safe to
     * call multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("AmbisonicsDecodeEffect is closed");
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int speakerLayoutType, int maxOrder);

    private static native int nApply(long effectPeer, int order, long inPeer, long outPeer);

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
