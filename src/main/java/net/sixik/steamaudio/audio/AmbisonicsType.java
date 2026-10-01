package net.sixik.steamaudio.audio;

/**
 * Supported channel ordering and normalization schemes for Ambisonic audio,
 * mirroring {@code IPLAmbisonicsType}.
 * <p>
 * Steam Audio's native format is N3D; keep Ambisonic data in N3D internally
 * and convert at the boundaries (e.g. when exchanging AmbiX content).
 */
public final class AmbisonicsType {

    /** ACN channel ordering, orthonormal spherical harmonics (Steam Audio native). */
    public static final int N3D = 0;

    /** ACN channel ordering, semi-normalized spherical harmonics (AmbiX format). */
    public static final int SN3D = 1;

    /** Furse-Malham (B-format). */
    public static final int FUMA = 2;

    private AmbisonicsType() {
    }
}
