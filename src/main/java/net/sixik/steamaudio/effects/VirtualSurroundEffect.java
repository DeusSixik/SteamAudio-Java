package net.sixik.steamaudio.effects;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLVirtualSurroundEffect}: spatializes multi-channel
 * speaker-based audio (stereo, quadraphonic, 5.1, 7.1) using HRTF-based
 * binaural rendering. The signal for each speaker is spatialized from a
 * point corresponding to the speaker's position.
 * <p>
 * The input buffer must have as many channels as the speaker layout
 * configured at creation; the output is stereo. This effect cannot be
 * applied in-place.
 */
public final class VirtualSurroundEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLVirtualSurroundEffect}; 0 means the effect is closed. */
    private long peer;

    /** Opaque pointer to the parent context. */
    private final long contextPeer;

    /** HRTF used by this effect; keeps it from being garbage collected. */
    private final HRTF hrtf;

    /**
     * Creates the effect via {@code iplVirtualSurroundEffectCreate}.
     *
     * @param context           Steam Audio context
     * @param samplingRate      sampling rate, Hz
     * @param frameSize         frame size, samples
     * @param speakerLayoutType speaker layout of the input buffers
     *                          ({@code SpeakerLayout.MONO..SURROUND_7_1})
     * @param hrtf              HRTF used for binaural rendering; must stay
     *                          open for the lifetime of the effect
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public VirtualSurroundEffect(Context context, int samplingRate, int frameSize,
                                 int speakerLayoutType, HRTF hrtf) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.hrtf = hrtf;
        peer = nCreate(contextPeer, samplingRate, frameSize, speakerLayoutType, hrtf.peerForChildren());
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
     * Applies the effect to a buffer ({@code iplVirtualSurroundEffectApply}).
     *
     * @param in  input buffer with as many channels as the speaker layout
     * @param out output stereo buffer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING} or
     *         {@link BinauralEffect#STATE_TAIL_COMPLETE} (this effect generates
     *         no tail)
     * @throws IllegalStateException if the effect, the HRTF or the buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("VirtualSurroundEffect or AudioBuffer is closed");
        }
        if (!hrtf.isOpen()) {
            throw new IllegalStateException("HRTF is closed");
        }
        return nApply(peer, hrtf.peerForChildren(), in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Resets the internal processing state ({@code iplVirtualSurroundEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Returns the number of tail samples remaining
     * ({@code iplVirtualSurroundEffectGetTailSize}).
     *
     * @return number of tail samples remaining
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples
     * ({@code iplVirtualSurroundEffectGetTail}).
     *
     * @param out output stereo buffer
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
     * Releases the effect ({@code iplVirtualSurroundEffectRelease}). Safe to
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
            throw new IllegalStateException("VirtualSurroundEffect is closed");
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int speakerLayoutType, long hrtfPeer);

    private static native int nApply(long effectPeer, long hrtfPeer, long inPeer, long outPeer);

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
