package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLReflectionEffect} — эффектом рендеринга
 * результатов reflections-симуляции (реверб).
 * <p>
 * Вход — 1-канальный (или 2-канальный) источник аудио; выход — буфер
 * с числом каналов, равным {@code numChannels} эффекта (для
 * Ambisonic-конфигурации — {@code (order + 1)<sup>2</sup>}).
 * Параметры эффекта (IR или RT60) берутся напрямую из
 * {@link Source#getReflectionsOutputsInto} — нативный слой прокидывает
 * {@code IPLReflectionEffectParams} из результатов симуляции без
 * копирования.
 * <p>
 * Для {@code TYPE_CONVOLUTION} и {@code TYPE_TAN} рекомендуется (или
 * обязателен для TAN) {@link ReflectionMixer}. Для
 * {@code TYPE_PARAMETRIC} и {@code TYPE_HYBRID} mixer не используется.
 */
public final class ReflectionEffect implements AutoCloseable {

    /** Многоканальная convolution-реверберация ({@code IPL_REFLECTIONEFFECTTYPE_CONVOLUTION}). */
    public static final int TYPE_CONVOLUTION = 0;

    /** Параметрический реверб на feedback delay networks
     * ({@code IPL_REFLECTIONEFFECTTYPE_PARAMETRIC}). Дешевле по CPU,
     * не рендерит отдельные эхо. */
    public static final int TYPE_PARAMETRIC = 1;

    /** Гибрид convolution + параметрического реверба
     * ({@code IPL_REFLECTIONEFFECTTYPE_HYBRID}). */
    public static final int TYPE_HYBRID = 2;

    /** Convolution на GPU через AMD TrueAudio Next
     * ({@code IPL_REFLECTIONEFFECTTYPE_TAN}); требует {@link ReflectionMixer}. */
    public static final int TYPE_TAN = 3;

    /** Opaque-указатель на {@code IPLReflectionEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает reflection-эффект через {@code iplReflectionEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         алгоритм реверба: {@code TYPE_*}
     * @param irSize       число сэмплов на канал IR (для parametric
     *                     можно задать размер фрейма)
     * @param numChannels  число каналов IR/выходных буферов
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public ReflectionEffect(Context context, int samplingRate, int frameSize,
                            int type, int irSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, irSize, numChannels);
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
     * Применяет эффект к буферу ({@code iplReflectionEffectApply}),
     * беря параметры (IR/RT60) из результатов reflections-симуляции
     * указанного источника.
     *
     * @param in     входной буфер (1 или 2 канала)
     * @param out    выходной буфер ({@code numChannels} каналов эффекта)
     * @param source источник с активной reflections-симуляцией
     * @param mixer  опциональный микшер для {@code TYPE_CONVOLUTION}/
     *               {@code TYPE_TAN}; {@code null} — рендерить напрямую
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException если эффект, источник или буферы закрыты
     */
    public int apply(AudioBuffer in, AudioBuffer out, Source source, ReflectionMixer mixer) {
        if (!isOpen() || !in.isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("ReflectionEffect, Source or AudioBuffer is closed");
        }
        long mixerPeer = (mixer == null) ? 0 : mixer.peerForChildren();
        return nApply(peer, in.peerForEffect(), out.peerForEffect(), source.peerForChildren(), mixerPeer);
    }

    /**
     * Возвращает число оставшихся tail-сэмплов
     * ({@code iplReflectionEffectGetTailSize}).
     *
     * @return число tail-сэмплов
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Извлекает один фрейм tail-сэмплов
     * ({@code iplReflectionEffectGetTail}).
     *
     * @param out   выходной буфер ({@code numChannels} каналов)
     * @param mixer опциональный микшер; {@code null} — без микшера
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out, ReflectionMixer mixer) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        long mixerPeer = (mixer == null) ? 0 : mixer.peerForChildren();
        return nGetTail(peer, out.peerForEffect(), mixerPeer);
    }

    /**
     * Сбрасывает внутреннее состояние обработки
     * ({@code iplReflectionEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Проверяет, что эффект открыт.
     *
     * @throws IllegalStateException если эффект закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ReflectionEffect is closed");
        }
    }

    /**
     * Освобождает эффект ({@code iplReflectionEffectRelease}). Повторный
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
     * Создает эффект через {@code iplReflectionEffectCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         алгоритм реверба ({@code IPLReflectionEffectType})
     * @param irSize       число сэмплов на канал IR
     * @param numChannels  число каналов IR
     * @return opaque-указатель на созданный эффект
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, int irSize, int numChannels);

    /**
     * Применяет эффект через {@code iplReflectionEffectApply}: натив
     * вызывает {@code iplSourceGetOutputs} для {@code IPL_SIMULATIONFLAGS_REFLECTIONS}
     * и прокидывает {@code IPLReflectionEffectParams} в apply без копирования.
     *
     * @param effectPeer  opaque-указатель эффекта
     * @param inPeer      указатель на входную структуру буфера
     * @param outPeer     указатель на выходную структуру буфера
     * @param sourcePeer  opaque-указатель источника
     * @param mixerPeer   opaque-указатель микшера или 0
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long inPeer, long outPeer,
                                     long sourcePeer, long mixerPeer);

    /**
     * Сбрасывает состояние эффекта через {@code iplReflectionEffectReset}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nReset(long effectPeer);

    /**
     * Возвращает число tail-сэмплов через {@code iplReflectionEffectGetTailSize}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @return число tail-сэмплов
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Извлекает фрейм tail-сэмплов через {@code iplReflectionEffectGetTail}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @param outPeer    указатель на выходную структуру буфера
     * @param mixerPeer  opaque-указатель микшера или 0
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer, long mixerPeer);

    /**
     * Освобождает эффект через {@code iplReflectionEffectRelease}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nRelease(long effectPeer);
}
