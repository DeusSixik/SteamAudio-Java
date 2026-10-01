package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLContext} — контекстом Steam Audio.
 * <p>
 * Контекст управляет низкоуровневыми операциями Steam Audio и должен быть
 * создан до создания любых других API-объектов. Обычно создается один раз
 * за время жизни приложения.
 * <p>
 * Handle хранится как {@code long} (opaque pointer) и передается в
 * нативные методы напрямую, без создания JObject — держатель на Java-объект
 * не нужен.
 */
public final class Context implements AutoCloseable {

    /** Версия Steam Audio API, равная {@code STEAMAUDIO_VERSION} (4.8.1). */
    public static final int API_VERSION = (4 << 16) | (8 << 8) | 1;

    /** SIMD: SSE2 (до 4 одновременных float-операций). */
    public static final int SIMD_LEVEL_SSE2 = 0;

    /** SIMD: SSE4.2 или старше (до 4 одновременных float-операций). */
    public static final int SIMD_LEVEL_SSE4 = 1;

    /** SIMD: AVX или старше (до 8 одновременных float-операций). */
    public static final int SIMD_LEVEL_AVX = 2;

    /** SIMD: AVX2 или старше (до 8 одновременных float-операций). */
    public static final int SIMD_LEVEL_AVX2 = 3;

    /** SIMD: AVX-512 или старше (до 16 одновременных float-операций). */
    public static final int SIMD_LEVEL_AVX512 = 4;

    /** Флаг: все API-функции выполняют дополнительные проверки. Замедляет работу. */
    public static final int CONTEXT_FLAG_VALIDATION = 1;

    /** Opaque-указатель на {@code IPLContext}; 0 означает закрытый контекст. */
    private long peer;

    /**
     * Создает контекст с настройками по умолчанию: максимальный SIMD-уровень
     * AVX2 и без дополнительных флагов.
     *
     * @throws SteamAudioException если Steam Audio вернул ошибку создания
     */
    public Context() {
        this(SIMD_LEVEL_AVX2, 0);
    }

    /**
     * Создает контекст с заданными настройками.
     *
     * @param simdLevel максимальный SIMD-уровень, который Steam Audio может
     *                  использовать; одно из значений {@code SIMD_LEVEL_*}
     * @param flags     комбинация флагов {@code CONTEXT_FLAG_*}; 0 — без флагов
     * @throws SteamAudioException если Steam Audio вернул ошибку создания
     */
    public Context(int simdLevel, int flags) {
        peer = nCreate(API_VERSION, simdLevel, flags);
    }

    /**
     * Проверяет, что контекст открыт.
     *
     * @return {@code true}, если контекст создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Возвращает opaque-указатель контекста для дочерних объектов пакета
     * (аудио-буферов, эффектов), которым он нужен для вызова
     * контекстно-зависимых функций.
     *
     * @return opaque-указатель на {@code IPLContext}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Вычисляет относительное направление от слушателя к источнику
     * ({@code iplCalculateRelativeDirection}); результат возвращается как
     * новый {@link Vector3}. Для хот-паса используйте перегрузку
     * {@link #calculateRelativeDirection(Vector3, Vector3, Vector3, Vector3, float[])}
     * без аллокации.
     *
     * @param sourcePosition    мировые координаты источника
     * @param listenerPosition  мировые координаты слушателя
     * @param listenerAhead     единичный мировой вектор «вперед» слушателя
     * @param listenerUp        единичный мировой вектор «вверх» слушателя
     * @return единичный вектор в системе координат слушателя, указывающий
     *         от слушателя к источнику
     */
    public Vector3 calculateRelativeDirection(Vector3 sourcePosition, Vector3 listenerPosition,
                                             Vector3 listenerAhead, Vector3 listenerUp) {
        float[] out = new float[3];
        calculateRelativeDirection(sourcePosition, listenerPosition, listenerAhead, listenerUp, out);
        return new Vector3(out[0], out[1], out[2]);
    }

    /**
     * Вычисляет относительное направление от слушателя к источнику и
     * записывает его в переданный массив (вариант без аллокации для
     * хот-паса).
     *
     * @param sourcePosition    мировые координаты источника
     * @param listenerPosition  мировые координаты слушателя
     * @param listenerAhead     единичный мировой вектор «вперед» слушателя
     * @param listenerUp        единичный мировой вектор «вверх» слушателя
     * @param out массив длиной не менее 3; получает координаты (x, y, z)
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
     * Вычисляет относительное направление от слушателя к источнику и
     * записывает его в переданный массив. Версия без объектов и аллокаций
     * для хот-паса.
     *
     * @param sourceX sourceY sourceZ    мировые координаты источника
     * @param listenerX listenerY listenerZ мировые координаты слушателя
     * @param aheadX aheadY aheadZ       единичный мировой вектор «вперед» слушателя
     * @param upX upY upZ                единичный мировой вектор «вверх» слушателя
     * @param out массив длиной не менее 3; получает координаты (x, y, z)
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
     * Освобождает контекст Steam Audio (вызывает {@code iplContextRelease}).
     * Повторный вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Создает {@code IPLContext} через {@code iplContextCreate}.
     *
     * @param version   версия API ({@code STEAMAUDIO_VERSION})
     * @param simdLevel максимальный SIMD-уровень ({@code IPLSIMDLevel})
     * @param flags     флаги контекста ({@code IPLContextFlags})
     * @return opaque-указатель на созданный контекст
     */
    private static native long nCreate(int version, int simdLevel, int flags);

    /**
     * Освобождает контекст через {@code iplContextRelease}.
     *
     * @param peer opaque-указатель на контекст
     */
    private static native void nRelease(long peer);

    /**
     * Вычисляет относительное направление от слушателя к источнику через
     * {@code iplCalculateRelativeDirection}.
     *
     * @param peer opaque-указатель на контекст
     * @param sourceX sourceY sourceZ мировые координаты источника
     * @param listenerX listenerY listenerZ мировые координаты слушателя
     * @param aheadX aheadY aheadZ единичный мировой вектор «вперед» слушателя
     * @param upX upY upZ единичный мировой вектор «вверх» слушателя
     * @param out массив назначения (не менее 3 элементов)
     */
    private static native void nCalculateRelativeDirection(long peer,
                                                           float sourceX, float sourceY, float sourceZ,
                                                           float listenerX, float listenerY, float listenerZ,
                                                           float aheadX, float aheadY, float aheadZ,
                                                           float upX, float upY, float upZ,
                                                           float[] out);

    static {
        SteamAudio.load();
    }
}
