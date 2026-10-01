package net.sixik.steamaudio;

/**
 * Обертка над {@code IPLStaticMesh} — статическим мешем акустической
 * геометрии. Меши не могут двигаться; для движущихся объектов существует
 * {@code InstancedMesh} (будет добавлен позже).
 */
public final class StaticMesh implements AutoCloseable {

    /** Opaque-указатель на {@code IPLStaticMesh}; 0 означает закрытый меш. */
    private long peer;

    /**
     * Создает обертку вокруг существующего handle (вызывается из
     * {@link Scene#createStaticMesh}).
     *
     * @param peer opaque-указатель на {@code IPLStaticMesh}
     */
    StaticMesh(long peer) {
        this.peer = peer;
    }

    /**
     * Проверяет, что меш открыт.
     *
     * @return {@code true}, если меш создан и не закрыт
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Добавляет меш в сцену ({@code iplStaticMeshAdd}); после этого
     * необходимо вызвать {@link Scene#commit()}.
     *
     * @param scene открытая сцена
     */
    public void addTo(Scene scene) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("StaticMesh or Scene is closed");
        }
        nAdd(peer, scene.peerForStaticMesh());
    }

    /**
     * Удаляет меш из сцены ({@code iplStaticMeshRemove}); после этого
     * необходимо вызвать {@link Scene#commit()}.
     *
     * @param scene открытая сцена
     */
    public void removeFrom(Scene scene) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("StaticMesh or Scene is closed");
        }
        nRemove(peer, scene.peerForStaticMesh());
    }

    /**
     * Освобождает меш ({@code iplStaticMeshRelease}). Повторный вызов
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
     * Добавляет меш в сцену через {@code iplStaticMeshAdd}.
     *
     * @param peer      opaque-указатель меша
     * @param scenePeer opaque-указатель сцены
     */
    private static native void nAdd(long peer, long scenePeer);

    /**
     * Удаляет меш из сцены через {@code iplStaticMeshRemove}.
     *
     * @param peer      opaque-указатель меша
     * @param scenePeer opaque-указатель сцены
     */
    private static native void nRemove(long peer, long scenePeer);

    /**
     * Освобождает меш через {@code iplStaticMeshRelease}.
     *
     * @param peer opaque-указатель меша
     */
    private static native void nRelease(long peer);
}
