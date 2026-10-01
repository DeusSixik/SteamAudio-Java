package net.sixik.steamaudio.effects;
import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.simulation.Source;

/**
 * Wrapper around {@code IPLDirectEffect} — an effect that renders the
 * results of direct simulation: distance attenuation, air absorption, directivity,
 * occlusion, transmission.
 * <p>
 * The effect parameters are passed as a flattened array of 9 floats —
 * the result of {@link Source#getDirectOutputsInto(float[])}:
 * {@code [distanceAttenuation, airAbs0, airAbs1, airAbs2, directivity,
 * occlusion, trans0, trans1, trans2]}. This format allows the effect to be
 * applied without intermediate objects or allocations.
 * <p>
 * The effect can be applied in-place (input and output are the same buffer).
 */
public final class DirectEffect implements AutoCloseable {

    /** Apply distance attenuation ({@code IPL_DIRECTEFFECTFLAGS_APPLYDISTANCEATTENUATION}). */
    public static final int APPLY_DISTANCE_ATTENUATION = 1;

    /** Apply air absorption ({@code IPL_DIRECTEFFECTFLAGS_APPLYAIRABSORPTION}). */
    public static final int APPLY_AIR_ABSORPTION = 2;

    /** Apply directivity ({@code IPL_DIRECTEFFECTFLAGS_APPLYDIRECTIVITY}). */
    public static final int APPLY_DIRECTIVITY = 4;

    /** Apply occlusion ({@code IPL_DIRECTEFFECTFLAGS_APPLYOCCLUSION}). */
    public static final int APPLY_OCCLUSION = 8;

    /** Apply transmission ({@code IPL_DIRECTEFFECTFLAGS_APPLYTRANSMISSION}). */
    public static final int APPLY_TRANSMISSION = 16;

    /** Frequency-independent transmission ({@code IPL_TRANSMISSIONTYPE_FREQINDEPENDENT}). */
    public static final int TRANSMISSION_FREQ_INDEPENDENT = 0;

    /** Frequency-dependent transmission ({@code IPL_TRANSMISSIONTYPE_FREQDEPENDENT}). */
    public static final int TRANSMISSION_FREQ_DEPENDENT = 1;

    /** Opaque pointer to {@code IPLDirectEffect}; 0 means the effect is closed. */
    private long peer;

    /**
     * Creates a direct effect via {@code iplDirectEffectCreate}.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param numChannels  number of channels of the input and output buffers
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public DirectEffect(Context context, int samplingRate, int frameSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, numChannels);
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
     * Applies the effect to a buffer ({@code iplDirectEffectApply}).
     * <p>
     * Input and output must have the same number of channels (the channel
     * count specified when the effect was created).
     *
     * @param in               input buffer
     * @param out              output buffer (may be the same as the input)
     * @param directOutputs    9 floats from {@link Source#getDirectOutputsInto(float[])}
     * @param effectFlags      combination of {@code APPLY_*} flags
     * @param transmissionType transmission mode: {@link #TRANSMISSION_FREQ_INDEPENDENT}
     *                         or {@link #TRANSMISSION_FREQ_DEPENDENT}
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException if the effect or buffers are closed
     */
    public int apply(AudioBuffer in, AudioBuffer out, float[] directOutputs,
                     int effectFlags, int transmissionType) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("DirectEffect or AudioBuffer is closed");
        }
        if (directOutputs.length < 9) {
            throw new IllegalArgumentException("directOutputs must contain at least 9 elements");
        }
        return nApply(peer, directOutputs, effectFlags, transmissionType,
                in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Resets the internal processing state
     * ({@code iplDirectEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Returns the number of remaining tail samples
     * ({@code iplDirectEffectGetTailSize}).
     *
     * @return number of tail samples
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Retrieves a single frame of tail samples
     * ({@code iplDirectEffectGetTail}).
     *
     * @param out output buffer
     * @return effect state: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         or {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, out.peerForEffect());
    }

    /**
     * Checks that the effect is open.
     *
     * @throws IllegalStateException if the effect is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("DirectEffect is closed");
        }
    }

    /**
     * Releases the effect ({@code iplDirectEffectRelease}). Calling
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
     * Creates the effect via {@code iplDirectEffectCreate}.
     *
     * @param contextPeer  opaque pointer to the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param numChannels  number of input/output channels
     * @return opaque pointer to the created effect
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int numChannels);

    /**
     * Applies the effect via {@code iplDirectEffectApply}.
     *
     * @param effectPeer       opaque pointer to the effect
     * @param directOutputs    flattened parameters of 9 floats
     * @param effectFlags      flags of the effects to apply
     * @param transmissionType transmission mode ({@code IPLTransmissionType})
     * @param inPeer           pointer to the input buffer structure
     * @param outPeer          pointer to the output buffer structure
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, float[] directOutputs, int effectFlags,
                                     int transmissionType, long inPeer, long outPeer);

    /**
     * Resets the effect state via {@code iplDirectEffectReset}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nReset(long effectPeer);

    /**
     * Returns the number of tail samples via {@code iplDirectEffectGetTailSize}.
     *
     * @param effectPeer opaque pointer to the effect
     * @return number of tail samples
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Retrieves a frame of tail samples via {@code iplDirectEffectGetTail}.
     *
     * @param effectPeer opaque pointer to the effect
     * @param outPeer    pointer to the output buffer structure
     * @return effect state ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Releases the effect via {@code iplDirectEffectRelease}.
     *
     * @param effectPeer opaque pointer to the effect
     */
    private static native void nRelease(long effectPeer);
}
