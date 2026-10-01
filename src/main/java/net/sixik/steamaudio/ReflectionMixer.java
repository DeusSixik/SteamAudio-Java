package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLReflectionMixer} — микшером выходов нескольких
 * reflection-эффектов, генерирующим единое звуковое поле отражений.
 * <p>
 * Использование опционально; для {@code TYPE_CONVOLUTION} микшер снижает
 * нагрузку на CPU, для {@code TYPE_TAN} — обязателен, а для
 * {@code TYPE_PARAMETRIC} и {@code TYPE_HYBRID} — не используется.
 */
public final class ReflectionMixer implements AutoCloseable {

    /** Opaque-указатель на {@code IPLReflectionMixer}; 0 означает закрытый микшер. */
    private long peer;

    /**
     * Создает reflection-микшер через {@code iplReflectionMixerCreate}.
     * Настройки должны совпадать с настройками {@link ReflectionEffect}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         алгоритм реверба
     *                     ({@code ReflectionEffect.TYPE_*})
     * @param irSize       число сэмплов на канал IR
     * @param numChannels  число каналов IR/выходных буферов
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public ReflectionMixer(Context context, int samplingRate, int frameSize,
                           int type, int irSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, type, irSize, numChannels);
    }

    /**
     * Проверяет, что микшер открыт.
     *
     * @return {@code true}, если микшер создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Извлекает содержимое микшера в буфер
     * ({@code iplReflectionMixerApply}), беря параметры из результатов
     * reflections-симуляции указанного источника.
     *
     * @param out    выходной буфер ({@code numChannels} каналов)
     * @param source источник с активной reflections-симуляцией
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException если микшер, источник или буфер закрыты
     */
    public int apply(AudioBuffer out, Source source) {
        if (!isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("ReflectionMixer, Source or AudioBuffer is closed");
        }
        return nApply(peer, out.peerForEffect(), source.peerForChildren());
    }

    /**
     * Сбрасывает внутреннее состояние обработки
     * ({@code iplReflectionMixerReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Проверяет, что микшер открыт.
     *
     * @throws IllegalStateException если микшер закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("ReflectionMixer is closed");
        }
    }

    /**
     * Освобождает микшер ({@code iplReflectionMixerRelease}). Повторный
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
     * Возвращает opaque-указатель микшера для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLReflectionMixer}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Создает микшер через {@code iplReflectionMixerCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param type         алгоритм реверба ({@code IPLReflectionEffectType})
     * @param irSize       число сэмплов на канал IR
     * @param numChannels  число каналов IR
     * @return opaque-указатель на созданный микшер
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int type, int irSize, int numChannels);

    /**
     * Извлекает содержимое микшера через {@code iplReflectionMixerApply}:
     * натив вызывает {@code iplSourceGetOutputs} для
     * {@code IPL_SIMULATIONFLAGS_REFLECTIONS} и прокидывает
     * {@code IPLReflectionEffectParams} в apply без копирования.
     *
     * @param peer        opaque-указатель микшера
     * @param outPeer     указатель на выходную структуру буфера
     * @param sourcePeer  opaque-указатель источника
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nApply(long peer, long outPeer, long sourcePeer);

    /**
     * Сбрасывает состояние микшера через {@code iplReflectionMixerReset}.
     *
     * @param peer opaque-указатель микшера
     */
    private static native void nReset(long peer);

    /**
     * Освобождает микшер через {@code iplReflectionMixerRelease}.
     *
     * @param peer opaque-указатель микшера
     */
    private static native void nRelease(long peer);
}
