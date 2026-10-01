package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLProbeArray} — временным массивом проб,
 * сгенерированным по геометрии сцены. Пробы — сферы влияния, в которых
 * хранятся запеченные данные. Массив добавляется в {@link ProbeBatch}
 * для последующего запекания reflections/pathing.
 */
public final class ProbeArray implements AutoCloseable {

    /** Проба в центре тяжести каждого треугольника геометрии
     * ({@code IPL_PROBEGENERATIONTYPE_CENTROID}). */
    public static final int GENERATION_CENTROID = 0;

    /** Равномерная сетка проб над горизонтальной поверхностью
     * ({@code IPL_PROBEGENERATIONTYPE_UNIFORMFLOOR}). */
    public static final int GENERATION_UNIFORM_FLOOR = 1;

    /** Opaque-указатель на {@code IPLProbeArray}; 0 означает закрытый объект. */
    private long peer;

    /**
     * Создает пустой массив проб через {@code iplProbeArrayCreate}.
     *
     * @param context контекст Steam Audio
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public ProbeArray(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Проверяет, что массив открыт.
     *
     * @return {@code true}, если массив создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Генерирует пробы по геометрии сцены
     * ({@code iplProbeArrayGenerateProbes}).
     *
     * @param scene     открытая зафиксированная сцена
     * @param type      алгоритм генерации: {@link #GENERATION_CENTROID}
     *                  или {@link #GENERATION_UNIFORM_FLOOR}
     * @param spacing   расстояние между соседними пробами, м (только
     *                  {@code GENERATION_UNIFORM_FLOOR})
     * @param height    высота над полом, м (только
     *                  {@code GENERATION_UNIFORM_FLOOR})
     * @param transform строка-major матрица 4x4, отображающая единичный
     *                  куб {@code [0..1]^3} в объем генерации; {@code null} —
     *                  единичная матрица
     * @throws IllegalStateException если массив или сцена закрыты
     */
    public void generateProbes(Scene scene, int type, float spacing, float height, float[] transform) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("ProbeArray or Scene is closed");
        }
        if (transform != null && transform.length < 16) {
            throw new IllegalArgumentException("transform must contain 16 elements");
        }
        nGenerateProbes(peer, scene.peerForChildren(), type, spacing, height, transform);
    }

    /**
     * Возвращает число сгенерированных проб
     * ({@code iplProbeArrayGetNumProbes}).
     *
     * @return число проб
     */
    public int getNumProbes() {
        requireOpen();
        return nGetNumProbes(peer);
    }

    /**
     * Проверяет, что массив открыт.
     *
     * @throws IllegalStateException если массив закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ProbeArray is closed");
        }
    }

    /**
     * Освобождает массив ({@code iplProbeArrayRelease}). Повторный вызов
     * безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Возвращает opaque-указатель массива для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLProbeArray}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Создает пустой массив через {@code iplProbeArrayCreate}.
     *
     * @param contextPeer opaque-указатель контекста
     * @return opaque-указатель на созданный массив
     */
    private static native long nCreate(long contextPeer);

    /**
     * Генерирует пробы через {@code iplProbeArrayGenerateProbes}.
     *
     * @param peer        opaque-указатель массива
     * @param scenePeer   opaque-указатель сцены
     * @param type        алгоритм генерации ({@code IPLProbeGenerationType})
     * @param spacing     расстояние между пробами, м
     * @param height      высота над полом, м
     * @param transform   массив из 16 float (row-major 4x4) или {@code null}
     */
    private static native void nGenerateProbes(long peer, long scenePeer, int type,
                                               float spacing, float height, float[] transform);

    /**
     * Возвращает число проб через {@code iplProbeArrayGetNumProbes}.
     *
     * @param peer opaque-указатель массива
     * @return число проб
     */
    private static native int nGetNumProbes(long peer);

    /**
     * Освобождает массив через {@code iplProbeArrayRelease}.
     *
     * @param peer opaque-указатель массива
     */
    private static native void nRelease(long peer);
}
