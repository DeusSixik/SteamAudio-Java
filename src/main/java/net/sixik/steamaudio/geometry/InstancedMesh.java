package net.sixik.steamaudio.geometry;

import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLInstancedMesh}: places one scene (the sub-scene)
 * inside another scene with a transform. Useful for moving objects: the
 * sub-scene contains the geometry, the instance updates only its transform,
 * avoiding a full scene rebuild.
 * <p>
 * After {@link #create}, {@link #removeFrom} or {@link #updateTransform},
 * call {@link Scene#commit()} on the parent scene for the changes to take
 * effect.
 */
public final class InstancedMesh implements AutoCloseable {

    /** Opaque pointer to {@code IPLInstancedMesh}; 0 means the mesh is closed. */
    private long peer;

    private InstancedMesh(long peer) {
        this.peer = peer;
    }

    /**
     * Creates an instanced mesh placing {@code subScene} into {@code scene}
     * with the given local-to-world transform
     * ({@code iplInstancedMeshCreate}). The mesh is NOT added to the scene
     * automatically; call {@link #addTo} afterwards.
     *
     * @param scene     parent scene that will contain the instance
     * @param subScene  scene whose geometry is instantiated
     * @param transform row-major 4x4 local-to-world transform (16 floats),
     *                  or {@code null} for identity
     * @return the created instanced mesh
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if either scene is closed
     */
    public static InstancedMesh create(Scene scene, Scene subScene, float[] transform) {
        if (scene == null || !scene.isOpen() || subScene == null || !subScene.isOpen()) {
            throw new IllegalStateException("Scene or sub-scene is closed");
        }
        if (transform != null && transform.length < 16) {
            throw new IllegalArgumentException("transform must contain 16 elements");
        }
        return new InstancedMesh(nCreate(scene.peerForChildren(), subScene.peerForChildren(), transform));
    }

    /**
     * Checks whether the mesh is open.
     *
     * @return {@code true} if the mesh is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Adds the mesh to the scene ({@code iplInstancedMeshAdd}); call
     * {@link Scene#commit()} afterwards.
     *
     * @param scene parent scene
     */
    public void addTo(Scene scene) {
        requireOpen();
        if (scene == null || !scene.isOpen()) {
            throw new IllegalStateException("Scene is closed");
        }
        nAdd(peer, scene.peerForChildren());
    }

    /**
     * Removes the mesh from the scene ({@code iplInstancedMeshRemove}); call
     * {@link Scene#commit()} afterwards. The mesh can be added back with
     * {@link #addTo}.
     *
     * @param scene parent scene
     */
    public void removeFrom(Scene scene) {
        requireOpen();
        if (scene == null || !scene.isOpen()) {
            throw new IllegalStateException("Scene is closed");
        }
        nRemove(peer, scene.peerForChildren());
    }

    /**
     * Updates the local-to-world transform of the instance
     * ({@code iplInstancedMeshUpdateTransform}); call {@link Scene#commit()}
     * afterwards.
     *
     * @param scene     parent scene
     * @param transform row-major 4x4 local-to-world transform (16 floats)
     */
    public void updateTransform(Scene scene, float[] transform) {
        requireOpen();
        if (scene == null || !scene.isOpen()) {
            throw new IllegalStateException("Scene is closed");
        }
        if (transform == null || transform.length < 16) {
            throw new IllegalArgumentException("transform must contain 16 elements");
        }
        nUpdateTransform(peer, scene.peerForChildren(), transform);
    }

    /**
     * Releases the mesh ({@code iplInstancedMeshRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("InstancedMesh is closed");
        }
    }

    private static native long nCreate(long scenePeer, long subScenePeer, float[] transform);

    private static native void nAdd(long peer, long scenePeer);

    private static native void nRemove(long peer, long scenePeer);

    private static native void nUpdateTransform(long peer, long scenePeer, float[] transform);

    private static native void nRelease(long peer);
}
