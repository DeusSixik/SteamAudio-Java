package net.sixik.steamaudio.core;

/**
 * Wrapper around {@code IPLContext} — the Steam Audio context.
 * <p>
 * The context manages low-level Steam Audio operations and must be created
 * before any other API objects. It is usually created once for the lifetime
 * of the application.
 * <p>
 * The handle is stored as a {@code long} (opaque pointer) and is passed to
 * the native methods directly, without creating a JObject — no holder for
 * the Java object is needed.
 */
public final class Context implements AutoCloseable {

    /** Steam Audio API version, equal to {@code STEAMAUDIO_VERSION} (4.8.1). */
    public static final int API_VERSION = (4 << 16) | (8 << 8) | 1;

    /** SIMD: SSE2 (up to 4 simultaneous float operations). */
    public static final int SIMD_LEVEL_SSE2 = 0;

    /** SIMD: SSE4.2 or newer (up to 4 simultaneous float operations). */
    public static final int SIMD_LEVEL_SSE4 = 1;

    /** SIMD: AVX or newer (up to 8 simultaneous float operations). */
    public static final int SIMD_LEVEL_AVX = 2;

    /** SIMD: AVX2 or newer (up to 8 simultaneous float operations). */
    public static final int SIMD_LEVEL_AVX2 = 3;

    /** SIMD: AVX-512 or newer (up to 16 simultaneous float operations). */
    public static final int SIMD_LEVEL_AVX512 = 4;

    /**
     * Sentinel for automatic SIMD level selection: resolved from the
     * {@code steamaudio.simdLevel} system property (a level name like
     * {@code "avx"} or an integer 0..4), falling back to
     * {@link #SIMD_LEVEL_AVX}. AVX is the safest non-trivial level: CPUs
     * without AVX2 (e.g. Intel i5-3470 / Ivy Bridge and older) cannot
     * execute AVX2 code. Note that the official {@code phonon.dll} also
     * caps the requested level at what the CPU supports (via IPP), so
     * requesting AVX2 on older hardware is still safe — but an explicit
     * AVX removes any dependence on that behavior.
     */
    public static final int SIMD_LEVEL_AUTO = -1;

    /** Flag: all API functions perform additional validation. Slows down execution. */
    public static final int CONTEXT_FLAG_VALIDATION = 1;

    /** System property selecting the SIMD level for {@link #SIMD_LEVEL_AUTO}. */
    public static final String SIMD_LEVEL_PROPERTY = "steamaudio.simdLevel";

    /** Opaque pointer to {@code IPLContext}; 0 means the context is closed. */
    private long peer;

    /**
     * Creates the context with default settings: automatic SIMD level
     * selection (see {@link #SIMD_LEVEL_AUTO}; the effective default is
     * AVX) and no additional flags.
     *
     * @throws SteamAudioException if Steam Audio returned a creation error
     */
    public Context() {
        this(SIMD_LEVEL_AUTO, 0);
    }

    /**
     * Creates the context with the given settings.
     *
     * @param simdLevel maximum SIMD level that Steam Audio may use; one of
     *                  the {@code SIMD_LEVEL_*} values, or
     *                  {@link #SIMD_LEVEL_AUTO} to resolve from the
     *                  {@code steamaudio.simdLevel} system property
     *                  (falling back to {@link #SIMD_LEVEL_AVX})
     * @param flags     combination of {@code CONTEXT_FLAG_*} flags; 0 for no flags
     * @throws SteamAudioException if Steam Audio returned a creation error
     */
    public Context(int simdLevel, int flags) {
        peer = nCreate(API_VERSION, resolveSimdLevel(simdLevel), flags);
    }

    /**
     * Resolves {@code SIMD_LEVEL_AUTO} (or any negative value) against the
     * {@code steamaudio.simdLevel} system property. The property accepts
     * either a level name ({@code sse2}, {@code sse4}, {@code avx},
     * {@code avx2}, {@code avx512}, case-insensitive) or an integer 0..4.
     * Anything invalid falls back to {@code SIMD_LEVEL_AVX}.
     *
     * @param simdLevel requested level
     * @return the effective SIMD level
     */
    static int resolveSimdLevel(int simdLevel) {
        if (simdLevel >= 0) {
            return simdLevel;
        }
        String property = System.getProperty(SIMD_LEVEL_PROPERTY, "avx").trim().toLowerCase();
        switch (property) {
            case "sse2":
            case "0": return SIMD_LEVEL_SSE2;
            case "sse4":
            case "1": return SIMD_LEVEL_SSE4;
            case "avx":
            case "2": return SIMD_LEVEL_AVX;
            case "avx2":
            case "3": return SIMD_LEVEL_AVX2;
            case "avx512":
            case "4": return SIMD_LEVEL_AVX512;
            default: return SIMD_LEVEL_AVX;
        }
    }

