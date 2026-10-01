package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLAmbisonicsEncodeEffect} — кодирует моно- (или
 * стерео-) точечный источник в амбисонический буфер заданного порядка.
 * Steam Audio кодирует в N3D (ACN, ортонормированные гармоники).
 */
public final class AmbisonicsEncodeEffect implements AutoCloseable {

    /** Opaque-указатель на {@code IPLAmbisonicsEncodeEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает эффект через {@code iplAmbisonicsEncodeEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param maxOrder     максимальный порядок кодирования
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public AmbisonicsEncodeEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
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
     * Кодирует буфер в амбисонический ({@code iplAmbisonicsEncodeEffectApply}).
     *
     * @param in            входной буфер (1 или 2 канала)
     * @param out           выходной буфер с {@code (order + 1)^2} каналами
     * @param direction     вектор от слушателя к источнику (не обязательно
     *                      единичный; нулевой вектор = омни)
     * @param order         порядок кодирования (не более {@code maxOrder})
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out, Vector3 direction, int order) {
        return apply(in, out, direction.x, direction.y, direction.z, order);
    }

    /**
     * Кодирует буфер в амбисонический — версия с примитивами для хот-паса.
     *
     * @param in            входной буфер (1 или 2 канала)
     * @param out           выходной буфер с {@code (order + 1)^2} каналами
     * @param dirX dirY dirZ вектор от слушателя к источнику
     * @param order         порядок кодирования (не более {@code maxOrder})
     * @return состояние эффекта
     */
    public int apply(AudioBuffer in, AudioBuffer out, float dirX, float dirY, float dirZ, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsEncodeEffect or AudioBuffer is closed");
        }
        return nApply(peer, dirX, dirY, dirZ, order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Освобождает эффект ({@code iplAmbisonicsEncodeEffectRelease}). Повторный
     * вызов безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int maxOrder);

    private static native int nApply(long effectPeer, float dirX, float dirY, float dirZ, int order,
                                     long inPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
