package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLHRTF} — функцией, описывающей, как звук с разных
 * направлений воспринимается каждым из ушей слушателя.
 * <p>
 * Steam Audio включает встроенную HRTF ({@code HRTF_TYPE_DEFAULT}); также
 * можно загрузить собственную из SOFA-файла. Создание HRTF относительно
 * дорого и не потокобезопасно — не делайте это в аудио-потоке.
 */
public final class HRTF implements AutoCloseable {

    /** Встроенная HRTF Steam Audio ({@code IPL_HRTFTYPE_DEFAULT}). */
    public static final int TYPE_DEFAULT = 0;

    /** HRTF из SOFA-файла ({@code IPL_HRTFTYPE_SOFA}). */
    public static final int TYPE_SOFA = 1;

    /** Без нормализации ({@code IPL_HRTFNORMTYPE_NONE}). */
    public static final int NORM_TYPE_NONE = 0;

    /** RMS-нормализация: одинаковая громкость со всех направлений
     * ({@code IPL_HRTFNORMTYPE_RMS}). */
    public static final int NORM_TYPE_RMS = 1;

    /** Opaque-указатель на {@code IPLHRTF}; 0 означает закрытый объект. */
    private long peer;

    /**
     * Создает HRTF с настройками по умолчанию: встроенный HRTF, громкость
     * 1.0, без нормализации.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов (не зависит от числа каналов)
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public HRTF(Context context, int samplingRate, int frameSize) {
        this(context, samplingRate, frameSize, TYPE_DEFAULT, 1.0f, NORM_TYPE_NONE);
    }

    /**
     * Создает HRTF с полным набором настроек.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         тип HRTF: {@link #TYPE_DEFAULT} или {@link #TYPE_SOFA}
     * @param volume       коррекция громкости; 1.0 — без изменений
     * @param normType     тип нормализации: {@link #NORM_TYPE_NONE} или
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public HRTF(Context context, int samplingRate, int frameSize, int type, float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, volume, normType);
    }

    /**
     * Создает HRTF из SOFA-файла с полным набором настроек.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param sofaFileName путь к SOFA-файлу с HRTF-данными
     * @param volume       коррекция громкости; 1.0 — без изменений
     * @param normType     тип нормализации: {@link #NORM_TYPE_NONE} или
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public HRTF(Context context, int samplingRate, int frameSize, String sofaFileName,
                float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreateSofaFile(context.peerForChildren(), samplingRate, frameSize, volume, normType, sofaFileName);
    }

    /**
     * Создает HRTF из буфера с данными SOFA-файла (без обращения к файловой
     * системе).
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param sofaData     байты SOFA-файла
     * @param volume       коррекция громкости; 1.0 — без изменений
     * @param normType     тип нормализации: {@link #NORM_TYPE_NONE} или
     *                     {@link #NORM_TYPE_RMS}
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public HRTF(Context context, int samplingRate, int frameSize, byte[] sofaData,
                float volume, int normType) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreateSofaData(context.peerForChildren(), samplingRate, frameSize, volume, normType, sofaData);
    }

    /**
     * Проверяет, что HRTF открыт.
     *
     * @return {@code true}, если HRTF создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Возвращает opaque-указатель HRTF для дочерних объектов пакета
     * (например, {@code BinauralEffect}).
     *
     * @return opaque-указатель на {@code IPLHRTF}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Освобождает HRTF ({@code iplHRTFRelease}). Повторный вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Создает HRTF через {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         тип HRTF ({@code IPLHRTFType})
     * @param volume       коррекция громкости
     * @param normType     тип нормализации ({@code IPLHRTFNormType})
     * @return opaque-указатель на созданный HRTF
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, float volume, int normType);

    /**
     * Освобождает HRTF через {@code iplHRTFRelease}.
     *
     * @param peer opaque-указатель на HRTF
     */
    private static native void nRelease(long peer);

    /**
     * Создает HRTF из SOFA-файла через {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param volume       коррекция громкости
     * @param normType     тип нормализации ({@code IPLHRTFNormType})
     * @param sofaFileName путь к SOFA-файлу
     * @return opaque-указатель на созданный HRTF
     */
    private static native long nCreateSofaFile(long contextPeer, int samplingRate, int frameSize,
                                               float volume, int normType, String sofaFileName);

    /**
     * Создает HRTF из буфера SOFA-данных через {@code iplHRTFCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param volume       коррекция громкости
     * @param normType     тип нормализации ({@code IPLHRTFNormType})
     * @param sofaData     байты SOFA-файла
     * @return opaque-указатель на созданный HRTF
     */
    private static native long nCreateSofaData(long contextPeer, int samplingRate, int frameSize,
                                               float volume, int normType, byte[] sofaData);
}
