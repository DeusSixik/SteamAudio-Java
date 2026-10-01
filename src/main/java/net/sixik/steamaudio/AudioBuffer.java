package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLAudioBuffer} — аудио-буфером Steam Audio.
 * <p>
 * Все буферы Steam Audio — uncompressed PCM с 32-битными float-сэмплами,
 * хранятся деинтерливнутыми (SoA: отдельный массив на канал). Буфер
 * выделяется через {@code iplAudioBufferAllocate}; память принадлежит
 * Steam Audio и освобождается через {@code iplAudioBufferFree} при
 * {@link #close()}.
 * <p>
 * Буфер должен быть закрыт <b>до</b> закрытия {@link Context}, который его
 * создал, иначе освобождение памяти будет обращено к уже уничтоженному
 * контексту.
 */
public final class AudioBuffer implements AutoCloseable {

    /** Peer (opaque-указатель) родительского контекста Steam Audio. */
    private final long contextPeer;

    /** Число каналов. */
    private final int numChannels;

    /** Число сэмплов на канал. */
    private final int numSamples;

    /**
     * Указатель на нативную структуру {@code IPLAudioBuffer}; 0 означает
     * закрытый буфер.
     */
    private long peer;

    /**
     * Создает и выделяет аудио-буфер через {@code iplAudioBufferAllocate}.
     *
     * @param context     контекст, через который выделяется память буфера
     * @param numChannels число каналов
     * @param numSamples  число сэмплов на канал
     * @throws SteamAudioException    если Steam Audio вернул ошибку
     * @throws IllegalStateException  если контекст закрыт
     */
    public AudioBuffer(Context context, int numChannels, int numSamples) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = contextPeerOf(context);
        this.numChannels = numChannels;
        this.numSamples = numSamples;
        this.peer = nAllocate(contextPeer, numChannels, numSamples);
    }

    /**
     * Возвращает число каналов.
     *
     * @return число каналов
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Возвращает число сэмплов на канал.
     *
     * @return число сэмплов на канал
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Проверяет, что буфер открыт.
     *
     * @return {@code true}, если буфер создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Читает сэмплы из буфера и записывает их в interleaved-массив
     * ({@code iplAudioBufferInterleave}).
     *
     * @param dst массив длиной не менее {@code numChannels * numSamples};
     *            заполняется по формату LRLRLR... (interleaved)
     */
    public void interleaveTo(float[] dst) {
        requireOpen();
        if (dst.length < numChannels * numSamples) {
            throw new IllegalArgumentException(
                    "dst too small: expected >= " + (numChannels * numSamples) + " samples, got " + dst.length);
        }
        nInterleave(contextPeer, peer, dst);
    }

    /**
     * Записывает interleaved-сэмплы из массива в буфер
     * ({@code iplAudioBufferDeinterleave}).
     *
     * @param src interleaved-массив длиной не менее
     *            {@code numChannels * numSamples}
     */
    public void deinterleaveFrom(float[] src) {
        requireOpen();
        if (src.length < numChannels * numSamples) {
            throw new IllegalArgumentException(
                    "src too small: expected >= " + (numChannels * numSamples) + " samples, got " + src.length);
        }
        nDeinterleave(contextPeer, peer, src);
    }

    /**
     * Освобождает буфер ({@code iplAudioBufferFree}). Повторный вызов
     * безопасен.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nFree(contextPeer, peer);
            peer = 0;
        }
    }

    /**
     * Возвращает peer-указатель контекста, не требуя публичного доступа
     * к полю {@link Context}.
     *
     * @param context открытый контекст
     * @return opaque-указатель контекста
     */
    private static long contextPeerOf(Context context) {
        return context.peerForChildren();
    }

    /**
     * Возвращает peer-указатель нативной структуры буфера для дочерних
     * объектов пакета (например, {@code BinauralEffect}).
     *
     * @return указатель на нативную структуру {@code IPLAudioBuffer}
     */
    long peerForEffect() {
        return peer;
    }

    /**
     * Проверяет, что буфер открыт.
     *
     * @throws IllegalStateException если буфер закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("AudioBuffer is closed");
        }
    }

    /**
     * Выделяет буфер через {@code iplAudioBufferAllocate}.
     *
     * @param contextPeer opaque-указатель контекста
     * @param numChannels число каналов
     * @param numSamples  число сэмплов на канал
     * @return указатель на нативную структуру {@code IPLAudioBuffer}
     */
    private static native long nAllocate(long contextPeer, int numChannels, int numSamples);

    /**
     * Освобождает буфер через {@code iplAudioBufferFree} и удаляет
     * нативную структуру.
     *
     * @param contextPeer opaque-указатель контекста
     * @param peer        указатель на нативную структуру буфера
     */
    private static native void nFree(long contextPeer, long peer);

    /**
     * Интерливит сэмплы буфера в массив ({@code iplAudioBufferInterleave}).
     *
     * @param contextPeer opaque-указатель контекста
     * @param peer        указатель на нативную структуру буфера
     * @param dst         interleaved-массив назначения
     */
    private static native void nInterleave(long contextPeer, long peer, float[] dst);

    /**
     * Деинтерливит сэмплы массива в буфер
     * ({@code iplAudioBufferDeinterleave}).
     *
     * @param contextPeer opaque-указатель контекста
     * @param peer        указатель на нативную структуру буфера
     * @param src         interleaved-массив источника
     */
    private static native void nDeinterleave(long contextPeer, long peer, float[] src);
}
