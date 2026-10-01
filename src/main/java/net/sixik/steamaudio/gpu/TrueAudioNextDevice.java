package net.sixik.steamaudio.gpu;

import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLTrueAudioNextDevice}: GPU-accelerated convolution
 * via AMD TrueAudio Next, running on an {@link OpenCLDevice}. Requires TAN
 * support (AMD GPUs); creation fails with a {@link SteamAudioException} on
 * unsupported systems.
 */
public final class TrueAudioNextDevice implements AutoCloseable {

    /** Opaque pointer to {@code IPLTrueAudioNextDevice}; 0 means the device is closed. */
    private long peer;

    /**
     * Creates a TrueAudio Next device via {@code iplTrueAudioNextDeviceCreate}.
     *
     * @param openCLDevice the OpenCL device to run on
     * @param frameSize    number of samples in an audio frame
     * @param irSize       number of samples in the IRs used for convolution
     * @param order        Ambisonics order of the IRs
     * @param maxSources   maximum number of sources using TAN convolution
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the OpenCL device is closed
     */
    public TrueAudioNextDevice(OpenCLDevice openCLDevice, int frameSize, int irSize, int order, int maxSources) {
        if (openCLDevice == null || !openCLDevice.isOpen()) {
            throw new IllegalStateException("OpenCLDevice is closed");
        }
        peer = nCreate(openCLDevice.peerForChildren(), frameSize, irSize, order, maxSources);
        if (peer == 0) {
            throw new SteamAudioException("Failed to create TrueAudio Next device");
        }
    }

    /**
     * Checks whether the device is open.
     *
     * @return {@code true} if the device is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the opaque pointer for wiring into simulators.
     *
     * @return opaque pointer to {@code IPLTrueAudioNextDevice}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the device ({@code iplTrueAudioNextDeviceRelease}). Safe to
     * call multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long openCLDevicePeer, int frameSize, int irSize, int order, int maxSources);

    private static native void nRelease(long peer);
}