    /**
     * Checks that the context is open.
     *
     * @return {@code true} if the context is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the context opaque pointer for child objects of the package
     * (audio buffers, effects) that need it to call context-dependent
     * functions.
     *
     * @return opaque pointer to {@code IPLContext}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Calculates the relative direction from the listener to the source
     * ({@code iplCalculateRelativeDirection}); the result is returned as a
     * new {@link Vector3}. For the hot path, use the allocation-free overload
     * {@link #calculateRelativeDirection(Vector3, Vector3, Vector3, Vector3, float[])}.
     *
     * @param sourcePosition    world-space coordinates of the source
     * @param listenerPosition  world-space coordinates of the listener
     * @param listenerAhead     unit world-space "ahead" vector of the listener
     * @param listenerUp        unit world-space "up" vector of the listener
     * @return unit vector in the listener coordinate system, pointing
     *         from the listener to the source
     */
    public Vector3 calculateRelativeDirection(Vector3 sourcePosition, Vector3 listenerPosition,
                                             Vector3 listenerAhead, Vector3 listenerUp) {
        float[] out = new float[3];
        calculateRelativeDirection(sourcePosition, listenerPosition, listenerAhead, listenerUp, out);
        return new Vector3(out[0], out[1], out[2]);
    }

    /**
     * Calculates the relative direction from the listener to the source and
     * writes it into the given array (allocation-free variant for the
     * hot path).
     *
     * @param sourcePosition    world-space coordinates of the source
     * @param listenerPosition  world-space coordinates of the listener
     * @param listenerAhead     unit world-space "ahead" vector of the listener
     * @param listenerUp        unit world-space "up" vector of the listener
     * @param out array of length at least 3; receives the (x, y, z) coordinates
     */
    public void calculateRelativeDirection(Vector3 sourcePosition, Vector3 listenerPosition,
                                           Vector3 listenerAhead, Vector3 listenerUp, float[] out) {
        calculateRelativeDirection(
                sourcePosition.x, sourcePosition.y, sourcePosition.z,
                listenerPosition.x, listenerPosition.y, listenerPosition.z,
                listenerAhead.x, listenerAhead.y, listenerAhead.z,
                listenerUp.x, listenerUp.y, listenerUp.z, out);
    }

    /**
     * Calculates the relative direction from the listener to the source and
     * writes it into the given array. Object- and allocation-free version
     * for the hot path.
     *
     * @param sourceX sourceY sourceZ    world-space coordinates of the source
     * @param listenerX listenerY listenerZ world-space coordinates of the listener
     * @param aheadX aheadY aheadZ       unit world-space "ahead" vector of the listener
     * @param upX upY upZ                unit world-space "up" vector of the listener
     * @param out array of length at least 3; receives the (x, y, z) coordinates
     */
    public void calculateRelativeDirection(float sourceX, float sourceY, float sourceZ,
                                           float listenerX, float listenerY, float listenerZ,
                                           float aheadX, float aheadY, float aheadZ,
                                           float upX, float upY, float upZ, float[] out) {
        if (peer == 0) {
            throw new IllegalStateException("Context is closed");
        }
        if (out.length < 3) {
            throw new IllegalArgumentException("out must contain at least 3 elements");
        }
        nCalculateRelativeDirection(peer, sourceX, sourceY, sourceZ,
                listenerX, listenerY, listenerZ,
                aheadX, aheadY, aheadZ,
                upX, upY, upZ, out);
    }

