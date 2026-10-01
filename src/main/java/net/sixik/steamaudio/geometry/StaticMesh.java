package net.sixik.steamaudio.geometry;
import net.sixik.steamaudio.core.SteamAudio;

/**
 * Wrapper around {@code IPLStaticMesh} — a static mesh of acoustic
 * geometry. Meshes cannot move; for moving objects there is
 * {@code InstancedMesh} (to be added later).
 */
public final class StaticMesh implements AutoCloseable {

    /** Opaque pointer to {@code IPLStaticMesh}; 0 means the mesh is closed. */
    private long peer;

    /**
     * Creates a wrapper around an existing handle (called from
     * {@link Scene#createStaticMesh}).
     *
     * @param peer opaque pointer to {@code IPLStaticMesh}
     */
    StaticMesh(long peer) {
        this.peer = peer;
    }

    /**
     * Checks whether the mesh is open.
     *
     * @return {@code true} if the mesh has been created and is not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Adds the mesh to the scene ({@code iplStaticMeshAdd}); afterwards
     * {@link Scene#commit()} must be called.
     *
     * @param scene open scene
     */
    public void addTo(Scene scene) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("StaticMesh or Scene is closed");
        }
        nAdd(peer, scene.peerForStaticMesh());
    }

    /**
     * Removes the mesh from the scene ({@code iplStaticMeshRemove}); afterwards
     * {@link Scene#commit()} must be called.
     *
     * @param scene open scene
     */
    public void removeFrom(Scene scene) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("StaticMesh or Scene is closed");
        }
        nRemove(peer, scene.peerForStaticMesh());
    }

    /**
     * Releases the mesh ({@code iplStaticMeshRelease}). Calling it again
     * is safe.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    /**
     * Adds the mesh to the scene via {@code iplStaticMeshAdd}.
     *
     * @param peer      opaque pointer of the mesh
     * @param scenePeer opaque pointer of the scene
     */
    private static native void nAdd(long peer, long scenePeer);

    /**
     * Removes the mesh from the scene via {@code iplStaticMeshRemove}.
     *
     * @param peer      opaque pointer of the mesh
     * @param scenePeer opaque pointer of the scene
     */
    private static native void nRemove(long peer, long scenePeer);

    /**
     * Releases the mesh via {@code iplStaticMeshRelease}.
     *
     * @param peer opaque pointer of the mesh
     */
    private static native void nRelease(long peer);
}
