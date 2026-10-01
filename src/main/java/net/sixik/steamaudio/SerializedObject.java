package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLSerializedObject} — сериализованным
 * представлением API-объекта Steam Audio (например, сцены).
 * <p>
 * Пустой объект создается для сериализации существующего объекта,
 * либо объект оборачивает существующий массив байтов для десериализации.
 */
public final class SerializedObject implements AutoCloseable {

    /** Opaque-указатель на {@code IPLSerializedObject}; 0 означает закрытый объект. */
    private long peer;

    /**
     * Создает пустой сериализованный объект через
     * {@code iplSerializedObjectCreate}.
     *
     * @param context контекст Steam Audio
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public SerializedObject(Context context) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        peer = nCreate(context.peerForChildren());
    }

    /**
     * Проверяет, что объект открыт.
     *
     * @return {@code true}, если объект создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Возвращает размер сериализованных данных в байтах
     * ({@code iplSerializedObjectGetSize}).
     *
     * @return размер данных в байтах
     */
    public long getSize() {
        requireOpen();
        return nGetSize(peer);
    }

    /**
     * Возвращает копию сериализованных данных
     * ({@code iplSerializedObjectGetData}).
     *
     * @return массив байтов длиной {@link #getSize()}
     */
    public byte[] getData() {
        requireOpen();
        long size = getSize();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Serialized data too large: " + size + " bytes");
        }
        byte[] data = new byte[(int) size];
        nGetData(peer, data);
        return data;
    }

    /**
     * Проверяет, что объект открыт.
     *
     * @throws IllegalStateException если объект закрыт
     */
    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("SerializedObject is closed");
        }
    }

    /**
     * Освобождает объект ({@code iplSerializedObjectRelease}). Повторный
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
     * Возвращает opaque-указатель объекта для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLSerializedObject}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Создает пустой объект через {@code iplSerializedObjectCreate}.
     *
     * @param contextPeer opaque-указатель контекста
     * @return opaque-указатель на созданный объект
     */
    private static native long nCreate(long contextPeer);

    /**
     * Возвращает размер данных через {@code iplSerializedObjectGetSize}.
     *
     * @param peer opaque-указатель объекта
     * @return размер данных в байтах
     */
    private static native long nGetSize(long peer);

    /**
     * Копирует данные объекта в массив назначения.
     *
     * @param peer opaque-указатель объекта
     * @param out  массив назначения длиной {@link #getSize()}
     */
    private static native void nGetData(long peer, byte[] out);

    /**
     * Освобождает объект через {@code iplSerializedObjectRelease}.
     *
     * @param peer opaque-указатель объекта
     */
    private static native void nRelease(long peer);
}
