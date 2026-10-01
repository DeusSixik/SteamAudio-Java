package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLBinauralEffect} — эффектом бинауральной
 * пространственности точки-источника через HRTF.
 * <p>
 * Входной буфер должен быть 1- или 2-канальным; выходной — 2-канальным
 * (стерео). Эффект нельзя применять in-place: вход и выход — разные
 * буферы. Направление должно быть единичным вектором в системе координат
 * слушателя (см. {@link Context#calculateRelativeDirection}).
 */
public final class BinauralEffect implements AutoCloseable {

    /** Интерполяция HRTF: ближайший сосед ({@code IPL_HRTFINTERPOLATION_NEAREST}).
     * Самый дешевый вариант; используйте по умолчанию. */
    public static final int INTERPOLATION_NEAREST = 0;

    /** Интерполяция HRTF: билинейная ({@code IPL_HRTFINTERPOLATION_BILINEAR}).
     * Дороже по CPU; полезна для широкополосных шумоподобных звуков. */
    public static final int INTERPOLATION_BILINEAR = 1;

    /** Остались tail-сэмплы во внутренних буферах эффекта
     * ({@code IPL_AUDIOEFFECTSTATE_TAILREMAINING}). */
    public static final int STATE_TAIL_REMAINING = 0;

    /** Tail-сэмплов больше нет ({@code IPL_AUDIOEFFECTSTATE_TAILCOMPLETE}). */
    public static final int STATE_TAIL_COMPLETE = 1;

    /** Opaque-указатель на {@code IPLBinauralEffect}; 0 означает закрытый эффект. */
    private long peer;

    /** Peer-указатель контекста, использованного при создании эффекта. */
    private final long contextPeer;

    /** HRTF, использованный при создании эффекта; предотвращает его GC. */
    private final HRTF hrtf;

    /**
     * Создает бинауральный эффект через {@code iplBinauralEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param hrtf         HRTF, используемый эффектом; должен оставаться
     *                     открытым все время жизни эффекта
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public BinauralEffect(Context context, int samplingRate, int frameSize, HRTF hrtf) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.hrtf = hrtf;
        this.peer = nCreate(contextPeer, samplingRate, frameSize, hrtf.peerForChildren());
    }

    /**
     * Проверяет, что эффект открыт.
     *
     * @return {@code true}, если эффект создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Применяет эффект к буферу ({@code iplBinauralEffectApply}).
     * <p>
     * Вход и выход — разные буферы (не in-place). Вход — 1 или 2 канала,
     * выход — 2 канала, одинаковое число сэмплов.
     *
     * @param in            входной буфер
     * @param out           выходной стерео-буфер
     * @param direction     единичный вектор от слушателя к источнику
     * @param interpolation интерполяция HRTF: {@link #INTERPOLATION_NEAREST}
     *                      или {@link #INTERPOLATION_BILINEAR}
     * @param spatialBlend  0.0 — без пространственности, 1.0 — полная
     * @return состояние эффекта: {@link #STATE_TAIL_REMAINING} или
     *         {@link #STATE_TAIL_COMPLETE}
     * @throws IllegalStateException если эффект, контекст, входной или
     *                              выходной буфер закрыты
     */
    public int apply(AudioBuffer in, AudioBuffer out, Vector3 direction,
                     int interpolation, float spatialBlend) {
        return apply(in, out, direction.x, direction.y, direction.z, interpolation, spatialBlend);
    }

    /**
     * Применяет эффект к буферу без объектов и аллокаций — версия
     * с примитивами для хот-паса. Смотрите полную документацию в
     * Vector3-перегрузке.
     *
     * @param in            входной буфер
     * @param out           выходной стерео-буфер
     * @param dirX dirY dirZ единичный вектор от слушателя к источнику
     * @param interpolation интерполяция HRTF: {@link #INTERPOLATION_NEAREST}
     *                      или {@link #INTERPOLATION_BILINEAR}
     * @param spatialBlend  0.0 — без пространственности, 1.0 — полная
     * @return состояние эффекта: {@link #STATE_TAIL_REMAINING} или
     *         {@link #STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out,
                     float dirX, float dirY, float dirZ,
                     int interpolation, float spatialBlend) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("BinauralEffect, Context, input or output AudioBuffer is closed");
        }
        if (!hrtf.isOpen()) {
            throw new IllegalStateException("HRTF is closed");
        }
        return nApply(peer, contextPeer, hrtf.peerForChildren(),
                dirX, dirY, dirZ, interpolation, spatialBlend,
                inPeerOf(in), outPeerOf(out));
    }

    /**
     * Возвращает число оставшихся tail-сэмплов
     * ({@code iplBinauralEffectGetTailSize}).
     *
     * @return число tail-сэмплов
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Извлекает один фрейм tail-сэмплов во внешний буфер
     * ({@code iplBinauralEffectGetTail}); вызывать вместо
     * {@link #apply} после остановки входа, пока возвращается
     * {@link #STATE_TAIL_REMAINING}.
     *
     * @param out выходной стерео-буфер
     * @return состояние эффекта: {@link #STATE_TAIL_REMAINING} или
     *         {@link #STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, outPeerOf(out));
    }

    /**
     * Сбрасывает внутреннее состояние обработки
     * ({@code iplBinauralEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Освобождает эффект ({@code iplBinauralEffectRelease}). Повторный
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
     * Возвращает peer-указатель буфера, не требуя публичного доступа
     * к полю {@link AudioBuffer}.
     *
     * @param buffer открытый буфер
     * @return указатель на нативную структуру буфера
     */
    private static long inPeerOf(AudioBuffer buffer) {
        return buffer.peerForEffect();
    }

    /**
     * Возвращает peer-указатель буфера, не требуя публичного доступа
     * к полю {@link AudioBuffer}.
     *
     * @param buffer открытый буфер
     * @return указатель на нативную структуру буфера
     */
    private static long outPeerOf(AudioBuffer buffer) {
        return buffer.peerForEffect();
    }

    /**
     * Проверяет, что эффект открыт.
     *
     * @throws IllegalStateException если эффект закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("BinauralEffect is closed");
        }
    }

    /**
     * Создает эффект через {@code iplBinauralEffectCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param hrtfPeer     opaque-указатель на HRTF
     * @return opaque-указатель на созданный эффект
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, long hrtfPeer);

    /**
     * Применяет эффект через {@code iplBinauralEffectApply}.
     *
     * @param effectPeer    opaque-указатель эффекта
     * @param contextPeer   opaque-указатель контекста
     * @param hrtfPeer      opaque-указатель HRTF
     * @param dirX dirY dirZ единичный вектор от слушателя к источнику
     * @param interpolation интерполяция HRTF ({@code IPLHRTFInterpolation})
     * @param spatialBlend  степень пространственности, 0.0..1.0
     * @param inPeer        указатель на входную нативную структуру буфера
     * @param outPeer       указатель на выходную нативную структуру буфера
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long contextPeer, long hrtfPeer,
                                     float dirX, float dirY, float dirZ,
                                     int interpolation, float spatialBlend,
                                     long inPeer, long outPeer);

    /**
     * Сбрасывает состояние эффекта через {@code iplBinauralEffectReset}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nReset(long effectPeer);

    /**
     * Возвращает число tail-сэмплов через {@code iplBinauralEffectGetTailSize}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @return число tail-сэмплов
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Извлекает фрейм tail-сэмплов через {@code iplBinauralEffectGetTail}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @param outPeer    указатель на выходную нативную структуру буфера
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Освобождает эффект через {@code iplBinauralEffectRelease}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nRelease(long effectPeer);
}
