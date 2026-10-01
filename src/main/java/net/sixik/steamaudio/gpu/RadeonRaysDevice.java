package net.sixik.steamaudio.gpu;

import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLRadeonRaysDevice}: GPU ray tracing via Radeon Rays,
 * running on an {@link OpenCLDevice}. Requires Radeon Rays support
 * (typically AMD GPUs via GPUUtilities); creation fails with a
 * {@link SteamAudioException} on unsupported systems.
 */
public final class RadeonRaysDevice implements AutoCloseable {

    /** Opaque pointer to {@code IPLRadeonRaysDevice}; 0 means the device is closed. */
    private long peer;

    /**
     * Creates a Radeon Rays device via {@code iplRadeonRaysDeviceCreate}.
     *
     * @param openCLDevice the OpenCL device to run on
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the OpenCL device is closed
     */
    public RadeonRaysDevice(OpenCLDevice openCLDevice) {
        if (openCLDevice == null || !openCLDevice.isOpen()) {
            throw new IllegalStateException("OpenCLDevice is closed");
        }
        peer = nCreate(openCLDevice.peerForChildren());
        if (peer == 0) {
            throw new SteamAudioException("Failed to create Radeon Rays device");
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
     * Returns the opaque pointer for wiring into scenes and simulators.
     *
     * @return opaque pointer to {@code IPLRadeonRaysDevice}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the device ({@code iplRadeonRaysDeviceRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long openCLDevicePeer);

    private static native void nRelease(long peer);
}
