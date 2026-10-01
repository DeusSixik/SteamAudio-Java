package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLSource} — источником симуляции, привязанным к
 * конкретному {@link Simulator}. Источник хранит позицию/ориентацию и
 * параметры direct-симуляции, а также выдает результаты симуляции
 * для последующего рендеринга через {@link DirectEffect}.
 */
public final class Source implements AutoCloseable {

    /** Флаг direct-симуляции источника: distance attenuation
     * ({@code IPL_DIRECTSIMULATIONFLAGS_DISTANCEATTENUATION}). */
    public static final int DIRECT_SIM_DISTANCE_ATTENUATION = 1;

    /** Флаг direct-симуляции: air absorption
     * ({@code IPL_DIRECTSIMULATIONFLAGS_AIRABSORPTION}). */
    public static final int DIRECT_SIM_AIR_ABSORPTION = 2;

    /** Флаг direct-симуляции: directivity
     * ({@code IPL_DIRECTSIMULATIONFLAGS_DIRECTIVITY}). */
    public static final int DIRECT_SIM_DIRECTIVITY = 4;

    /** Флаг direct-симуляции: occlusion
     * ({@code IPL_DIRECTSIMULATIONFLAGS_OCCLUSION}). */
    public static final int DIRECT_SIM_OCCLUSION = 8;

    /** Флаг direct-симуляции: transmission; требует включенного occlusion
     * ({@code IPL_DIRECTSIMULATIONFLAGS_TRANSMISSION}). */
    public static final int DIRECT_SIM_TRANSMISSION = 16;

    /** Алгоритм окклюзии: одиночный луч от слушателя к источнику
     * ({@code IPL_OCCLUSIONTYPE_RAYCAST}). */
    public static final int OCCLUSION_RAYCAST = 0;

    /** Алгоритм окклюзии: волюметрический, с частичной окклюзией
     * ({@code IPL_OCCLUSIONTYPE_VOLUMETRIC}). */
    public static final int OCCLUSION_VOLUMETRIC = 1;

    /** Opaque-указатель на {@code IPLSource}; 0 означает закрытый источник. */
    private long peer;

    /** Opaque-указатель родительского симулятора. */
    private final long simulatorPeer;

    /**
     * Создает источник через {@code iplSourceCreate}. После создания
     * необходимо вызвать {@link #add()} и {@link Simulator#commit()}.
     *
     * @param simulator       открытый симулятор
     * @param simulationFlags типы симуляций источника ({@code Simulator.FLAGS_*})
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если симулятор закрыт
     */
    public Source(Simulator simulator, int simulationFlags) {
        if (simulator == null || !simulator.isOpen()) {
            throw new IllegalStateException("Simulator is closed");
        }
        this.simulatorPeer = simulator.peerForChildren();
        this.peer = nCreate(simulatorPeer, simulationFlags);
    }

    /**
     * Проверяет, что источник открыт.
     *
     * @return {@code true}, если источник создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Добавляет источник в симулятор ({@code iplSourceAdd}); после этого
     * необходим {@link Simulator#commit()}.
     */
    public void add() {
        requireOpen();
        nAdd(peer, simulatorPeer);
    }

    /**
     * Удаляет источник из симулятора ({@code iplSourceRemove}); после
     * этого необходим {@link Simulator#commit()}.
     */
    public void remove() {
        requireOpen();
        nRemove(peer, simulatorPeer);
    }

    /**
     * Задает параметры direct-симуляции источника
     * ({@code iplSourceSetInputs} с {@code IPL_SIMULATIONFLAGS_DIRECT}).
     * <p>
     * Используются настройки по умолчанию: distance attenuation — модель
     * по умолчанию (обратная дистанция с плато 1 м), air absorption —
     * модель по умолчанию, directivity — всенаправленная
     * ({@code dipoleWeight = 0}). Базис {@code right} вычисляется в
     * нативе как {@code ahead × up}.
     *
     * @param directFlags         комбинация флагов {@code DIRECT_SIM_*}
     * @param sourcePosition      мировые координаты источника
     * @param sourceAhead         единичный вектор «вперед» источника
     * @param sourceUp            единичный вектор «вверх» источника
     * @param occlusionType       алгоритм окклюзии: {@link #OCCLUSION_RAYCAST}
     *                            или {@link #OCCLUSION_VOLUMETRIC}
     * @param occlusionRadius     радиус сферы источника при волюметрической
     *                            окклюзии, м
     * @param numOcclusionSamples число point-сэмплов волюметрической окклюзии
     * @param numTransmissionRays макс. число поверхностей для transmission
     */
    public void setDirectInputs(int directFlags, Vector3 sourcePosition, Vector3 sourceAhead, Vector3 sourceUp,
                                int occlusionType, float occlusionRadius, int numOcclusionSamples,
                                int numTransmissionRays) {
        setDirectInputs(directFlags,
                sourcePosition.x, sourcePosition.y, sourcePosition.z,
                sourceAhead.x, sourceAhead.y, sourceAhead.z,
                sourceUp.x, sourceUp.y, sourceUp.z,
                occlusionType, occlusionRadius, numOcclusionSamples, numTransmissionRays);
    }

