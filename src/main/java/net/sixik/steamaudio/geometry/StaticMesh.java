package net.sixik.steamaudio.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.sixik.steamaudio.core.SerializedObject;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper around {@code IPLStaticMesh} — a static mesh of acoustic
 * geometry. Meshes cannot move; for moving objects there is
 * {@link InstancedMesh}.
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
     * Replaces one of the mesh's materials ({@code iplStaticMeshSetMaterial});
     * call {@link Scene#commit()} afterwards.
     *
     * @param scene     the scene the mesh belongs to
     * @param newMaterial the replacement material
     * @param index     index of the material to replace
     * @throws IllegalStateException    if the mesh or the scene is closed
     * @throws IllegalArgumentException if the index is negative
     */
    public void setMaterial(Scene scene, Material newMaterial, int index) {
        if (peer == 0 || scene == null || !scene.isOpen()) {
            throw new IllegalStateException("StaticMesh or Scene is closed");
        }
        if (index < 0) {
            throw new IllegalArgumentException("material index must be non-negative");
        }
        ByteBuffer materialBuffer = ByteBuffer
                .allocateDirect((2 * Material.NUM_BANDS + 1) * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        for (float value : newMaterial.absorption) {
            materialBuffer.putFloat(value);
        }
        materialBuffer.putFloat(newMaterial.scattering);
        for (float value : newMaterial.transmission) {
            materialBuffer.putFloat(value);
        }
        nSetMaterial(peer, scene.peerForStaticMesh(), materialBuffer, index);
    }

    /**
     * Saves the mesh to a serialized object ({@code iplStaticMeshSave}).
     *
     * @param destination open serialized object
     * @throws IllegalStateException if the mesh or the serialized object is closed
     */
    public void save(SerializedObject destination) {
        if (peer == 0 || destination == null || !destination.isOpen()) {
            throw new IllegalStateException("StaticMesh or SerializedObject is closed");
        }
        nSave(peer, destination.peerForChildren());
    }

    /**
     * Loads a static mesh from a serialized object
     * ({@code iplStaticMeshLoad}); call {@link StaticMesh#addTo} and
     * {@link Scene#commit()} afterwards.
     *
     * @param scene scene the mesh will belong to
     * @param source serialized object containing the mesh data
     * @return the loaded mesh
     * @throws SteamAudioException   if Steam Audio returns an error
     * @throws IllegalStateException if the scene or the serialized object is closed
     */
    public static StaticMesh load(Scene scene, SerializedObject source) {
        if (scene == null || !scene.isOpen() || source == null || !source.isOpen()) {
            throw new IllegalStateException("Scene or SerializedObject is closed");
        }
        StaticMesh mesh = new StaticMesh(0);
        mesh.peer = nLoad(scene.peerForStaticMesh(), source.peerForChildren());
        if (mesh.peer == 0) {
            throw new SteamAudioException("Failed to load static mesh");
        }
        return mesh;
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
     * Replaces a material via {@code iplStaticMeshSetMaterial}.
     *
     * @param peer          opaque pointer of the mesh
     * @param scenePeer     opaque pointer of the scene
     * @param newMaterial   direct buffer with the {@code IPLMaterial} layout
     *                      (absorption[3], scattering, transmission[3])
     * @param index         index of the material to replace
     */
    private static native void nSetMaterial(long peer, long scenePeer,
                                            ByteBuffer newMaterial, int index);

    /**
     * Saves the mesh via {@code iplStaticMeshSave}.
     *
     * @param peer            opaque pointer of the mesh
     * @param destinationPeer opaque pointer of the serialized object
     */
    private static native void nSave(long peer, long destinationPeer);

    /**
     * Loads the mesh via {@code iplStaticMeshLoad}.
     *
     * @param scenePeer  opaque pointer of the scene
     * @param sourcePeer opaque pointer of the serialized object
     * @return opaque pointer of the loaded mesh
     */
    private static native long nLoad(long scenePeer, long sourcePeer);

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
