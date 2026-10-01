package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLPathEffect} — эффектом рендеринга результатов
 * pathing-симуляции: звук, приходящий к слушателю по обходным путям
 * (дифракция через дверные проемы, коридоры и т.п.).
 * <p>
 * Выход — Ambisonic-буфер с {@code (maxOrder + 1)^2} каналами; версия
 * {@code spatialize = false} рендерит неротированное амбисоническое
 * аудио, которое нужно повернуть под ориентацию слушателя перед
 * воспроизведением (эффекты вращения/декодирования будут добавлены
 * позже). Эффект нельзя применять in-place.
 */
public final class PathEffect implements AutoCloseable {

    /** Opaque-указатель на {@code IPLPathEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает path-эффект через {@code iplPathEffectCreate} в режиме
     * рендеринга амбисонического аудио без пространственности
     * ({@code spatialize = false}).
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param maxOrder     максимальный Ambisonics order выходного буфера
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public PathEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, maxOrder);
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
     * Применяет эффект к буферу ({@code iplPathEffectApply}), беря
     * параметры (EQ коэффициенты, Ambisonic SH-коэффициенты) напрямую из
     * результатов pathing-симуляции указанного источника.
     *
     * @param in    входной моно-буфер
     * @param out   выходной буфер с {@code (maxOrder + 1)^2} каналами
     * @param source источник с активной pathing-симуляцией
     * @param order Ambisonic order обработки (не более {@code maxOrder};
     *              меньшие значения снижают CPU)
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException если эффект, источник или буферы закрыты
     */
    public int apply(AudioBuffer in, AudioBuffer out, Source source, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("PathEffect, Source or AudioBuffer is closed");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must be non-negative");
        }
        return nApply(peer, in.peerForEffect(), out.peerForEffect(), source.peerForChildren(), order);
    }

    /**
     * Возвращает число оставшихся tail-сэмплов
     * ({@code iplPathEffectGetTailSize}).
     *
     * @return число tail-сэмплов
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Извлекает один фрейм tail-сэмплов
     * ({@code iplPathEffectGetTail}).
     *
     * @param out выходной буфер
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int getTail(AudioBuffer out) {
        requireOpen();
        if (!out.isOpen()) {
            throw new IllegalStateException("Output AudioBuffer is closed");
        }
        return nGetTail(peer, out.peerForEffect());
    }

    /**
     * Сбрасывает внутреннее состояние обработки
     * ({@code iplPathEffectReset}).
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
            throw new IllegalStateException("PathEffect is closed");
        }
    }

    /**
     * Освобождает эффект ({@code iplPathEffectRelease}). Повторный вызов
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
     * Создает эффект через {@code iplPathEffectCreate}
     * ({@code spatialize = false}).
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param maxOrder     максимальный Ambisonics order
     * @return opaque-указатель на созданный эффект
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int maxOrder);

    /**
     * Применяет эффект через {@code iplPathEffectApply}: натив вызывает
     * {@code iplSourceGetOutputs} для {@code IPL_SIMULATIONFLAGS_PATHING}
     * и прокидывает {@code shCoeffs}/EQ из результатов симуляции без
     * копирования.
     *
     * @param effectPeer opaque-указатель эффекта
     * @param inPeer     указатель на входную структуру буфера
     * @param outPeer    указатель на выходную структуру буфера
     * @param sourcePeer opaque-указатель источника
     * @param order      Ambisonic order обработки
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, long inPeer, long outPeer,
                                     long sourcePeer, int order);

    /**
     * Сбрасывает состояние эффекта через {@code iplPathEffectReset}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nReset(long effectPeer);

    /**
     * Возвращает число tail-сэмплов через {@code iplPathEffectGetTailSize}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @return число tail-сэмплов
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Извлекает фрейм tail-сэмплов через {@code iplPathEffectGetTail}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @param outPeer    указатель на выходную структуру буфера
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Освобождает эффект через {@code iplPathEffectRelease}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nRelease(long effectPeer);
}
