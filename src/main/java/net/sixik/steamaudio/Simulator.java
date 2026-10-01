package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLSimulator} — объектом, управляющим симуляциями
 * Steam Audio (direct/reflections/pathing) для всех добавленных источников.
 * <p>
 * Симулятор создается с флагами {@code FLAGS_*}; тип сцены и тип эффекта
 * отражений фиксированы на все время жизни объекта.
 */
public final class Simulator implements AutoCloseable {

    /** Включить direct-симуляцию ({@code IPL_SIMULATIONFLAGS_DIRECT}). */
    public static final int FLAGS_DIRECT = 1;

    /** Включить reflections-симуляцию ({@code IPL_SIMULATIONFLAGS_REFLECTIONS}). */
    public static final int FLAGS_REFLECTIONS = 2;

    /** Включить pathing-симуляцию ({@code IPL_SIMULATIONFLAGS_PATHING}). */
    public static final int FLAGS_PATHING = 4;

    /** Opaque-указатель на {@code IPLSimulator}; 0 означает закрытый симулятор. */
    private long peer;

    /**
     * Создает симулятор через {@code iplSimulatorCreate} с настройками,
     * рассчитанными на direct-симуляцию.
     * <p>
     * Прочие настройки фиксированы: сцена — встроенный трассировщик
     * ({@code SCENE_TYPE_DEFAULT}), тип отражений — convolution, до 256
     * occlusion-сэмплов, 4096 лучей, 2 сек. IR, Ambisonic order 1,
     * до 8 источников, 1 поток.
     *
     * @param context         контекст Steam Audio
     * @param simulationFlags типы симуляций ({@code FLAGS_*}); для
     *                        direct-only используйте {@link #FLAGS_DIRECT}
     * @param samplingRate    частота дискретизации, Гц
     * @param frameSize       размер фрейма в сэмплов
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public Simulator(Context context, int simulationFlags, int samplingRate, int frameSize) {
        this(context, simulationFlags, samplingRate, frameSize,
                ReflectionEffect.TYPE_CONVOLUTION);
    }

    /**
     * Создает симулятор через {@code iplSimulatorCreate} с явным типом
     * эффекта отражений.
     *
     * @param context         контекст Steam Audio
     * @param simulationFlags типы симуляций ({@code FLAGS_*})
     * @param samplingRate    частота дискретизации, Гц
     * @param frameSize       размер фрейма в сэмплов
     * @param reflectionType  тип эффекта отражений
     *                        ({@code ReflectionEffect.TYPE_*})
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public Simulator(Context context, int simulationFlags, int samplingRate, int frameSize, int reflectionType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), simulationFlags, Scene.SCENE_TYPE_DEFAULT,
                reflectionType, 256, 4096, 32,
                2.0f, 1, 8, 1, 8, 8, samplingRate, frameSize);
    }

    /**
     * Проверяет, что симулятор открыт.
     *
     * @return {@code true}, если симулятор создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Указывает сцену для симуляций ({@code iplSimulatorSetScene}).
     * После вызова необходим {@link #commit()}.
     *
     * @param scene открытая сцена
     */
    public void setScene(Scene scene) {
        requireOpen();
        if (scene == null || !scene.isOpen()) {
            throw new IllegalStateException("Scene is closed");
        }
        nSetScene(peer, scene.peerForChildren());
    }

    /**
     * Задает общие параметры симуляции, не привязанные к источникам
     * ({@code iplSimulatorSetSharedInputs}).
     * <p>
     * Позиция и ориентация слушателя задаются тремя векторами; базис
     * {@code right} вычисляется в нативе как {@code ahead × up}.
     * Параметры, не относящиеся к direct-симуляции (rays/bounces/duration/order),
     * используются reflections-симуляцией.
     *
     * @param flags                 типы симуляций ({@code FLAGS_*})
     * @param listenerPosition      мировые координаты слушателя
     * @param listenerAhead         единичный вектор «вперед» слушателя
     * @param listenerUp            единичный вектор «вверх» слушателя
     * @param numRays               число лучей (reflections)
     * @param numBounces            число отражений луча (reflections)
     * @param duration              длительность IR в секундах (reflections)
     * @param order                 Ambisonic order IR (reflections)
     * @param irradianceMinDistance минимальная дистанция для расчета
     *                              облученности поверхности (reflections)
     */
    public void setSharedInputs(int flags, Vector3 listenerPosition, Vector3 listenerAhead, Vector3 listenerUp,
                                int numRays, int numBounces, float duration, int order,
                                float irradianceMinDistance) {
        setSharedInputs(flags,
                listenerPosition.x, listenerPosition.y, listenerPosition.z,
                listenerAhead.x, listenerAhead.y, listenerAhead.z,
                listenerUp.x, listenerUp.y, listenerUp.z,
                numRays, numBounces, duration, order, irradianceMinDistance);
    }

