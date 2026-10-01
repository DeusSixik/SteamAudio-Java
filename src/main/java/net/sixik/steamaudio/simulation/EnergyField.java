package net.sixik.steamaudio.simulation;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLEnergyField}: a compact representation of reflected
 * sound energy over time, stored per Ambisonics channel and frequency band.
 * Energy fields are much smaller than impulse responses and are what gets
 * stored as baked data in probe batches; use a
 * {@code Reconstructor} (or the effects) to turn them back into audio.
 */
public final class EnergyField implements AutoCloseable {

    /** Opaque pointer to {@code IPLEnergyField}; 0 means the field is closed. */
    private long peer;

    /** Number of Ambisonics channels, cached at creation. */
    private final int numChannels;

    /**
     * Creates an energy field via {@code iplEnergyFieldCreate}.
     *
     * @param context  Steam Audio context
     * @param duration total duration of the field, seconds; determines the
     *                 number of bins
     * @param order    Ambisonics order; determines the number of channels
     *                 ({@code (order + 1)^2})
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public EnergyField(Context context, float duration, int order) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), duration, order);
        numChannels = nGetNumChannels(peer);
    }

    /**
     * Checks whether the field is open.
     *
     * @return {@code true} if the field is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the number of Ambisonics channels
     * ({@code iplEnergyFieldGetNumChannels}).
     *
     * @return number of channels
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Returns the number of histogram bins per channel/band
     * ({@code iplEnergyFieldGetNumBins}).
     *
     * @return number of bins
     */
    public int getNumBins() {
        requireOpen();
        return nGetNumBins(peer);
    }

    /**
     * Copies the full field data into a Java array
     * ({@code iplEnergyFieldGetData}). Layout: channel-major, then band,
     * then bin (row-major), {@code numChannels * IPL_NUM_BANDS * numBins}
     * floats.
     *
     * @param out destination array of at least
     *            {@code numChannels * Material.NUM_BANDS * numBins} floats
     */
    public void getData(float[] out) {
        requireOpen();
        int required = numChannels * 3 * nGetNumBins(peer);
        if (out.length < required) {
            throw new IllegalArgumentException("out must contain at least " + required + " elements");
        }
        nGetData(peer, out);
    }

    /**
     * Resets all values to zero ({@code iplEnergyFieldReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Copies data from another field into this one
     * ({@code iplEnergyFieldCopy}).
     *
     * @param src the source field
     */
    public void copyFrom(EnergyField src) {
        requireOpen();
        if (src == null || !src.isOpen()) {
            throw new IllegalStateException("Source EnergyField is closed");
        }
        nCopy(src.peerForChildren(), peer);
    }

    /**
     * Swaps the contents of this field with another
     * ({@code iplEnergyFieldSwap}).
     *
     * @param other the field to swap with
     */
    public void swap(EnergyField other) {
        requireOpen();
        if (other == null || !other.isOpen()) {
            throw new IllegalStateException("Other EnergyField is closed");
        }
        nSwap(peer, other.peerForChildren());
    }

    /**
     * Adds two fields and stores the result in this one
     * ({@code iplEnergyFieldAdd}).
     *
     * @param in1 first operand
     * @param in2 second operand
     */
    public void add(EnergyField in1, EnergyField in2) {
        requireOpen();
        if (in1 == null || !in1.isOpen() || in2 == null || !in2.isOpen()) {
            throw new IllegalStateException("Input EnergyField is closed");
        }
        nAdd(in1.peerForChildren(), in2.peerForChildren(), peer);
    }

    /**
     * Scales a field by a scalar and stores the result in this one
     * ({@code iplEnergyFieldScale}).
     *
     * @param in     the input field
     * @param scalar the scale factor
     */
    public void scale(EnergyField in, float scalar) {
        requireOpen();
        if (in == null || !in.isOpen()) {
            throw new IllegalStateException("Input EnergyField is closed");
        }
        nScale(in.peerForChildren(), scalar, peer);
    }

    /**
     * Scales a field by a scalar and accumulates the result into this one
     * ({@code iplEnergyFieldScaleAccum}).
     *
     * @param in     the input field
     * @param scalar the scale factor
     */
    public void scaleAccum(EnergyField in, float scalar) {
        requireOpen();
        if (in == null || !in.isOpen()) {
            throw new IllegalStateException("Input EnergyField is closed");
        }
        nScaleAccum(in.peerForChildren(), scalar, peer);
    }

    /**
     * Releases the field ({@code iplEnergyFieldRelease}). Safe to call
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
     * @return opaque pointer to {@code IPLEnergyField}
     */
    public long peerForChildren() {
        return peer;
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("EnergyField is closed");
        }
    }

    private static native long nCreate(long contextPeer, float duration, int order);

    private static native int nGetNumChannels(long peer);

    private static native int nGetNumBins(long peer);

    private static native void nGetData(long peer, float[] out);

    private static native void nReset(long peer);

    private static native void nCopy(long srcPeer, long dstPeer);

    private static native void nSwap(long aPeer, long bPeer);

    private static native void nAdd(long in1Peer, long in2Peer, long outPeer);

    private static native void nScale(long inPeer, float scalar, long outPeer);

    private static native void nScaleAccum(long inPeer, float scalar, long outPeer);

    private static native void nRelease(long peer);
}
