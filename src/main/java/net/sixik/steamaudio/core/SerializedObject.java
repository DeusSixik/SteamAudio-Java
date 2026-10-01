package net.sixik.steamaudio.core;

/**
 * Wrapper around {@code IPLSerializedObject} — a serialized representation
 * of a Steam Audio API object (for example, a scene).
 * <p>
 * An empty object is created for serializing an existing object,
 * or the object wraps an existing byte array for deserialization.
 */
public final class SerializedObject implements AutoCloseable {

    /** Opaque pointer to {@code IPLSerializedObject}; 0 means the object is closed. */
    private long peer;

    /**
     * Creates an empty serialized object via
     * {@code iplSerializedObjectCreate}.
     *
     * @param context Steam Audio context
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public SerializedObject(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Checks that the object is open.
     *
     * @return {@code true} if the object is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the size of the serialized data in bytes
     * ({@code iplSerializedObjectGetSize}).
     *
     * @return data size in bytes
     */
    public long getSize() {
        requireOpen();
        return nGetSize(peer);
    }

    /**
     * Returns a copy of the serialized data
     * ({@code iplSerializedObjectGetData}).
     *
     * @return byte array of length {@link #getSize()}
     */
    public byte[] getData() {
        requireOpen();
        long size = getSize();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Serialized data too large: " + size + " bytes");
        }
        byte[] data = new byte[(int) size];
        nGetData(peer, data);
        return data;
    }

    /**
     * Checks that the object is open.
     *
     * @throws IllegalStateException if the object is closed
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("SerializedObject is closed");
        }
    }

    /**
     * Releases the object ({@code iplSerializedObjectRelease}). Calling it
     * again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Returns the object opaque pointer for child objects of the package.
     *
     * @return opaque pointer to {@code IPLSerializedObject}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Creates an empty object via {@code iplSerializedObjectCreate}.
     *
     * @param contextPeer context opaque pointer
     * @return opaque pointer to the created object
     */
    private static native long nCreate(long contextPeer);

    /**
     * Returns the data size via {@code iplSerializedObjectGetSize}.
     *
     * @param peer object opaque pointer
     * @return data size in bytes
     */
    private static native long nGetSize(long peer);

    /**
     * Copies the object data into the destination array.
     *
     * @param peer object opaque pointer
     * @param out  destination array of length {@link #getSize()}
     */
    private static native void nGetData(long peer, byte[] out);

    /**
     * Releases the object via {@code iplSerializedObjectRelease}.
     *
     * @param peer object opaque pointer
     */
    private static native void nRelease(long peer);
}
