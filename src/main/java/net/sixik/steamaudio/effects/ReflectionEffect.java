package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.simulation.Source;

/**
 * Wrapper around {@code IPLReflectionEffect} — an effect that renders the
 * results of the reflections simulation (reverb).
 * <p>
 * The input is a 1-channel (or 2-channel) audio source; the output is a
 * buffer with a number of channels equal to the effect's {@code numChannels}
 * (for an Ambisonic configuration — {@code (order + 1)<sup>2</sup>}).
 * The effect parameters (IR or RT60) are taken directly from
 * {@link Source#getReflectionsOutputsInto} — the native layer passes the
 * {@code IPLReflectionEffectParams} from the simulation results into apply
 * without copying.
 * <p>
 * For {@code TYPE_CONVOLUTION} and {@code TYPE_TAN}, {@link ReflectionMixer}
 * is recommended (or required for TAN). For {@code TYPE_PARAMETRIC} and
 * {@code TYPE_HYBRID}, the mixer is not used.
 */
public final class ReflectionEffect implements AutoCloseable {

    /** Multichannel convolution reverb ({@code IPL_REFLECTIONEFFECTTYPE_CONVOLUTION}). */
    public static final int TYPE_CONVOLUTION = 0;

    /** Parametric reverb based on feedback delay networks
     * ({@code IPL_REFLECTIONEFFECTTYPE_PARAMETRIC}). Cheaper on CPU,
     * does not render distinct echoes. */
    public static final int TYPE_PARAMETRIC = 1;

    /** Hybrid of convolution + parametric reverb
     * ({@code IPL_REFLECTIONEFFECTTYPE_HYBRID}). */
    public static final int TYPE_HYBRID = 2;

    /** Convolution on GPU via AMD TrueAudio Next
     * ({@code IPL_REFLECTIONEFFECTTYPE_TAN}); requires {@link ReflectionMixer}. */
    public static final int TYPE_TAN = 3;

    /** Opaque pointer to {@code IPLReflectionEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates a reflection effect via {@code iplReflectionEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         reverb algorithm: {@code TYPE_*}
     * @param irSize       number of samples per IR channel (for parametric
     *                     the frame size can be used)
     * @param numChannels  number of channels of the IR/output buffers
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public ReflectionEffect(Context context, int samplingRate, int frameSize,
                            int type, int irSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, irSize, numChannels);
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
     * Applies the effect to a buffer ({@code iplReflectionEffectApply}),
     * taking the parameters (IR/RT60) from the reflections simulation
     * results of the given source.
     *
     * @param in     input buffer (1 or 2 channels)
     * @param out    output buffer (with the effect's {@code numChannels} channels)
     * @param source source with an active reflections simulation
     * @param mixer  optional mixer for {@code TYPE_CONVOLUTION}/
     *               {@code TYPE_TAN}; {@code null} — render directly
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException if the effect, source or buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, Source source, ReflectionMixer mixer) {
        if (!isOpen() || !in.isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("ReflectionEffect, Source or AudioBuffer is closed");
        }
        long mixerPeer = (mixer == null) ? 0 : mixer.peerForChildren();
        return nApply(peer, in.peerForEffect(), out.peerForEffect(), source.peerForChildren(), mixerPeer);
    }

    /**
     * Returns the number of remaining tail samples
     * ({@code iplReflectionEffectGetTailSize}).
     *
     * @return number of tail samples
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples
     * ({@code iplReflectionEffectGetTail}).
     *
     * @param out   output buffer ({@code numChannels} channels)
     * @param mixer optional mixer; {@code null} — no mixer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out, ReflectionMixer mixer) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        long mixerPeer = (mixer == null) ? 0 : mixer.peerForChildren();
        return nGetTail(peer, out.peerForEffect(), mixerPeer);
    }

    /**
     * Resets the internal processing state
     * ({@code iplReflectionEffectReset}).
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
            throw new IllegalStateException("ReflectionEffect is closed");
        }
    }

    /**
     * Releases the effect ({@code iplReflectionEffectRelease}). Calling
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
     * Creates the effect via {@code iplReflectionEffectCreate}.
     *
     * @param contextPeer  opaque pointer to the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         reverb algorithm ({@code IPLReflectionEffectType})
     * @param irSize       number of samples per IR channel
     * @param numChannels  number of IR channels
     * @return opaque pointer to the created effect
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, int irSize, int numChannels);

    /**
     * Applies the effect via {@code iplReflectionEffectApply}: the native
     * side calls {@code iplSourceGetOutputs} for
     * {@code IPL_SIMULATIONFLAGS_REFLECTIONS} and passes the
     * {@code IPLReflectionEffectParams} into apply without copying.
     *
     * @param effectPeer  opaque pointer to the effect
     * @param inPeer      pointer to the input buffer structure
     * @param outPeer     pointer to the output buffer structure
     * @param sourcePeer  opaque pointer to the source
     * @param mixerPeer   opaque pointer to the mixer or 0
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long inPeer, long outPeer,
                                     long sourcePeer, long mixerPeer);

    /**
     * Resets the effect state via {@code iplReflectionEffectReset}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nReset(long effectPeer);

    /**
     * Returns the number of tail samples via {@code iplReflectionEffectGetTailSize}.
     *
     * @param effectPeer opaque pointer to the effect
     * @return number of tail samples
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Retrieves a frame of tail samples via {@code iplReflectionEffectGetTail}.
     *
     * @param effectPeer opaque pointer to the effect
     * @param outPeer    pointer to the output buffer structure
     * @param mixerPeer  opaque pointer to the mixer or 0
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer, long mixerPeer);

    /**
     * Releases the effect via {@code iplReflectionEffectRelease}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nRelease(long effectPeer);
}