    /**
     * Задает параметры direct-симуляции без объектов и аллокаций — версия
     * с примитивами для хот-паса. Смотрите полную документацию в
     * Vector3-перегрузке.
     *
     * @param directFlags         комбинация флагов {@code DIRECT_SIM_*}
     * @param sourceX sourceY sourceZ позиция источника
     * @param aheadX aheadY aheadZ вектор «вперед» источника
     * @param upX upY upZ         вектор «вверх» источника
     * @param occlusionType       алгоритм окклюзии ({@code IPLOcclusionType})
     * @param occlusionRadius     радиус сферы источника, м
     * @param numOcclusionSamples число point-сэмплов волюметрической окклюзии
     * @param numTransmissionRays макс. число поверхностей для transmission
     */
    public void setDirectInputs(int directFlags,
                                float sourceX, float sourceY, float sourceZ,
                                float aheadX, float aheadY, float aheadZ,
                                float upX, float upY, float upZ,
                                int occlusionType, float occlusionRadius, int numOcclusionSamples,
                                int numTransmissionRays) {
        requireOpen();
        nSetDirectInputs(peer, directFlags,
                sourceX, sourceY, sourceZ,
                aheadX, aheadY, aheadZ,
                upX, upY, upZ,
                occlusionType, occlusionRadius, numOcclusionSamples, numTransmissionRays);
    }

    /**
     * Задает параметры reflections-симуляции источника
     * ({@code iplSourceSetInputs} с {@code IPL_SIMULATIONFLAGS_REFLECTIONS}).
     * <p>
     * Реалтайм-симуляция (без baked-данных): масштабы затухания реверба
     * ({@code reverbScale}) применяются к RT60 в каждой полосе; 1.0 —
     * использовать симулированные значения без изменений. Гибридные
     * настройки по умолчанию: transition time 1.0 с, overlap 0.25.
     *
     * @param reverbScale массив из {@value Material#NUM_BANDS} масштабов RT60
     */
    public void setReflectionsInputs(float[] reverbScale) {
        requireOpen();
        if (reverbScale.length < Material.NUM_BANDS) {
            throw new IllegalArgumentException("reverbScale must contain " + Material.NUM_BANDS + " values");
        }
        nSetReflectionsInputs(peer, reverbScale);
    }

    /**
     * Получает результаты direct-симуляции
     * ({@code iplSourceGetOutputs} с {@code IPL_SIMULATIONFLAGS_DIRECT})
     * и записывает их в массив без аллокаций. Формат массива:
     * {@code [distanceAttenuation, airAbs0, airAbs1, airAbs2, directivity,
     * occlusion, trans0, trans1, trans2]} — 9 float.
     *
     * @param out массив длиной не менее 9
     * @throws IllegalStateException если источник закрыт
     */
    public void getDirectOutputsInto(float[] out) {
        requireOpen();
        if (out.length < 9) {
            throw new IllegalArgumentException("out must contain at least 9 elements");
        }
        nGetDirectOutputs(peer, out);
    }

    /**
     * Проверяет, что источник открыт.
     *
     * @throws IllegalStateException если источник закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("Source is closed");
        }
    }

    /**
     * Возвращает opaque-указатель источника для дочерних объектов пакета
     * (например, {@code ReflectionEffect}).
     *
     * @return opaque-указатель на {@code IPLSource}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Освобождает источник ({@code iplSourceRelease}). Перед этим вызовите
     * {@link #remove()}, если источник был добавлен в симулятор. Повторный
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
     * Создает источник через {@code iplSourceCreate}.
     *
     * @param simulatorPeer   opaque-указатель симулятора
     * @param simulationFlags типы симуляций ({@code IPLSimulationFlags})
     * @return opaque-указатель на созданный источник
     */
    private static native long nCreate(long simulatorPeer, int simulationFlags);