    /**
     * Calculates distance attenuation between a source and a listener
     * ({@code iplDistanceAttenuationCalculate}) and writes it to the output
     * array without allocations.
     *
     * @param modelType attenuation model ({@code IPLDistanceAttenuationModelType}):
     *                  0 = default (inverse distance with a 1 m plateau),
     *                  1 = inverse distance with a configurable minimum distance
     * @param minDistance no attenuation applied below this distance, meters
     *                    (only for {@code INVERSE_DISTANCE})
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out array of at least 1 element; receives the attenuation (0..1)
     */
    public void calculateDistanceAttenuation(int modelType, float minDistance,
                                             float sourceX, float sourceY, float sourceZ,
                                             float listenerX, float listenerY, float listenerZ,
                                             float[] out) {
        if (peer == 0) {
            throw new IllegalStateException("Context is closed");
        }
        if (out.length < 1) {
            throw new IllegalArgumentException("out must contain at least 1 element");
        }
        nCalculateDistanceAttenuation(peer, modelType, minDistance,
                sourceX, sourceY, sourceZ, listenerX, listenerY, listenerZ, out);
    }

    /**
     * Convenience overload of {@link #calculateDistanceAttenuation} returning
     * a new value (allocates nothing on the native side).
     *
     * @param modelType attenuation model type
     * @param minDistance minimum distance, meters
     * @param sourcePosition world-space position of the source
     * @param listenerPosition world-space position of the listener
     * @return distance attenuation, between 0 and 1
     */
    public float calculateDistanceAttenuation(int modelType, float minDistance,
                                              Vector3 sourcePosition, Vector3 listenerPosition) {
        float[] out = new float[1];
        calculateDistanceAttenuation(modelType, minDistance,
                sourcePosition.x, sourcePosition.y, sourcePosition.z,
                listenerPosition.x, listenerPosition.y, listenerPosition.z, out);
        return out[0];
    }

    /**
     * Calculates 3-band air absorption coefficients between a source and a
     * listener ({@code iplAirAbsorptionCalculate}) writing into the output
     * array without allocations.
     *
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out array of at least 3 elements; receives the coefficients
     *            (low/mid/high), each between 0 and 1
     */
    public void calculateAirAbsorption(float sourceX, float sourceY, float sourceZ,
                                       float listenerX, float listenerY, float listenerZ,
                                       float[] out) {
        if (peer == 0) {
            throw new IllegalStateException("Context is closed");
        }
        if (out.length < 3) {
            throw new IllegalArgumentException("out must contain at least 3 elements");
        }
        nCalculateAirAbsorption(peer, sourceX, sourceY, sourceZ,
                listenerX, listenerY, listenerZ, out);
    }

    /**
     * Convenience overload of {@link #calculateAirAbsorption}.
     *
     * @param sourcePosition world-space position of the source
     * @param listenerPosition world-space position of the listener
     * @return new array of 3 coefficients (low/mid/high)
     */
    public float[] calculateAirAbsorption(Vector3 sourcePosition, Vector3 listenerPosition) {
        float[] out = new float[3];
        calculateAirAbsorption(sourcePosition.x, sourcePosition.y, sourcePosition.z,
                listenerPosition.x, listenerPosition.y, listenerPosition.z, out);
        return out;
    }

    /**
     * Calculates the directivity term of a source with the given orientation
     * relative to a listener ({@code iplDirectivityCalculate}), writing into
     * the output array without allocations.
     *
     * @param dipoleWeight how much of the dipole to blend in: 0 = pure
     *                     omnidirectional, 1 = pure dipole, 0.5 = cardioid
     * @param dipolePower  dipole sharpness; higher values focus sound into a
     *                     narrower range of directions
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param sourceAheadX sourceAheadY sourceAheadZ unit vector the source points towards
     * @param sourceUpX sourceUpY sourceUpZ unit "up" vector of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out array of at least 1 element; receives the directivity value (0..1)
     */
    public void calculateDirectivity(float dipoleWeight, float dipolePower,
                                     float sourceX, float sourceY, float sourceZ,
                                     float sourceAheadX, float sourceAheadY, float sourceAheadZ,
                                     float sourceUpX, float sourceUpY, float sourceUpZ,
                                     float listenerX, float listenerY, float listenerZ,
                                     float[] out) {
        if (peer == 0) {
            throw new IllegalStateException("Context is closed");
        }
        if (out.length < 1) {
            throw new IllegalArgumentException("out must contain at least 1 element");
        }
        nCalculateDirectivity(peer, dipoleWeight, dipolePower,
                sourceX, sourceY, sourceZ,
                sourceAheadX, sourceAheadY, sourceAheadZ,
                sourceUpX, sourceUpY, sourceUpZ,
                listenerX, listenerY, listenerZ, out);
    }

