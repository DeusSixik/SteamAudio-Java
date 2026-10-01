package net.sixik.steamaudio;

/**
 * Запекатель pathing-данных: просчитывает пути между всеми парами проб
 * в probe batch по геометрии сцены и сохраняет результат в batch
 * ({@code iplPathBakerBake}). Запекание — дорогая внереалтайм-операция;
 * одновременно может выполняться только одно запекание на контекст.
 */
public final class PathBaker {

    private PathBaker() {
    }

    /**
     * Запекает слой pathing-данных в probe batch. Если batch уже содержит
     * данные с этим идентификатором, они будут перезаписаны.
     * <p>
     * Идентификатор слоя: {@code type = PATHING, variation = DYNAMIC} —
     * стандартная раскладка для реалтайм pathing-симуляции.
     *
     * @param context    контекст Steam Audio
     * @param scene      открытая зафиксированная сцена
     * @param probeBatch открытый probe batch с пробами
     * @param numSamples число point-сэмплов вокруг пробы при проверке
     *                   взаимной видимости (numSamples × numSamples лучей
     *                   на пару проб)
     * @param radius     радиус сферы пробы при проверке видимости, м
     * @param threshold  порог доли незаблокированных лучей для признания
     *                   пары проб взаимно видимой (0..1)
     * @param visRange   максимальная дистанция взаимной видимости проб, м
     * @param pathRange  максимальная длина пути между пробами, м
     * @param numThreads число потоков запекания
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст, сцена или batch закрыты
     */
    public static void bake(Context context, Scene scene, ProbeBatch probeBatch,
                            int numSamples, float radius, float threshold,
                            float visRange, float pathRange, int numThreads) {
        if (!context.isOpen() || scene == null || !scene.isOpen()
                || probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("Context, Scene or ProbeBatch is closed");
        }
        nBake(context.peerForChildren(), scene.peerForChildren(), probeBatch.peerForChildren(),
                numSamples, radius, threshold, visRange, pathRange, numThreads);
    }

    /**
     * Отменяет выполняемое запекание pathing-данных
     * ({@code iplPathBakerCancelBake}).
     *
     * @param context контекст Steam Audio
     */
    public static void cancelBake(Context context) {
        nCancelBake(context.peerForChildren());
    }

    /**
     * Запекает pathing-данные через {@code iplPathBakerBake}.
     *
     * @param contextPeer   opaque-указатель контекста
     * @param scenePeer     opaque-указатель сцены
     * @param probeBatchPeer opaque-указатель probe batch
     * @param numSamples    число point-сэмплов видимости
     * @param radius        радиус сферы пробы, м
     * @param threshold     порог видимости (0..1)
     * @param visRange      дистанция взаимной видимости, м
     * @param pathRange     максимальная длина пути, м
     * @param numThreads    число потоков
     */
    private static native void nBake(long contextPeer, long scenePeer, long probeBatchPeer,
                                     int numSamples, float radius, float threshold,
                                     float visRange, float pathRange, int numThreads);

    /**
     * Отменяет запекание через {@code iplPathBakerCancelBake}.
     *
     * @param contextPeer opaque-указатель контекста
     */
    private static native void nCancelBake(long contextPeer);
}
