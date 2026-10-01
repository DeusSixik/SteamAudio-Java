package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.simulation.Source;

/**
 * Wrapper around {@code IPLReflectionMixer} — a mixer for the outputs of
 * multiple reflection effects, producing a single reflections sound field.
 * <p>
 * Using it is optional; for {@code TYPE_CONVOLUTION} the mixer reduces CPU
 * load, for {@code TYPE_TAN} it is required, and for
 * {@code TYPE_PARAMETRIC} and {@code TYPE_HYBRID} it is not used.
 */
public final class ReflectionMixer implements AutoCloseable {

    /** Opaque pointer to {@code IPLReflectionMixer}; 0 means the mixer is closed. */
    private long peer;

    /**
     * Creates a reflection mixer via {@code iplReflectionMixerCreate}.
     * The settings must match the settings of the {@link ReflectionEffect}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         reverb algorithm
     *                     ({@code ReflectionEffect.TYPE_*})
     * @param irSize       number of samples per IR channel
     * @param numChannels  number of channels of the IR/output buffers
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public ReflectionMixer(Context context, int samplingRate, int frameSize,
                           int type, int irSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, irSize, numChannels);
    }

    /**
     * Checks that the mixer is open.
     *
     * @return {@code true} if the mixer is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Retrieves the mixer contents into a buffer
     * ({@code iplReflectionMixerApply}), taking the parameters from the
     * reflections simulation results of the given source.
     *
     * @param out    output buffer ({@code numChannels} channels)
     * @param source source with an active reflections simulation
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException if the mixer, source or buffer is closed
     */
    public int apply(AudioBuffer out, Source source) {
        if (!isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("ReflectionMixer, Source or AudioBuffer is closed");
        }
        return nApply(peer, out.peerForEffect(), source.peerForChildren());
    }

    /**
     * Resets the internal processing state
     * ({@code iplReflectionMixerReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Checks that the mixer is open.
     *
     * @throws IllegalStateException if the mixer is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ReflectionMixer is closed");
        }
    }

    /**
     * Releases the mixer ({@code iplReflectionMixerRelease}). Calling
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
     * Returns the opaque pointer of the mixer for the package's child objects.
     *
     * @return opaque pointer to {@code IPLReflectionMixer}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Creates the mixer via {@code iplReflectionMixerCreate}.
     *
     * @param contextPeer  opaque pointer to the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         reverb algorithm ({@code IPLReflectionEffectType})
     * @param irSize       number of samples per IR channel
     * @param numChannels  number of IR channels
     * @return opaque pointer to the created mixer
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, int irSize, int numChannels);

    /**
     * Retrieves the mixer contents via {@code iplReflectionMixerApply}:
     * the native side calls {@code iplSourceGetOutputs} for
     * {@code IPL_SIMULATIONFLAGS_REFLECTIONS} and passes the
     * {@code IPLReflectionEffectParams} into apply without copying.
     *
     * @param peer        opaque pointer to the mixer
     * @param outPeer     pointer to the output buffer structure
     * @param sourcePeer  opaque pointer to the source
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nApply(long peer, long outPeer, long sourcePeer);

    /**
     * Resets the mixer state via {@code iplReflectionMixerReset}.
     *
     * @param peer opaque pointer to the mixer
     */
    private static native void nReset(long peer);

    /**
     * Releases the mixer via {@code iplReflectionMixerRelease}.
     *
     * @param peer opaque pointer to the mixer
     */
    private static native void nRelease(long peer);
}
