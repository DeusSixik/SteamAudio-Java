package net.sixik.steamaudio.audio;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.effects.BinauralEffect;

/**
 * Wrapper around {@code IPLHRTF} — a function describing how sound from
 * different directions is perceived by each of the listener's ears.
 * <p>
 * Steam Audio includes a built-in HRTF ({@code HRTF_TYPE_DEFAULT}); a custom
 * one can also be loaded from a SOFA file. Creating an HRTF is relatively
 * expensive and not thread-safe — do not do it on the audio thread.
 */
public final class HRTF implements AutoCloseable {

    /** Built-in Steam Audio HRTF ({@code IPL_HRTFTYPE_DEFAULT}). */
    public static final int TYPE_DEFAULT = 0;

    /** HRTF from a SOFA file ({@code IPL_HRTFTYPE_SOFA}). */
    public static final int TYPE_SOFA = 1;

    /** No normalization ({@code IPL_HRTFNORMTYPE_NONE}). */
    public static final int NORM_TYPE_NONE = 0;

    /** RMS normalization: equal loudness from all directions
     * ({@code IPL_HRTFNORMTYPE_RMS}). */
    public static final int NORM_TYPE_RMS = 1;

    /** Opaque pointer to {@code IPLHRTF}; 0 means the object is closed. */
    private long peer;

    /**
     * Creates an HRTF with the default settings: built-in HRTF, volume
     * 1.0, no normalization.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples (independent of the number of channels)
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public HRTF(Context context, int samplingRate, int frameSize) {
        this(context, samplingRate, frameSize, TYPE_DEFAULT, 1.0f, NORM_TYPE_NONE);
    }

    /**
     * Creates an HRTF with a full set of settings.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         HRTF type: {@link #TYPE_DEFAULT} or {@link #TYPE_SOFA}
     * @param volume       volume adjustment; 1.0 — no change
     * @param normType     normalization type: {@link #NORM_TYPE_NONE} or
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public HRTF(Context context, int samplingRate, int frameSize, int type, float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, volume, normType);
    }

    /**
     * Creates an HRTF from a SOFA file with a full set of settings.
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param sofaFileName path to the SOFA file with HRTF data
     * @param volume       volume adjustment; 1.0 — no change
     * @param normType     normalization type: {@link #NORM_TYPE_NONE} or
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public HRTF(Context context, int samplingRate, int frameSize, String sofaFileName,
                float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreateSofaFile(context.peerForChildren(), samplingRate, frameSize, volume, normType, sofaFileName);
    }

    /**
     * Creates an HRTF from a buffer with SOFA file data (without accessing
     * the file system).
     *
     * @param context      Steam Audio context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param sofaData     SOFA file bytes
     * @param volume       volume adjustment; 1.0 — no change
     * @param normType     normalization type: {@link #NORM_TYPE_NONE} or
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public HRTF(Context context, int samplingRate, int frameSize, byte[] sofaData,
                float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreateSofaData(context.peerForChildren(), samplingRate, frameSize, volume, normType, sofaData);
    }

    /**
     * Checks whether the HRTF is open.
     *
     * @return {@code true} if the HRTF has been created and is not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the opaque HRTF pointer for child objects of the package
     * (e.g. {@code BinauralEffect}).
     *
     * @return opaque pointer to {@code IPLHRTF}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Releases the HRTF ({@code iplHRTFRelease}). Calling it again is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Creates an HRTF via {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque pointer of the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param type         HRTF type ({@code IPLHRTFType})
     * @param volume       volume adjustment
     * @param normType     normalization type ({@code IPLHRTFNormType})
     * @return opaque pointer to the created HRTF
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, float volume, int normType);

    /**
     * Releases the HRTF via {@code iplHRTFRelease}.
     *
     * @param peer opaque pointer to the HRTF
     */
    private static native void nRelease(long peer);

    /**
     * Creates an HRTF from a SOFA file via {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque pointer of the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param volume       volume adjustment
     * @param normType     normalization type ({@code IPLHRTFNormType})
     * @param sofaFileName path to the SOFA file
     * @return opaque pointer to the created HRTF
     */
    private static native long nCreateSofaFile(long contextPeer, int samplingRate, int frameSize,
                                               float volume, int normType, String sofaFileName);

    /**
     * Creates an HRTF from a SOFA data buffer via {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque pointer of the context
     * @param samplingRate sampling rate, Hz
     * @param frameSize    frame size in samples
     * @param volume       volume adjustment
     * @param normType     normalization type ({@code IPLHRTFNormType})
     * @param sofaData     SOFA file bytes
     * @return opaque pointer to the created HRTF
     */
    private static native long nCreateSofaData(long contextPeer, int samplingRate, int frameSize,
                                               float volume, int normType, byte[] sofaData);
}