    /**
     * Задает общие параметры симуляции без объектов и аллокаций — версия
     * с примитивами для хот-паса. Смотрите полную документацию в
     * Vector3-перегрузке.
     *
     * @param flags                 типы симуляций ({@code FLAGS_*})
     * @param listenerX listenerY listenerZ позиция слушателя
     * @param aheadX aheadY aheadZ  вектор «вперед» слушателя
     * @param upX upY upZ           вектор «вверх» слушателя
     * @param numRays               число лучей (reflections)
     * @param numBounces            число отражений луча (reflections)
     * @param duration              длительность IR в секундах (reflections)
     * @param order                 Ambisonic order IR (reflections)
     * @param irradianceMinDistance минимальная дистанция облученности (reflections)
     */
    public void setSharedInputs(int flags,
                                float listenerX, float listenerY, float listenerZ,
                                float aheadX, float aheadY, float aheadZ,
                                float upX, float upY, float upZ,
                                int numRays, int numBounces, float duration, int order,
                                float irradianceMinDistance) {
        requireOpen();
        nSetSharedInputs(peer, flags,
                listenerX, listenerY, listenerZ,
                aheadX, aheadY, aheadZ,
                upX, upY, upZ,
                numRays, numBounces, duration, order, irradianceMinDistance);
    }

    /**
     * Коммитит изменения сцены/списка источников
     * ({@code iplSimulatorCommit}).
     */
    public void commit() {
        requireOpen();
        nCommit(peer);
    }

    /**
     * Запускает direct-симуляцию для всех источников
     * ({@code iplSimulatorRunDirect}).
     * <p>
     * Не вызывайте этот метод из аудио-потока, если включены occlusion
     * и/или transmission.
     */
    public void runDirect() {
        requireOpen();
        nRunDirect(peer);
    }

    /**
     * Запускает reflections-симуляцию для всех источников
     * ({@code iplSimulatorRunReflections}).
     * <p>
     * CPU-интенсивная операция; вызывайте из отдельного потока, а не из
     * аудио-потока или потока обновления игры. Для симуляции требуются:
     * установленная сцена, общие входы (numRays/numBounces/duration/order)
     * и reflections-входы источников (см.
     * {@link Source#setReflectionsInputs}).
     */
    public void runReflections() {
        requireOpen();
        nRunReflections(peer);
    }

