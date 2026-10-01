package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLProbeBatch} — набором проб, в котором хранятся
 * запеченные слои данных (reflections, pathing). В режиме реального
 * времени симуляции используют данные из прикрепленного к
 * {@link Simulator} probe batch.
 */
public final class ProbeBatch implements AutoCloseable {

    /** Opaque-указатель на {@code IPLProbeBatch}; 0 означает закрытый объект. */
    private long peer;

    /**
     * Создает пустой probe batch через {@code iplProbeBatchCreate}.
     *
     * @param context контекст Steam Audio
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public ProbeBatch(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Проверяет, что batch открыт.
     *
     * @return {@code true}, если batch создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Добавляет все пробы массива в batch
     * ({@code iplProbeBatchAddProbeArray}).
     *
     * @param probeArray открытый массив проб
     */
    public void addProbeArray(ProbeArray probeArray) {
        requireOpen();
        if (probeArray == null || !probeArray.isOpen()) {
            throw new IllegalStateException("ProbeArray is closed");
        }
        nAddProbeArray(peer, probeArray.peerForChildren());
    }

    /**
     * Добавляет одиночную пробу в batch ({@code iplProbeBatchAddProbe}).
     *
     * @param centerX centerY centerZ координаты центра пробы
     * @param radius  радиус сферы влияния пробы, м
     */
    public void addProbe(float centerX, float centerY, float centerZ, float radius) {
        requireOpen();
        nAddProbe(peer, centerX, centerY, centerZ, radius);
    }

    /**
     * Коммитит изменения batch ({@code iplProbeBatchCommit}); обязательно
     * после добавления/удаления проб и перед запеканием.
     */
    public void commit() {
        requireOpen();
        nCommit(peer);
    }

    /**
     * Возвращает число проб в batch ({@code iplProbeBatchGetNumProbes}).
     *
     * @return число проб
     */
    public int getNumProbes() {
        requireOpen();
        return nGetNumProbes(peer);
    }

    /**
     * Сохраняет probe batch (включая запеченные данные) в сериализованный
     * объект ({@code iplProbeBatchSave}).
     *
     * @param destination открытый сериализованный объект
     */
    public void save(SerializedObject destination) {
        requireOpen();
        if (!destination.isOpen()) {
            throw new IllegalStateException("SerializedObject is closed");
        }
        nSave(peer, destination.peerForChildren());
    }

    /**
     * Загружает probe batch из сериализованного объекта
     * ({@code iplProbeBatchLoad}).
     *
     * @param context контекст Steam Audio
     * @param source  сериализованный объект с данными batch
     * @return загруженный probe batch
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public static ProbeBatch load(Context context, SerializedObject source) {
        if (!context.isOpen() || !source.isOpen()) {
            throw new IllegalStateException("Context or SerializedObject is closed");
        }
        return new ProbeBatch(nLoad(context.peerForChildren(), source.peerForChildren()));
    }

    /**
     * Проверяет, что batch открыт.
     *
     * @throws IllegalStateException если batch закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
    }

    /**
     * Освобождает batch ({@code iplProbeBatchRelease}). Повторный вызов
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
     * Возвращает opaque-указатель batch для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLProbeBatch}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Создает обертку вокруг существующего handle (используется
     * в {@link #load}).
     *
     * @param peer opaque-указатель на {@code IPLProbeBatch}
     */
    private ProbeBatch(long peer) {
        this.peer = peer;
    }

    /**
     * Создает пустой batch через {@code iplProbeBatchCreate}.
     *
     * @param contextPeer opaque-указатель контекста
     * @return opaque-указатель на созданный batch
     */
    private static native long nCreate(long contextPeer);

    /**
     * Добавляет массив проб через {@code iplProbeBatchAddProbeArray}.
     *
     * @param peer           opaque-указатель batch
     * @param probeArrayPeer opaque-указатель массива проб
     */
    private static native void nAddProbeArray(long peer, long probeArrayPeer);

    /**
     * Добавляет одиночную пробу через {@code iplProbeBatchAddProbe}.
     *
     * @param peer opaque-указатель batch
     * @param centerX centerY centerZ центр пробы
     * @param radius радиус сферы влияния, м
     */
    private static native void nAddProbe(long peer, float centerX, float centerY, float centerZ, float radius);

    /**
     * Коммитит batch через {@code iplProbeBatchCommit}.
     *
     * @param peer opaque-указатель batch
     */
    private static native void nCommit(long peer);

    /**
     * Возвращает число проб через {@code iplProbeBatchGetNumProbes}.
     *
     * @param peer opaque-указатель batch
     * @return число проб
     */
    private static native int nGetNumProbes(long peer);

    /**
     * Сохраняет batch через {@code iplProbeBatchSave}.
     *
     * @param peer             opaque-указатель batch
     * @param destinationPeer  opaque-указатель сериализованного объекта
     */
    private static native void nSave(long peer, long destinationPeer);

    /**
     * Загружает batch через {@code iplProbeBatchLoad}.
     *
     * @param contextPeer opaque-указатель контекста
     * @param sourcePeer  opaque-указатель сериализованного объекта
     * @return opaque-указатель на загруженный batch
     */
    private static native long nLoad(long contextPeer, long sourcePeer);

    /**
     * Освобождает batch через {@code iplProbeBatchRelease}.
     *
     * @param peer opaque-указатель batch
     */
    private static native void nRelease(long peer);
}
