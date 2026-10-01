package net.sixik.steamaudio.effects;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.core.Vector3;

/**
 * Wrapper over {@code IPLPanningEffect}: pans a single-channel point source
 * to a multi-channel speaker layout based on the 3D position of the source
 * relative to the listener.
 * <p>
 * The input buffer must be mono; the output buffer must have as many
 * channels as the speaker layout configured at creation. This effect cannot
 * be applied in-place and generates no tail samples.
 */
public final class PanningEffect implements AutoCloseable {

    /** Opaque pointer to {@code IPLPanningEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates the effect via {@code iplPanningEffectCreate}.
     *
     * @param context           Steam Audio context
     * @param samplingRate      sampling rate, Hz
     * @param frameSize         frame size, samples
     * @param speakerLayoutType speaker layout for the output buffers
     *                          ({@code SpeakerLayout.MONO..SURROUND_7_1})
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public PanningEffect(Context context, int samplingRate, int frameSize, int speakerLayoutType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, speakerLayoutType);
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
     * Applies the effect to a buffer ({@code iplPanningEffectApply}).
     *
     * @param in        input mono buffer
     * @param out       output buffer with as many channels as the speaker layout
     * @param direction unit vector pointing from the listener towards the source
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING} or
     *         {@link BinauralEffect#STATE_TAIL_COMPLETE} (this effect always
     *         reports no tail)
     * @throws IllegalStateException if the effect or the buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, Vector3 direction) {
        return apply(in, out, direction.x, direction.y, direction.z);
    }

    /**
     * Applies the effect to a buffer — primitive-argument version for
     * allocation-free hot paths.
     *
     * @param in             input mono buffer
     * @param out            output buffer with as many channels as the speaker layout
     * @param dirX dirY dirZ unit vector pointing from the listener towards the source
     * @return effect state
     */
    public int apply(AudioBuffer in, AudioBuffer out, float dirX, float dirY, float dirZ) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("PanningEffect or AudioBuffer is closed");
        }
        return nApply(peer, dirX, dirY, dirZ, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Resets the internal processing state ({@code iplPanningEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Returns the number of tail samples remaining
     * ({@code iplPanningEffectGetTailSize}). This effect generates no tail.
     *
     * @return number of tail samples remaining
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples ({@code iplPanningEffectGetTail}).
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
     * Releases the effect ({@code iplPanningEffectRelease}). Safe to call
     * multiple times.
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
            throw new IllegalStateException("PanningEffect is closed");
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int speakerLayoutType);

    private static native int nApply(long effectPeer, float dirX, float dirY, float dirZ,
                                     long inPeer, long outPeer);

    private static native void nReset(long effectPeer);

    private static native int nGetTailSize(long effectPeer);

    private static native int nGetTail(long effectPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
