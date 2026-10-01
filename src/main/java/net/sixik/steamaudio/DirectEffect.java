package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLDirectEffect} — эффектом рендеринга результатов
 * direct-симуляции: distance attenuation, air absorption, directivity,
 * occlusion, transmission.
 * <p>
 * Параметры эффекта передаются расплющенным массивом из 9 float —
 * результатом {@link Source#getDirectOutputsInto(float[])}:
 * {@code [distanceAttenuation, airAbs0, airAbs1, airAbs2, directivity,
 * occlusion, trans0, trans1, trans2]}. Такой формат позволяет применить
 * эффект без промежуточных объектов и аллокаций.
 * <p>
 * Эффект может применяться in-place (вход и выход — один буфер).
 */
public final class DirectEffect implements AutoCloseable {

    /** Применять distance attenuation ({@code IPL_DIRECTEFFECTFLAGS_APPLYDISTANCEATTENUATION}). */
    public static final int APPLY_DISTANCE_ATTENUATION = 1;

    /** Применять air absorption ({@code IPL_DIRECTEFFECTFLAGS_APPLYAIRABSORPTION}). */
    public static final int APPLY_AIR_ABSORPTION = 2;

    /** Применять directivity ({@code IPL_DIRECTEFFECTFLAGS_APPLYDIRECTIVITY}). */
    public static final int APPLY_DIRECTIVITY = 4;

    /** Применять occlusion ({@code IPL_DIRECTEFFECTFLAGS_APPLYOCCLUSION}). */
    public static final int APPLY_OCCLUSION = 8;

    /** Применять transmission ({@code IPL_DIRECTEFFECTFLAGS_APPLYTRANSMISSION}). */
    public static final int APPLY_TRANSMISSION = 16;

    /** Transmission, независимый от частоты ({@code IPL_TRANSMISSIONTYPE_FREQINDEPENDENT}). */
    public static final int TRANSMISSION_FREQ_INDEPENDENT = 0;

    /** Transmission, зависимый от частоты ({@code IPL_TRANSMISSIONTYPE_FREQDEPENDENT}). */
    public static final int TRANSMISSION_FREQ_DEPENDENT = 1;

    /** Opaque-указатель на {@code IPLDirectEffect}; 0 означает закрытый эффект. */
    private long peer;

    /**
     * Создает direct-эффект через {@code iplDirectEffectCreate}.
     *
     * @param context      контекст Steam Audio
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param numChannels  число каналов входных и выходных буферов
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public DirectEffect(Context context, int samplingRate, int frameSize, int numChannels) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren(), samplingRate, frameSize, numChannels);
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
     * Применяет эффект к буферу ({@code iplDirectEffectApply}).
     * <p>
     * Вход и выход должны иметь одинаковое число каналов (число каналов,
     * заданное при создании эффекта).
     *
     * @param in               входной буфер
     * @param out              выходной буфер (может совпадать с входным)
     * @param directOutputs    9 float из {@link Source#getDirectOutputsInto(float[])}
     * @param effectFlags      комбинация флагов {@code APPLY_*}
     * @param transmissionType режим transmission: {@link #TRANSMISSION_FREQ_INDEPENDENT}
     *                         или {@link #TRANSMISSION_FREQ_DEPENDENT}
     * @return состояние эффекта: {@link BinauralEffect#STATE_TAIL_REMAINING}
     *         или {@link BinauralEffect#STATE_TAIL_COMPLETE}
     * @throws IllegalStateException если эффект или буферы закрыты
     */
    public int apply(AudioBuffer in, AudioBuffer out, float[] directOutputs,
                     int effectFlags, int transmissionType) {
        if (!isOpen() || !in.isOpen() || !out.isOpen()) {
            throw new IllegalStateException("DirectEffect or AudioBuffer is closed");
        }
        if (directOutputs.length < 9) {
            throw new IllegalArgumentException("directOutputs must contain at least 9 elements");
        }
        return nApply(peer, directOutputs, effectFlags, transmissionType,
                in.peerForEffect(), out.peerForEffect());
    }

    /**
     * Сбрасывает внутреннее состояние обработки
     * ({@code iplDirectEffectReset}).
     */
    public void reset() {
        requireOpen();
        nReset(peer);
    }

    /**
     * Возвращает число оставшихся tail-сэмплов
     * ({@code iplDirectEffectGetTailSize}).
     *
     * @return число tail-сэмплов
     */
    public int getTailSize() {
        requireOpen();
        return nGetTailSize(peer);
    }

    /**
     * Извлекает один фрейм tail-сэмплов
     * ({@code iplDirectEffectGetTail}).
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
     * Проверяет, что эффект открыт.
     *
     * @throws IllegalStateException если эффект закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("DirectEffect is closed");
        }
    }

    /**
     * Освобождает эффект ({@code iplDirectEffectRelease}). Повторный
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
     * Создает эффект через {@code iplDirectEffectCreate}.
     *
     * @param contextPeer  opaque-указатель контекста
     * @param samplingRate частота дискретизации, Гц
     * @param frameSize    размер фрейма в сэмплов
     * @param numChannels  число каналов входа/выхода
     * @return opaque-указатель на созданный эффект
     */
    private static native long nCreate(long contextPeer, int samplingRate, int frameSize, int numChannels);

    /**
     * Применяет эффект через {@code iplDirectEffectApply}.
     *
     * @param effectPeer       opaque-указатель эффекта
     * @param directOutputs    расплющенные параметры из 9 float
     * @param effectFlags      флаги применяемых эффектов
     * @param transmissionType режим transmission ({@code IPLTransmissionType})
     * @param inPeer           указатель на входную структуру буфера
     * @param outPeer          указатель на выходную структуру буфера
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nApply(long effectPeer, float[] directOutputs, int effectFlags,
                                     int transmissionType, long inPeer, long outPeer);

    /**
     * Сбрасывает состояние эффекта через {@code iplDirectEffectReset}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nReset(long effectPeer);

    /**
     * Возвращает число tail-сэмплов через {@code iplDirectEffectGetTailSize}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @return число tail-сэмплов
     */
    private static native int nGetTailSize(long effectPeer);

    /**
     * Извлекает фрейм tail-сэмплов через {@code iplDirectEffectGetTail}.
     *
     * @param effectPeer opaque-указатель эффекта
     * @param outPeer    указатель на выходную структуру буфера
     * @return состояние эффекта ({@code IPLAudioEffectState})
     */
    private static native int nGetTail(long effectPeer, long outPeer);

    /**
     * Освобождает эффект через {@code iplDirectEffectRelease}.
     *
     * @param effectPeer opaque-указатель эффекта
     */
    private static native void nRelease(long effectPeer);
}
