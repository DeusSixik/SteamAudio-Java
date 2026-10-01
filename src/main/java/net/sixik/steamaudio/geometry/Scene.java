package net.sixik.steamaudio.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SerializedObject;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper around {@code IPLScene} — a scene that stores acoustic geometry.
 * <p>
 * The scene is created with the built-in ray tracer
 * ({@code IPL_SCENETYPE_DEFAULT}); Embree/RadeonRays/custom types will be
 * added later. After adding or removing meshes, {@link #commit()} must be
 * called.
 * <p>
 * Geometry is passed to Steam Audio via direct {@link ByteBuffer}s with
 * native byte order — no copying on the native side.
 */
public final class Scene implements AutoCloseable {

    /** Built-in Steam Audio CPU ray tracer ({@code IPL_SCENETYPE_DEFAULT}). */
    public static final int SCENE_TYPE_DEFAULT = 0;

    /** Opaque pointer to {@code IPLScene}; 0 means the scene is closed. */
    private long peer;

    /** Opaque pointer of the parent context. */
    private final long contextPeer;

    /**
     * Creates a scene of the built-in type via {@code iplSceneCreate}.
     *
     * @param context Steam Audio context
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public Scene(Context context) {
        this(context, SCENE_TYPE_DEFAULT);
    }

    /**
     * Creates a scene of the given type via {@code iplSceneCreate}.
     *
     * @param context Steam Audio context
     * @param type    scene type: {@link #SCENE_TYPE_DEFAULT}
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public Scene(Context context, int type) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.peer = nCreate(contextPeer, type);
    }

    /**
     * Creates a wrapper around an existing handle (used in
     * {@link #load}).
     *
     * @param contextPeer opaque pointer of the parent context
     * @param peer        opaque pointer to {@code IPLScene}
     */
    private Scene(long contextPeer, long peer) {
        this.contextPeer = contextPeer;
        this.peer = peer;
    }

    /**
     * Checks whether the scene is open.
     *
     * @return {@code true} if the scene has been created and is not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Creates a static mesh and adds it to the scene
     * ({@code iplStaticMeshCreate}).
     * <p>
     * Vertices are given in XYZ order: {@code [x0, y0, z0, x1, y1, z1, ...]}.
     * Triangles are indexed triples of vertices in counter-clockwise order
     * (when viewed from the side the normal points to). The winding order is
     * counter-clockwise (counter-clockwise winding order).
     *
     * @param vertices        vertex array, 3 floats per vertex
     * @param triangles       index array, 3 ints per triangle
     * @param materialIndices material index for each triangle
     * @param materials       material array
     * @return the created mesh
     * @throws IllegalArgumentException if the array sizes are invalid
     * @throws IllegalStateException    if the scene is closed
     */
    public StaticMesh createStaticMesh(float[] vertices, int[] triangles,
                                       int[] materialIndices, Material[] materials) {
        if (peer == 0) {
            throw new IllegalStateException("Scene is closed");
        }

        int numVertices = vertices.length / 3;
        int numTriangles = triangles.length / 3;
        int numMaterials = materials.length;

        if (vertices.length != numVertices * 3 || numVertices < 1) {
            throw new IllegalArgumentException("vertices length must be a multiple of 3");
        }
        if (triangles.length != numTriangles * 3 || numTriangles < 1) {
            throw new IllegalArgumentException("triangles length must be a multiple of 3");
        }
        if (materialIndices.length != numTriangles) {
            throw new IllegalArgumentException(
                    "materialIndices length must equal number of triangles (" + numTriangles + ")");
        }
        if (numMaterials < 1) {
            throw new IllegalArgumentException("at least one material is required");
        }
        for (int index : triangles) {
            if (index < 0 || index >= numVertices) {
                throw new IllegalArgumentException("triangle vertex index out of range: " + index);
            }
        }
        for (int index : materialIndices) {
            if (index < 0 || index >= numMaterials) {
                throw new IllegalArgumentException("material index out of range: " + index);
            }
        }

        // In Steam Audio 4.x, iplStaticMeshCreate does not add the mesh to
        // the scene — an explicit iplStaticMeshAdd is required.
        StaticMesh staticMesh = new StaticMesh(nCreateStaticMesh(peer, numVertices, numTriangles, numMaterials,
                flatten(vertices), flattenTriangles(triangles),
                flatten(materialIndices), flattenMaterials(materials)));
        staticMesh.addTo(this);
        return staticMesh;
    }

    /**
     * Commits changes to the scene ({@code iplSceneCommit}). Mandatory
     * after adding or removing meshes.
     */
    public void commit() {
        if (peer == 0) {
            throw new IllegalStateException("Scene is closed");
        }
        nCommit(peer);
    }

    /**
     * Saves the scene to a serialized object
     * ({@code iplSceneSave}).
     *
     * @param destination open serialized object
     */
    public void save(SerializedObject destination) {
        if (peer == 0 || !destination.isOpen()) {
            throw new IllegalStateException("Scene or SerializedObject is closed");
        }
        nSave(peer, destination.peerForChildren());
    }

    /**
     * Loads a scene from a serialized object
     * ({@code iplSceneLoad}).
     *
     * @param context Steam Audio context
     * @param source  serialized object with scene data
     * @return the loaded scene
     * @throws SteamAudioException   if Steam Audio returned an error
     * @throws IllegalStateException if the context is closed
     */
    public static Scene load(Context context, SerializedObject source) {
        if (!context.isOpen() || !source.isOpen()) {
            throw new IllegalStateException("Context or SerializedObject is closed");
        }
        return new Scene(context.peerForChildren(), nLoad(context.peerForChildren(), SCENE_TYPE_DEFAULT, source.peerForChildren()));
    }

    /**
     * Releases the scene ({@code iplSceneRelease}). Calling it again
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
     * Returns the opaque scene pointer for child objects of the package.
     *
     * @return opaque pointer to {@code IPLScene}
     */
    public long peerForChildren() {
        return peer;
    }

    /**
     * Returns the opaque scene pointer for static meshes.
     *
     * @return opaque pointer to {@code IPLScene}
     */
    long peerForStaticMesh() {
        return peer;
    }

    /**
     * Packs a vertex array into a direct {@link ByteBuffer} with native
     * byte order ({@code IPLVector3[]} format).
     *
     * @param vertices XYZ vertex array
     * @return direct buffer of {@code vertices.length} floats
     */
    private static ByteBuffer flatten(float[] vertices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vertices);
        return buffer;
    }

    /**
     * Packs a triangle index array into a direct
     * {@link ByteBuffer} ({@code IPLTriangle[]} format).
     *
     * @param triangles index array, 3 per triangle
     * @return direct buffer of {@code triangles.length} ints
     */
    private static ByteBuffer flattenTriangles(int[] triangles) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(triangles.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asIntBuffer().put(triangles);
        return buffer;
    }

    /**
     * Packs a material index array into a direct {@link ByteBuffer}.
     *
     * @param materialIndices material index per triangle
     * @return direct buffer of {@code materialIndices.length} ints
     */
    private static ByteBuffer flatten(int[] materialIndices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(materialIndices.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asIntBuffer().put(materialIndices);
        return buffer;
    }

    /**
     * Packs materials into a direct {@link ByteBuffer} with the
     * {@code IPLMaterial} layout: {@code absorption[3], scattering, transmission[3]} —
     * 7 floats per material.
     *
     * @param materials material array
     * @return direct buffer of {@code 7 * materials.length} floats
     */
    private static ByteBuffer flattenMaterials(Material[] materials) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                materials.length * (2 * Material.NUM_BANDS + 1) * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        for (Material material : materials) {
            for (float value : material.absorption) {
                buffer.putFloat(value);
            }
            buffer.putFloat(material.scattering);
            for (float value : material.transmission) {
                buffer.putFloat(value);
            }
        }
        return buffer;
    }

    /**
     * Creates a scene via {@code iplSceneCreate}.
     *
     * @param contextPeer opaque pointer of the context
     * @param type        scene type ({@code IPLSceneType})
     * @return opaque pointer to the created scene
     */
    private static native long nCreate(long contextPeer, int type);

    /**
     * Creates a static mesh via {@code iplStaticMeshCreate}.
     *
     * @param scenePeer    opaque pointer of the scene
     * @param numVertices  number of vertices
     * @param numTriangles number of triangles
     * @param numMaterials number of materials
     * @param vertices     direct buffer with vertices ({@code IPLVector3[]})
     * @param triangles    direct buffer with triangles ({@code IPLTriangle[]})
     * @param materialIndices direct buffer with material indices ({@code IPLint32[]})
     * @param materials    direct buffer with materials ({@code IPLMaterial[]})
     * @return opaque pointer to the created mesh
     */
    private static native long nCreateStaticMesh(long scenePeer, int numVertices, int numTriangles, int numMaterials,
                                                 ByteBuffer vertices, ByteBuffer triangles,
                                                 ByteBuffer materialIndices, ByteBuffer materials);

    /**
     * Commits the scene via {@code iplSceneCommit}.
     *
     * @param peer opaque pointer of the scene
     */
    private static native void nCommit(long peer);

    /**
     * Saves the scene via {@code iplSceneSave}.
     *
     * @param peer       opaque pointer of the scene
     * @param destinationPeer opaque pointer of the serialized object
     */
    private static native void nSave(long peer, long destinationPeer);

    /**
     * Loads a scene via {@code iplSceneLoad}.
     *
     * @param contextPeer opaque pointer of the context
     * @param type        scene type ({@code IPLSceneType})
     * @param sourcePeer  opaque pointer of the serialized object
     * @return opaque pointer to the loaded scene
     */
    private static native long nLoad(long contextPeer, int type, long sourcePeer);

    /**
     * Releases the scene via {@code iplSceneRelease}.
     *
     * @param peer opaque pointer of the scene
     */
    private static native void nRelease(long peer);
}
