package net.sixik.steamaudio.gpu;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLEmbreeDevice}: application-wide state for the Intel
 * Embree ray tracer, required by scenes of type {@code Scene.SCENE_TYPE_EMBREE}.
 * <p>
 * Embree is loaded dynamically by Steam Audio; if {@code embree.dll} is not
 * installed on the system, creation fails with a {@link SteamAudioException}
 * instead of crashing. Ship {@code embree.dll} alongside {@code phonon.dll}
 * to use this backend.
 */
public final class EmbreeDevice implements AutoCloseable {

    /** Opaque pointer to {@code IPLEmbreeDevice}; 0 means the device is closed. */
    private long peer;

    /**
     * Creates an Embree device via {@code iplEmbreeDeviceCreate}.
     *
     * @param context Steam Audio context
     * @throws SteamAudioException   if Steam Audio returns an error (in
     *                               particular, if Embree is not installed)
     * @throws IllegalStateException if the context is closed
     */
    public EmbreeDevice(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
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
     * Returns the opaque pointer for wiring into scene constructors.
     *
     * @return opaque pointer to {@code IPLEmbreeDevice}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the device ({@code iplEmbreeDeviceRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer);

    private static native void nRelease(long peer);
}