    /**
     * Convenience overload of {@link #calculateDirectivity}.
     *
     * @param dipoleWeight dipole blend (0..1)
     * @param dipolePower  dipole sharpness
     * @param sourcePosition world-space position of the source
     * @param sourceAhead  unit vector the source points towards
     * @param sourceUp     unit "up" vector of the source
     * @param listenerPosition world-space position of the listener
     * @return directivity value, between 0 and 1
     */
    public float calculateDirectivity(float dipoleWeight, float dipolePower,
                                      Vector3 sourcePosition, Vector3 sourceAhead, Vector3 sourceUp,
                                      Vector3 listenerPosition) {
        float[] out = new float[1];
        calculateDirectivity(dipoleWeight, dipolePower,
                sourcePosition.x, sourcePosition.y, sourcePosition.z,
                sourceAhead.x, sourceAhead.y, sourceAhead.z,
                sourceUp.x, sourceUp.y, sourceUp.z,
                listenerPosition.x, listenerPosition.y, listenerPosition.z, out);
        return out[0];
    }

    /**
     * Releases the Steam Audio context (calls {@code iplContextRelease}).
     * Safe to call multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Creates {@code IPLContext} via {@code iplContextCreate}.
     *
     * @param version   API version ({@code STEAMAUDIO_VERSION})
     * @param simdLevel maximum SIMD level ({@code IPLSIMDLevel})
     * @param flags     context flags ({@code IPLContextFlags})
     * @return opaque pointer to the created context
     */
    private static native long nCreate(int version, int simdLevel, int flags);

    /**
     * Releases the context via {@code iplContextRelease}.
     *
     * @param peer opaque pointer to the context
     */
    private static native void nRelease(long peer);

    /**
     * Calculates the relative direction from the listener to the source via
     * {@code iplCalculateRelativeDirection}.
     *
     * @param peer opaque pointer to the context
     * @param sourceX sourceY sourceZ world-space coordinates of the source
     * @param listenerX listenerY listenerZ world-space coordinates of the listener
     * @param aheadX aheadY aheadZ unit world-space "ahead" vector of the listener
     * @param upX upY upZ unit world-space "up" vector of the listener
     * @param out destination array (at least 3 elements)
     */
    private static native void nCalculateRelativeDirection(long peer,
                                                           float sourceX, float sourceY, float sourceZ,
                                                           float listenerX, float listenerY, float listenerZ,
                                                           float aheadX, float aheadY, float aheadZ,
                                                           float upX, float upY, float upZ,
                                                           float[] out);

    /**
     * Calculates distance attenuation via {@code iplDistanceAttenuationCalculate}.
     *
     * @param peer opaque pointer to the context
     * @param modelType attenuation model ({@code IPLDistanceAttenuationModelType})
     * @param minDistance minimum distance, meters
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out output array (at least 1 element)
     */
    private static native void nCalculateDistanceAttenuation(long peer, int modelType, float minDistance,
                                                             float sourceX, float sourceY, float sourceZ,
                                                             float listenerX, float listenerY, float listenerZ,
                                                             float[] out);

    /**
     * Calculates air absorption via {@code iplAirAbsorptionCalculate}.
     *
     * @param peer opaque pointer to the context
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out output array (at least 3 elements)
     */
    private static native void nCalculateAirAbsorption(long peer,
                                                       float sourceX, float sourceY, float sourceZ,
                                                       float listenerX, float listenerY, float listenerZ,
                                                       float[] out);

    /**
     * Calculates the directivity term via {@code iplDirectivityCalculate}.
     *
     * @param peer opaque pointer to the context
     * @param dipoleWeight dipole blend (0..1)
     * @param dipolePower  dipole sharpness
     * @param sourceX sourceY sourceZ world-space position of the source
     * @param sourceAheadX sourceAheadY sourceAheadZ unit vector the source points towards
     * @param sourceUpX sourceUpY sourceUpZ unit "up" vector of the source
     * @param listenerX listenerY listenerZ world-space position of the listener
     * @param out output array (at least 1 element)
     */
    private static native void nCalculateDirectivity(long peer, float dipoleWeight, float dipolePower,
                                                     float sourceX, float sourceY, float sourceZ,
                                                     float sourceAheadX, float sourceAheadY, float sourceAheadZ,
                                                     float sourceUpX, float sourceUpY, float sourceUpZ,
                                                     float listenerX, float listenerY, float listenerZ,
                                                     float[] out);

    static {
        SteamAudio.load();
    }
}