    /**
     * Добавляет probe batch для использования в симуляциях
     * ({@code iplSimulatorAddProbeBatch}); после этого необходим
     * {@link #commit()}. Нужен для reflections с baked-данными и для
     * pathing-симуляции.
     *
     * @param probeBatch открытый probe batch
     */
    public void addProbeBatch(ProbeBatch probeBatch) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nAddProbeBatch(peer, probeBatch.peerForChildren());
    }

    /**
     * Удаляет probe batch из симулятора ({@code iplSimulatorRemoveProbeBatch});
     * после этого необходим {@link #commit()}.
     *
     * @param probeBatch открытый probe batch
     */
    public void removeProbeBatch(ProbeBatch probeBatch) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nRemoveProbeBatch(peer, probeBatch.peerForChildren());
    }

    /**
     * Запускает pathing-симуляцию для всех источников
     * ({@code iplSimulatorRunPathing}).
     * <p>
     * CPU-интенсивная операция; вызывайте из отдельного потока. Требует:
     * сцену, probe batch (см. {@link #addProbeBatch}) и pathing-входы
     * источников (см. {@link Source#setPathingInputs}).
     */
    public void runPathing() {
        requireOpen();
        nRunPathing(peer);
    }

    /**
     * Освобождает симулятор ({@code iplSimulatorRelease}). Повторный
     * вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Возвращает opaque-указатель симулятора для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLSimulator}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Проверяет, что симулятор открыт.
     *
     * @throws IllegalStateException если симулятор закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("Simulator is closed");
        }
    }

    /**
     * Создает симулятор через {@code iplSimulatorCreate}.
     *
     * @param contextPeer            opaque-указатель контекста
     * @param flags                  типы симуляций
     * @param sceneType              тип сцены ({@code IPLSceneType})
     * @param reflectionType         тип эффекта отражений
     * @param maxNumOcclusionSamples макс. число occlusion-сэмплов
     * @param maxNumRays             макс. число лучей
     * @param numDiffuseSamples      число диффузных сэмплов
     * @param maxDuration            макс. длительность IR, сек
     * @param maxOrder               макс. Ambisonic order
     * @param maxNumSources          макс. число источников reflections
     * @param numThreads             число потоков симуляции
     * @param rayBatchSize           размер батча лучей (custom ray tracer)
     * @param numVisSamples          число visibility-сэмплов pathing
     * @param samplingRate           частота дискретизации, Гц
     * @param frameSize              размер фрейма, сэмплов
     * @return opaque-указатель на созданный симулятор
     */
    private static native long nCreate(long contextPeer, int flags, int sceneType, int reflectionType,
                                       int maxNumOcclusionSamples, int maxNumRays, int numDiffuseSamples,
                                       float maxDuration, int maxOrder, int maxNumSources, int numThreads,
                                       int rayBatchSize, int numVisSamples, int samplingRate, int frameSize);

    /**
     * Устанавливает сцену через {@code iplSimulatorSetScene}.
     *
     * @param peer      opaque-указатель симулятора
     * @param scenePeer opaque-указатель сцены
     */
    private static native void nSetScene(long peer, long scenePeer);

    /**
     * Устанавливает общие входы через {@code iplSimulatorSetSharedInputs}.
     *
     * @param peer                  opaque-указатель симулятора
     * @param flags                 типы симуляций
     * @param listenerX listenerY listenerZ позиция слушателя
     * @param aheadX aheadY aheadZ вектор «вперед» слушателя
     * @param upX upY upZ вектор «вверх» слушателя
     * @param numRays               число лучей
     * @param numBounces            число отражений
     * @param duration              длительность IR, сек
     * @param order                 Ambisonic order
     * @param irradianceMinDistance минимальная дистанция облученности
     */
    private static native void nSetSharedInputs(long peer, int flags,
                                                float listenerX, float listenerY, float listenerZ,
                                                float aheadX, float aheadY, float aheadZ,
                                                float upX, float upY, float upZ,
                                                int numRays, int numBounces, float duration,
                                                int order, float irradianceMinDistance);

    /**
     * Коммитит симулятор через {@code iplSimulatorCommit}.
     *
     * @param peer opaque-указатель симулятора
     */
    private static native void nCommit(long peer);

    /**
     * Запускает direct-симуляцию через {@code iplSimulatorRunDirect}.
     *
     * @param peer opaque-указатель симулятора
     */
    private static native void nRunDirect(long peer);

    /**
     * Запускает reflections-симуляцию через {@code iplSimulatorRunReflections}.
     *
     * @param peer opaque-указатель симулятора
     */
    private static native void nRunReflections(long peer);

    /**
     * Добавляет probe batch через {@code iplSimulatorAddProbeBatch}.
     *
     * @param peer           opaque-указатель симулятора
     * @param probeBatchPeer opaque-указатель probe batch
     */
    private static native void nAddProbeBatch(long peer, long probeBatchPeer);

    /**
     * Удаляет probe batch через {@code iplSimulatorRemoveProbeBatch}.
     *
     * @param peer           opaque-указатель симулятора
     * @param probeBatchPeer opaque-указатель probe batch
     */
    private static native void nRemoveProbeBatch(long peer, long probeBatchPeer);

    /**
     * Запускает pathing-симуляцию через {@code iplSimulatorRunPathing}.
     *
     * @param peer opaque-указатель симулятора
     */
    private static native void nRunPathing(long peer);

    /**
     * Освобождает симулятор через {@code iplSimulatorRelease}.
     *
     * @param peer opaque-указатель симулятора
     */
    private static native void nRelease(long peer);
}
