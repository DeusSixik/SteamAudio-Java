package net.sixik.steamaudio.gpu;

import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLOpenCLDevice}: application-wide state for OpenCL,
 * encapsulating a {@code cl_context} and up to two command queues. Created
 * from an {@link OpenCLDeviceList}; required by Radeon Rays and TrueAudio
 * Next, and usable for GPU-accelerated reflection baking.
 */
public final class OpenCLDevice implements AutoCloseable {

    /** Opaque pointer to {@code IPLOpenCLDevice}; 0 means the device is closed. */
    private long peer;

    /**
     * Wraps an existing handle (called from
     * {@link OpenCLDeviceList#createDevice}).
     *
     * @param peer opaque pointer to {@code IPLOpenCLDevice}
     * @throws SteamAudioException if the peer is null (creation failed)
     */
    OpenCLDevice(long peer) {
        if (peer == 0) {
            throw new SteamAudioException("Failed to create OpenCL device");
        }
        this.peer = peer;
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
     * Returns the opaque pointer for wiring into simulators and bakers.
     *
     * @return opaque pointer to {@code IPLOpenCLDevice}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the device ({@code iplOpenCLDeviceRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native void nRelease(long peer);
}
