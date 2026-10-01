package net.sixik.steamaudio.audio;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.effects.BinauralEffect;

/**
 * Wrapper around {@code IPLAudioBuffer} — the Steam Audio audio buffer.
 * <p>
 * All Steam Audio buffers are uncompressed PCM with 32-bit float samples,
 * stored deinterleaved (SoA: a separate array per channel). The buffer
 * is allocated via {@code iplAudioBufferAllocate}; the memory belongs to
 * Steam Audio and is freed via {@code iplAudioBufferFree} in
 * {@link #close()}.
 * <p>
 * The buffer must be closed <b>before</b> the {@link Context} that created
 * it is closed, otherwise freeing the memory would access an already
 * destroyed context.
 */
public final class AudioBuffer implements AutoCloseable {

    /** Peer (opaque pointer) of the parent Steam Audio context. */
    private final long contextPeer;

    /** Number of channels. */
    private final int numChannels;

    /** Number of samples per channel. */
    private final int numSamples;

    /**
     * Pointer to the native {@code IPLAudioBuffer} structure; 0 means
     * the buffer is closed.
     */
    private long peer;

    /**
     * Creates and allocates an audio buffer via {@code iplAudioBufferAllocate}.
     *
     * @param context     context through which the buffer memory is allocated
     * @param numChannels number of channels
     * @param numSamples  number of samples per channel
     * @throws SteamAudioException    if Steam Audio returned an error
     * @throws IllegalStateException  if the context is closed
     */
    public AudioBuffer(Context context, int numChannels, int numSamples) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = contextPeerOf(context);
        this.numChannels = numChannels;
        this.numSamples = numSamples;
        this.peer = nAllocate(contextPeer, numChannels, numSamples);
    }

    /**
     * Returns the number of channels.
     *
     * @return number of channels
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Returns the number of samples per channel.
     *
     * @return number of samples per channel
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Checks whether the buffer is open.
     *
     * @return {@code true} if the buffer has been created and is not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Reads samples from the buffer and writes them into an interleaved array
     * ({@code iplAudioBufferInterleave}).
     *
     * @param dst array of length at least {@code numChannels * numSamples};
     *            filled in the LRLRLR... (interleaved) format
     */
    public void interleaveTo(float[] dst) {
        requireOpen();
        if (dst.length < numChannels * numSamples) {
            throw new IllegalArgumentException(
                    "dst too small: expected >= " + (numChannels * numSamples) + " samples, got " + dst.length);
        }
        nInterleave(contextPeer, peer, dst);
    }

    /**
     * Writes interleaved samples from the array into the buffer
     * ({@code iplAudioBufferDeinterleave}).
     *
     * @param src interleaved array of length at least
     *            {@code numChannels * numSamples}
     */
    public void deinterleaveFrom(float[] src) {
        requireOpen();
        if (src.length < numChannels * numSamples) {
            throw new IllegalArgumentException(
                    "src too small: expected >= " + (numChannels * numSamples) + " samples, got " + src.length);
        }
        nDeinterleave(contextPeer, peer, src);
    }

    /**
     * Frees the buffer ({@code iplAudioBufferFree}). Calling it again
     * is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nFree(contextPeer, peer);
            peer = 0;
        }
    }

    /**
     * Mixes another buffer into this one ({@code iplAudioBufferMix}).
     * Both buffers must have the same number of channels and samples.
     *
     * @param in the source buffer whose content is mixed into this buffer
     * @throws IllegalStateException    if either buffer is closed
     * @throws IllegalArgumentException if the channel/sample counts differ
     */
    public void mix(AudioBuffer in) {
        requireOpen();
        if (!in.isOpen()) {
            throw new IllegalStateException("Input AudioBuffer is closed");
        }
        if (in.numChannels != numChannels || in.numSamples != numSamples) {
            throw new IllegalArgumentException(
                    "mix requires matching buffers: " + in.numChannels + "ch/" + in.numSamples
                            + " samples vs " + numChannels + "ch/" + numSamples + " samples");
        }
        nMix(contextPeer, in.peerForEffect(), peer);
    }

    /**
     * Downmixes a multi-channel buffer into a mono buffer
     * ({@code iplAudioBufferDownmix}). The channels are summed and divided
     * by the number of source channels. This buffer must be mono; the source
     * must have the same number of samples.
     *
     * @param in the multi-channel source buffer
     * @throws IllegalStateException    if either buffer is closed
     * @throws IllegalArgumentException if this buffer is not mono or the
     *                                  sample counts differ
     */
    public void downmixFrom(AudioBuffer in) {
        requireOpen();
        if (!in.isOpen()) {
            throw new IllegalStateException("Input AudioBuffer is closed");
        }
        if (numChannels != 1) {
            throw new IllegalArgumentException("downmix target must be mono");
        }
        if (in.numSamples != numSamples) {
            throw new IllegalArgumentException(
                    "downmix requires matching sample counts: " + in.numSamples + " vs " + numSamples);
        }
        nDownmix(contextPeer, in.peerForEffect(), peer);
    }

    /**
     * Converts this buffer from one Ambisonics format to another
     * ({@code iplAudioBufferConvertAmbisonics}). Both buffers must have the
     * same number of samples; this buffer receives the result. Conversion
     * may be applied to the same buffer on the native side, but here the
     * source and destination are distinct Java objects.
     *
     * @param inType Ambisonics format of the source
     * @param outType Ambisonics format for this buffer
     * @param in     the source buffer
     * @throws IllegalStateException    if either buffer is closed
     * @throws IllegalArgumentException if the sample counts differ
     */
    public void convertAmbisonicsFrom(int inType, int outType, AudioBuffer in) {
        requireOpen();
        if (!in.isOpen()) {
            throw new IllegalStateException("Input AudioBuffer is closed");
        }
        if (in.numSamples != numSamples) {
            throw new IllegalArgumentException(
                    "ambisonics conversion requires matching sample counts: "
                            + in.numSamples + " vs " + numSamples);
        }
        nConvertAmbisonics(contextPeer, inType, outType, in.peerForEffect(), peer);
    }

    /**
     * Returns the peer pointer of the context without requiring public
     * access to the {@link Context} field.
     *
     * @param context open context
     * @return opaque pointer of the context
     */
    private static long contextPeerOf(Context context) {
        return context.peerForChildren();
    }

    /**
     * Returns the peer pointer of the native buffer structure for child
     * objects of the package (e.g. {@code BinauralEffect}).
     *
     * @return pointer to the native {@code IPLAudioBuffer} structure
     */
    public long peerForEffect() {
        return peer;
    }

    /**
     * Checks that the buffer is open.
     *
     * @throws IllegalStateException if the buffer is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("AudioBuffer is closed");
        }
    }

    /**
     * Allocates the buffer via {@code iplAudioBufferAllocate}.
     *
     * @param contextPeer opaque pointer of the context
     * @param numChannels number of channels
     * @param numSamples  number of samples per channel
     * @return pointer to the native {@code IPLAudioBuffer} structure
     */
    private static native long nAllocate(long contextPeer, int numChannels, int numSamples);

    /**
     * Frees the buffer via {@code iplAudioBufferFree} and destroys
     * the native structure.
     *
     * @param contextPeer opaque pointer of the context
     * @param peer        pointer to the native buffer structure
     */
    private static native void nFree(long contextPeer, long peer);

    /**
     * Interleaves the buffer samples into an array ({@code iplAudioBufferInterleave}).
     *
     * @param contextPeer opaque pointer of the context
     * @param peer        pointer to the native buffer structure
     * @param dst         destination interleaved array
     */
    private static native void nInterleave(long contextPeer, long peer, float[] dst);

    /**
     * Deinterleaves the array samples into the buffer
     * ({@code iplAudioBufferDeinterleave}).
     *
     * @param contextPeer opaque pointer of the context
     * @param peer        pointer to the native buffer structure
     * @param src         source interleaved array
     */
    private static native void nDeinterleave(long contextPeer, long peer, float[] src);

    /**
     * Mixes the source buffer into this one via {@code iplAudioBufferMix}.
     *
     * @param contextPeer opaque pointer of the context
     * @param inPeer      pointer to the source native buffer structure
     * @param peer        pointer to the destination native buffer structure
     */
    private static native void nMix(long contextPeer, long inPeer, long peer);

    /**
     * Downmixes the multi-channel source into this mono buffer via
     * {@code iplAudioBufferDownmix}.
     *
     * @param contextPeer opaque pointer of the context
     * @param inPeer      pointer to the source native buffer structure
     * @param peer        pointer to the destination native buffer structure
     */
    private static native void nDownmix(long contextPeer, long inPeer, long peer);

    /**
     * Converts Ambisonics formats via {@code iplAudioBufferConvertAmbisonics}.
     *
     * @param contextPeer opaque pointer of the context
     * @param inType      source Ambisonics format ({@code IPLAmbisonicsType})
     * @param outType     destination Ambisonics format ({@code IPLAmbisonicsType})
     * @param inPeer      pointer to the source native buffer structure
     * @param peer        pointer to the destination native buffer structure
     */
    private static native void nConvertAmbisonics(long contextPeer, int inType, int outType,
                                                  long inPeer, long peer);
}
