package net.sixik.steamaudio.audio;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.simulation.EnergyField;

/**
 * Wrapper over {@code IPLReconstructor}: converts compact energy fields
 * (as stored in probe batches) back into full impulse responses suitable
 * for convolution.
 */
public final class Reconstructor implements AutoCloseable {

    /** Opaque pointer to {@code IPLReconstructor}; 0 means it is closed. */
    private long peer;

    /**
     * Creates a reconstructor via {@code iplReconstructorCreate}.
     *
     * @param context      Steam Audio context
     * @param maxDuration  largest IR duration that will be reconstructed, seconds
     * @param maxOrder     largest Ambisonics order that will be reconstructed
     * @param samplingRate sampling rate of the reconstructed IRs, Hz
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the context is closed
     */
    public Reconstructor(Context context, float maxDuration, int maxOrder, int samplingRate) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), maxDuration, maxOrder, samplingRate);
    }

    /**
     * Checks whether the reconstructor is open.
     *
     * @return {@code true} if it is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Reconstructs an impulse response from an energy field
     * ({@code iplReconstructorReconstruct} with a single input).
     *
     * @param energyField the energy field to reconstruct from
     * @param duration    IR duration, not more than {@code maxDuration}
     * @param order       Ambisonics order, not more than {@code maxOrder}
     * @param out         the impulse response to write into
     * @throws IllegalStateException if any argument object is closed
     */
    public void reconstruct(EnergyField energyField, float duration, int order, ImpulseResponse out) {
        if (!isOpen() || energyField == null || !energyField.isOpen()
                || out == null || !out.isOpen()) {
            throw new IllegalStateException("Reconstructor, EnergyField or ImpulseResponse is closed");
        }
        nReconstruct(peer, energyField.peerForChildren(), duration, order, out.peerForChildren());
    }

    /**
     * Releases the reconstructor ({@code iplReconstructorRelease}). Safe to
     * call multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, float maxDuration, int maxOrder, int samplingRate);

    private static native void nReconstruct(long peer, long energyFieldPeer, float duration, int order,
                                            long outPeer);

    private static native void nRelease(long peer);
}
