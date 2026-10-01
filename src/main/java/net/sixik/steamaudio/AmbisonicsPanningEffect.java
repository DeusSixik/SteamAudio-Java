package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLAmbisonicsPanningEffect} — рендерит
 * амбисоническое аудио панорамированием на стандартную раскладку
 * динамиков. Нельзя применять in-place; tail-сэмплов не генерирует.
 */
public final class AmbisonicsPanningEffect implements AutoCloseable {

    /** Opaque-указатель на {@code IPLAmbisonicsPanningEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает эффект через {@code iplAmbisonicsPanningEffectCreate}.
     *
     * @param context           контекст Steam Audio
     * @param samplingRate      частота дискретизации, Гц
     * @param frameSize         размер фрейма в сэмплов
     * @param speakerLayoutType тип раскладки динамиков
     *                          ({@code SpeakerLayout.MONO..SURROUND_7_1})
     * @param maxOrder          максимальный порядок входного буфера
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public AmbisonicsPanningEffect(Context context, int samplingRate, int frameSize,
                                   int speakerLayoutType, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, speakerLayoutType, maxOrder);
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
     * Панорамирует амбисонический буфер на динамики
     * ({@code iplAmbisonicsPanningEffectApply}).
     *
     * @param in    входной буфер с {@code (order + 1)^2} каналами
     * @param out   выходной буфер с числом каналов раскладки динамиков
     * @param order порядок входного буфера
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsPanningEffect or AudioBuffer is closed");
        }
        return nApply(peer, order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Освобождает эффект ({@code iplAmbisonicsPanningEffectRelease}). Повторный
     * вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize,
                                       int speakerLayoutType, int maxOrder);

    private static native int nApply(long effectPeer, int order, long inPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
