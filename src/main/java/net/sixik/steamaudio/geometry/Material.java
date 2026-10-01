package net.sixik.steamaudio.geometry;
import net.sixik.steamaudio.core.SteamAudio;

/**
 * Acoustic properties of a surface, corresponding to {@code IPLMaterial}.
 * <p>
 * The properties are defined for three frequency bands with center
 * frequencies of 400 Hz, 2.5 kHz and 15 kHz ({@code IPL_NUM_BANDS} = 3).
 */
public final class Material {

    /** Number of frequency bands ({@code IPL_NUM_BANDS}). */
    public static final int NUM_BANDS = 3;

    /** Fraction of absorbed sound energy at low, mid and high
     * frequencies (0.0..1.0). */
    public final float[] absorption;

    /** Fraction of sound energy scattered in a random direction upon
     * reflection: 0.0 — purely specular, 1.0 — purely diffuse. */
    public final float scattering;

    /** Fraction of sound energy passing through the surface at low,
     * mid and high frequencies (0.0..1.0); used when calculating
     * direct attenuation (occlusion). */
    public final float[] transmission;

    /**
     * Creates a material with the given properties.
     *
     * @param absorption   array of {@value #NUM_BANDS} absorption values
     * @param scattering   scattering coefficient (0.0..1.0)
     * @param transmission array of {@value #NUM_BANDS} transmission values
     */
    public Material(float[] absorption, float scattering, float[] transmission) {
        if (absorption.length != NUM_BANDS || transmission.length != NUM_BANDS) {
            throw new IllegalArgumentException(
                    "absorption and transmission must contain " + NUM_BANDS + " values");
        }
        this.absorption = absorption.clone();
        this.scattering = scattering;
        this.transmission = transmission.clone();
    }

    /** Default material: {@code {"generic", ...}} from the Steam Audio documentation. */
    public static Material generic() {
        return new Material(new float[]{0.10f, 0.20f, 0.30f}, 0.05f,
                new float[]{0.100f, 0.050f, 0.030f});
    }

    /** The "brick" material from the Steam Audio documentation. */
    public static Material brick() {
        return new Material(new float[]{0.03f, 0.04f, 0.07f}, 0.05f,
                new float[]{0.015f, 0.015f, 0.015f});
    }

    /** The "concrete" material from the Steam Audio documentation. */
    public static Material concrete() {
        return new Material(new float[]{0.05f, 0.07f, 0.08f}, 0.05f,
                new float[]{0.015f, 0.002f, 0.001f});
    }

    /** The "glass" material from the Steam Audio documentation. */
    public static Material glass() {
        return new Material(new float[]{0.06f, 0.03f, 0.02f}, 0.05f,
                new float[]{0.060f, 0.044f, 0.011f});
    }

    /** The "wood" material from the Steam Audio documentation. */
    public static Material wood() {
        return new Material(new float[]{0.11f, 0.07f, 0.06f}, 0.05f,
                new float[]{0.070f, 0.014f, 0.005f});
    }

    /** The "metal" material from the Steam Audio documentation. */
    public static Material metal() {
        return new Material(new float[]{0.20f, 0.07f, 0.06f}, 0.05f,
                new float[]{0.200f, 0.025f, 0.010f});
    }

    /** The "carpet" material from the Steam Audio documentation. */
    public static Material carpet() {
        return new Material(new float[]{0.24f, 0.69f, 0.73f}, 0.05f,
                new float[]{0.020f, 0.005f, 0.003f});
    }
}
