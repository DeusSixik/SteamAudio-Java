package net.sixik.steamaudio.audio;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLImpulseResponse}: an Ambisonic impulse response
 * used for convolution reverb. Data is stored as
 * {@code numChannels * numSamples} floats, channel-major.
 */
public final class ImpulseResponse implements AutoCloseable {

    /** Opaque pointer to {@code IPLImpulseResponse}; 0 means the IR is closed. */
    private long peer;

    /** Number of Ambisonics channels, cached at creation. */
    private final int numChannels;

    /** Number of samples per channel, cached at creation. */
    private final int numSamples;

    /**
     * Creates an impulse response via {@code iplImpulseResponseCreate}.
     *
     * @param context      Steam Audio context
     * @param duration     total duration of the IR, seconds
     * @param order        Ambisonics order ({@code (order + 1)^2} channels)
     * @param samplingRate sampling rate, Hz; together with the duration
     *                     determines the number of samples
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public ImpulseResponse(Context context, float duration, int order, int samplingRate) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), duration, order, samplingRate);
        numChannels = nGetNumChannels(peer);
        numSamples = nGetNumSamples(peer);
    }

    /**
     * Checks whether the IR is open.
     *
     * @return {@code true} if the IR is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the number of Ambisonics channels
     * ({@code iplImpulseResponseGetNumChannels}).
     *
     * @return number of channels
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Returns the number of samples per channel
     * ({@code iplImpulseResponseGetNumSamples}).
     *
     * @return number of samples
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Copies the full IR data into a Java array
     * ({@code iplImpulseResponseGetData}). Layout: channel-major (row-major),
     * {@code numChannels * numSamples} floats.
     *
     * @param out destination array of at least {@code numChannels * numSamples}
     *            floats
     */
    public void getData(float[] out) {
        requireOpen();
        int required = numChannels * numSamples;
        if (out.length < required) {
            throw new IllegalArgumentException("out must contain at least " + required + " elements");
        }
        nGetData(peer, out);
    }

    /**
     * Resets all values to zero ({@code iplImpulseResponseReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Copies data from another IR into this one
     * ({@code iplImpulseResponseCopy}).
     *
     * @param src the source IR
     */
    public void copyFrom(ImpulseResponse src) {
        requireOpen();
        if (src == null || !src.isOpen()) {
            throw new IllegalStateException("Source ImpulseResponse is closed");
        }
        nCopy(src.peerForChildren(), peer);
    }

    /**
     * Swaps the contents of this IR with another ({@code iplImpulseResponseSwap}).
     *
     * @param other the IR to swap with
     */
    public void swap(ImpulseResponse other) {
        requireOpen();
        if (other == null || !other.isOpen()) {
            throw new IllegalStateException("Other ImpulseResponse is closed");
        }
        nSwap(peer, other.peerForChildren());
    }

    /**
     * Adds two IRs and stores the result in this one
     * ({@code iplImpulseResponseAdd}).
     *
     * @param in1 first operand
     * @param in2 second operand
     */
    public void add(ImpulseResponse in1, ImpulseResponse in2) {
        requireOpen();
        if (in1 == null || !in1.isOpen() || in2 == null || !in2.isOpen()) {
            throw new IllegalStateException("Input ImpulseResponse is closed");
        }
        nAdd(in1.peerForChildren(), in2.peerForChildren(), peer);
    }

    /**
     * Scales an IR by a scalar and stores the result in this one
     * ({@code iplImpulseResponseScale}).
     *
     * @param in     the input IR
     * @param scalar the scale factor
     */
    public void scale(ImpulseResponse in, float scalar) {
        requireOpen();
        if (in == null || !in.isOpen()) {
            throw new IllegalStateException("Input ImpulseResponse is closed");
        }
        nScale(in.peerForChildren(), scalar, peer);
    }

    /**
     * Scales an IR by a scalar and accumulates the result into this one
     * ({@code iplImpulseResponseScaleAccum}).
     *
     * @param in     the input IR
     * @param scalar the scale factor
     */
    public void scaleAccum(ImpulseResponse in, float scalar) {
        requireOpen();
        if (in == null || !in.isOpen()) {
            throw new IllegalStateException("Input ImpulseResponse is closed");
        }
        nScaleAccum(in.peerForChildren(), scalar, peer);
    }

    /**
     * Releases the IR ({@code iplImpulseResponseRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Returns the opaque pointer for child objects of the package.
     *
     * @return opaque pointer to {@code IPLImpulseResponse}
     */
    public long peerForChildren() {
        return peer;
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ImpulseResponse is closed");
        }
    }

    private static native long nCreate(long contextPeer, float duration, int order, int samplingRate);

    private static native int nGetNumChannels(long peer);

    private static native int nGetNumSamples(long peer);

    private static native void nGetData(long peer, float[] out);

    private static native void nReset(long peer);

    private static native void nCopy(long srcPeer, long dstPeer);

    private static native void nSwap(long aPeer, long bPeer);

    private static native void nAdd(long in1Peer, long in2Peer, long outPeer);

    private static native void nScale(long inPeer, float scalar, long outPeer);

    private static native void nScaleAccum(long inPeer, float scalar, long outPeer);

    private static native void nRelease(long peer);
}