    /**
     * Добавляет источник в симулятор через {@code iplSourceAdd}.
     *
     * @param peer          opaque-указатель источника
     * @param simulatorPeer opaque-указатель симулятора
     */
    private static native void nAdd(long peer, long simulatorPeer);

    /**
     * Удаляет источник из симулятора через {@code iplSourceRemove}.
     *
     * @param peer          opaque-указатель источника
     * @param simulatorPeer opaque-указатель симулятора
     */
    private static native void nRemove(long peer, long simulatorPeer);

    /**
     * Устанавливает direct-входы через {@code iplSourceSetInputs}.
     *
     * @param peer                opaque-указатель источника
     * @param directFlags         флаги direct-симуляции
     * @param sourceX sourceY sourceZ позиция источника
     * @param aheadX aheadY aheadZ вектор «вперед» источника
     * @param upX upY upZ вектор «вверх» источника
     * @param occlusionType       алгоритм окклюзии ({@code IPLOcclusionType})
     * @param occlusionRadius     радиус сферы источника
     * @param numOcclusionSamples число occlusion-сэмплов
     * @param numTransmissionRays число transmission-лучей
     */
    private static native void nSetDirectInputs(long peer, int directFlags,
                                                float sourceX, float sourceY, float sourceZ,
                                                float aheadX, float aheadY, float aheadZ,
                                                float upX, float upY, float upZ,
                                                int occlusionType, float occlusionRadius,
                                                int numOcclusionSamples, int numTransmissionRays);

    /**
     * Устанавливает reflections-входы через {@code iplSourceSetInputs}.
     *
     * @param peer        opaque-указатель источника
     * @param reverbScale массив из 3 масштабов RT60
     */
    private static native void nSetReflectionsInputs(long peer, float[] reverbScale);

    /**
     * Устанавливает pathing-входы через {@code iplSourceSetInputs}.
     *
     * @param peer          opaque-указатель источника
     * @param probeBatchPeer opaque-указатель probe batch
     * @param pathingOrder  Ambisonic order путей
     * @param visRadius     радиус сферы видимости, м
     * @param visThreshold  порог видимости (0..1)
     * @param visRange      максимальная дистанция видимости, м
     */
    private static native void nSetPathingInputs(long peer, long probeBatchPeer, int pathingOrder,
                                                 float visRadius, float visThreshold, float visRange);

    /**
     * Задает параметры pathing-симуляции источника
     * ({@code iplSourceSetInputs} с {@code IPL_SIMULATIONFLAGS_PATHING}).
     * <p>
     * Используются запеченные в probe batch данные; при включенной валидации
     * запеченные пути проверяются на видимость, а заблокированные пути
     * перестраиваются в реальном времени.
     *
     * @param probeBatch   открытый probe batch с запеченными pathing-данными
     * @param pathingOrder Ambisonic order представления направленности путей
     * @param visRadius    радиус сферы видимости пробы, м
     * @param visThreshold порог доли незаблокированных лучей видимости (0..1)
     * @param visRange     максимальная дистанция между взаимно видимыми
     *                     пробами, м
     */
    public void setPathingInputs(ProbeBatch probeBatch, int pathingOrder,
                                 float visRadius, float visThreshold, float visRange) {
        requireOpen();
        if (probeBatch == null || !probeBatch.isOpen()) {
            throw new IllegalStateException("ProbeBatch is closed");
        }
        nSetPathingInputs(peer, probeBatch.peerForChildren(), pathingOrder, visRadius, visThreshold, visRange);
    }

    /**
     * Получает direct-результаты через {@code iplSourceGetOutputs} и
     * копирует их в массив.
     *
     * @param peer opaque-указатель источника
     * @param out  массив назначения (не менее 9 элементов)
     */
    private static native void nGetDirectOutputs(long peer, float[] out);

    /**
     * Освобождает источник через {@code iplSourceRelease}.
     *
     * @param peer opaque-указатель источника
     */
    private static native void nRelease(long peer);
}
