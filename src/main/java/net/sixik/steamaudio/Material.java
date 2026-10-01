package net.sixik.steamaudio;

/**
 * Акустические свойства поверхности, соответствующие {@code IPLMaterial}.
 * <p>
 * Свойства заданы для трех частотных полос с центральными частотами
 * 400 Гц, 2.5 кГц и 15 кГц ({@code IPL_NUM_BANDS} = 3).
 */
public final class Material {

    /** Число частотных полос ({@code IPL_NUM_BANDS}). */
    public static final int NUM_BANDS = 3;

    /** Доля поглощенной звуковой энергии на низких, средних и высоких
     * частотах (0.0..1.0). */
    public final float[] absorption;

    /** Доля звуковой энергии, рассеянной в случайном направлении при
     * отражении: 0.0 — чистое зеркальное, 1.0 — чистое диффузное. */
    public final float scattering;

    /** Доля звуковой энергии, проходящей сквозь поверхность, на низких,
     * средних и высоких частотах (0.0..1.0); используется при расчете
     * прямого затухания (occlusion). */
    public final float[] transmission;

    /**
     * Создает материал с заданными свойствами.
     *
     * @param absorption   массив из {@value #NUM_BANDS} значений поглощения
     * @param scattering   коэффициент рассеяния (0.0..1.0)
     * @param transmission массив из {@value #NUM_BANDS} значений прозрачности
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

    /** Материал по умолчанию: {@code {"generic", ...}} из документации Steam Audio. */
    public static Material generic() {
        return new Material(new float[]{0.10f, 0.20f, 0.30f}, 0.05f,
                new float[]{0.100f, 0.050f, 0.030f});
    }

    /** Материал «кирпич» из документации Steam Audio. */
    public static Material brick() {
        return new Material(new float[]{0.03f, 0.04f, 0.07f}, 0.05f,
                new float[]{0.015f, 0.015f, 0.015f});
    }

    /** Материал «бетон» из документации Steam Audio. */
    public static Material concrete() {
        return new Material(new float[]{0.05f, 0.07f, 0.08f}, 0.05f,
                new float[]{0.015f, 0.002f, 0.001f});
    }

    /** Материал «стекло» из документации Steam Audio. */
    public static Material glass() {
        return new Material(new float[]{0.06f, 0.03f, 0.02f}, 0.05f,
                new float[]{0.060f, 0.044f, 0.011f});
    }

    /** Материал «дерево» из документации Steam Audio. */
    public static Material wood() {
        return new Material(new float[]{0.11f, 0.07f, 0.06f}, 0.05f,
                new float[]{0.070f, 0.014f, 0.005f});
    }

    /** Материал «металл» из документации Steam Audio. */
    public static Material metal() {
        return new Material(new float[]{0.20f, 0.07f, 0.06f}, 0.05f,
                new float[]{0.200f, 0.025f, 0.010f});
    }

    /** Материал «ковер» из документации Steam Audio. */
    public static Material carpet() {
        return new Material(new float[]{0.24f, 0.69f, 0.73f}, 0.05f,
                new float[]{0.020f, 0.005f, 0.003f});
    }
}
