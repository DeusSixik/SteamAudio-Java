package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLAmbisonicsRotationEffect} — поворачивает
 * амбисонический буфер из "мировых координат" в систему координат
 * слушателя. Может применяться in-place.
 */
public final class AmbisonicsRotationEffect implements AutoCloseable {

    /** Opaque-указатель на {@code IPLAmbisonicsRotationEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает эффект через {@code iplAmbisonicsRotationEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param maxOrder     максимальный порядок обработки
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public AmbisonicsRotationEffect(Context context, int samplingRate, int frameSize, int maxOrder) {
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
     * Поворачивает буфер под ориентацию слушателя
     * ({@code iplAmbisonicsRotationEffectApply}).
     *
     * @param in            входной буфер с {@code (order + 1)^2} каналами
     * @param out           выходной буфер (может совпадать со входом)
     * @param listenerPosition      мировые координаты слушателя
     * @param listenerAhead         единичный вектор «вперед» слушателя
     * @param listenerUp            единичный вектор «вверх» слушателя
     * @param order         порядок обработки (не более {@code maxOrder})
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     */
    public int apply(AudioBuffer in, AudioBuffer out,
                     Vector3 listenerPosition, Vector3 listenerAhead, Vector3 listenerUp, int order) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("AmbisonicsRotationEffect or AudioBuffer is closed");
        }
        return nApply(peer, listenerPosition.x, listenerPosition.y, listenerPosition.z,
                listenerAhead.x, listenerAhead.y, listenerAhead.z,
                listenerUp.x, listenerUp.y, listenerUp.z,
                order, in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Освобождает эффект ({@code iplAmbisonicsRotationEffectRelease}). Повторный
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

    private static native int nApply(long effectPeer,
                                     float listenerX, float listenerY, float listenerZ,
                                     float aheadX, float aheadY, float aheadZ,
                                     float upX, float upY, float upZ,
                                     int order, long inPeer, long outPeer);

    private static native void nRelease(long effectPeer);
}
