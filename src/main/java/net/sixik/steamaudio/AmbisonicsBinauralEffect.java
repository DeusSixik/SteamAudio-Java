package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLAmbisonicsBinauralEffect} — рендерит
 * амбисоническое аудио бинаурально через HRTF (иммерсивнее панорамирования,
 * немного дороже по CPU). Вход — амбисонический буфер, выход — стерео.
 */
public final class AmbisonicsBinauralEffect implements AutoCloseable {

    /** Opaque-указатель на {@code IPLAmbisonicsBinauralEffect}; 0 означает закрытый эффект. */
    private long peer;

    /** Opaque-указатель родительского контекста. */
    private final long contextPeer;

    /** HRTF, использованный при создании; предотвращает его GC. */
    private final HRTF hrtf;

    /**
     * Создает эффект через {@code iplAmbisonicsBinauralEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param hrtf         HRTF, используемый эффектом; должен оставаться
     *                     открытым все время жизни эффекта
     * @param maxOrder     максимальный порядок входного буфера
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public AmbisonicsBinauralEffect(Context context, int samplingRate, int frameSize, HRTF hrtf, int maxOrder) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.hrtf = hrtf;
        peer = nCreate(contextPeer, samplingRate, frameSize, hrtf.peerForChildren(), maxOrder);
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
     * Рендерит амбисонический буфер бинаурально
     * ({@code iplAmbisonicsBinauralEffectApply}).
     *
     * @param in    входной амбисонический буфер с {@code (order + 1)^2} каналами
     * @param out   выходной стерео-буфер
     * @param order порядок входного буфера
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsBinauralEffect or AudioBuffer is closed");
        }
        if (!hrtf.isOpen()) {
            throw new IllegalStateException("HRTF is closed");
        }
        return nApply(peer, contextPeer, hrtf.peerForChildren(), order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Освобождает эффект ({@code iplAmbisonicsBinauralEffectRelease}). Повторный
     * вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, long hrtfPeer, int maxOrder);

    private static native int nApply(long effectPeer, long contextPeer, long hrtfPeer, int order,
                                     long inPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
