package net.sixik.steamaudio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Обертка над {@code IPLScene} — сценой, хранящей акустическую геометрию.
 * <p>
 * Сцена создается со встроенным трассировщиком лучей
 * ({@code IPL_SCENETYPE_DEFAULT}); типы Embree/RadeonRays/custom будут
 * добавлены позже. После добавления или удаления мешей необходимо вызвать
 * {@link #commit()}.
 * <p>
 * Геометрия передается в Steam Audio через прямые {@link ByteBuffer}
 * с нативным порядком байтов — без копирования в нативную сторону.
 */
public final class Scene implements AutoCloseable {

    /** Встроенный CPU-трассировщик лучей Steam Audio ({@code IPL_SCENETYPE_DEFAULT}). */
    public static final int SCENE_TYPE_DEFAULT = 0;

    /** Opaque-указатель на {@code IPLScene}; 0 означает закрытую сцену. */
    private long peer;

    /** Opaque-указатель родительского контекста. */
    private final long contextPeer;

    /**
     * Создает сцену встроенного типа через {@code iplSceneCreate}.
     *
     * @param context контекст Steam Audio
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public Scene(Context context) {
        this(context, SCENE_TYPE_DEFAULT);
    }

    /**
     * Создает сцену заданного типа через {@code iplSceneCreate}.
     *
     * @param context контекст Steam Audio
     * @param type    тип сцены: {@link #SCENE_TYPE_DEFAULT}
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public Scene(Context context, int type) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        this.peer = nCreate(contextPeer, type);
    }

    /**
     * Создает обертку вокруг существующего handle (используется
     * в {@link #load}).
     *
     * @param contextPeer opaque-указатель родительского контекста
     * @param peer        opaque-указатель на {@code IPLScene}
     */
    private Scene(long contextPeer, long peer) {
        this.contextPeer = contextPeer;
        this.peer = peer;
    }

    /**
     * Проверяет, что сцена открыта.
     *
     * @return {@code true}, если сцена создана и не закрыта
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Создает статический меш и добавляет его в сцену
     * ({@code iplStaticMeshCreate}).
     * <p>
     * Вершины заданы в порядке XYZ: {@code [x0, y0, z0, x1, y1, z1, ...]}.
     * Треугольники — индексированные тройки вершин против часовой стрелки
     * (при взгляде со стороны нормали). Порядок обхода — против часовой
     * стрелки (counter-clockwise winding order).
     *
     * @param vertices        массив вершин, по 3 float на вершину
     * @param triangles       массив индексов, по 3 int на треугольник
     * @param materialIndices индекс материала для каждого треугольника
     * @param materials       массив материалов
     * @return созданный меш
     * @throws IllegalArgumentException если размеры массивов некорректны
     * @throws IllegalStateException    если сцена закрыта
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

        // В Steam Audio 4.x iplStaticMeshCreate не добавляет меш в сцену —
        // требуется явный iplStaticMeshAdd.
        StaticMesh staticMesh = new StaticMesh(nCreateStaticMesh(peer, numVertices, numTriangles, numMaterials,
                flatten(vertices), flattenTriangles(triangles),
                flatten(materialIndices), flattenMaterials(materials)));
        staticMesh.addTo(this);
        return staticMesh;
    }

    /**
     * Фиксирует изменения в сцене ({@code iplSceneCommit}). Обязательно
     * после добавления или удаления мешей.
     */
    public void commit() {
        if (peer == 0) {
            throw new IllegalStateException("Scene is closed");
        }
        nCommit(peer);
    }

    /**
     * Сохраняет сцену в сериализованный объект
     * ({@code iplSceneSave}).
     *
     * @param destination открытый сериализованный объект
     */
    public void save(SerializedObject destination) {
        if (peer == 0 || !destination.isOpen()) {
            throw new IllegalStateException("Scene or SerializedObject is closed");
        }
        nSave(peer, destination.peerForChildren());
    }

    /**
     * Загружает сцену из сериализованного объекта
     * ({@code iplSceneLoad}).
     *
     * @param context контекст Steam Audio
     * @param source  сериализованный объект с данными сцены
     * @return загруженная сцена
     * @throws SteamAudioException   если Steam Audio вернул ошибку
     * @throws IllegalStateException если контекст закрыт
     */
    public static Scene load(Context context, SerializedObject source) {
        if (!context.isOpen() || !source.isOpen()) {
            throw new IllegalStateException("Context or SerializedObject is closed");
        }
        return new Scene(context.peerForChildren(), nLoad(context.peerForChildren(), SCENE_TYPE_DEFAULT, source.peerForChildren()));
    }

    /**
     * Освобождает сцену ({@code iplSceneRelease}). Повторный вызов
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
     * Возвращает opaque-указатель сцены для дочерних объектов пакета.
     *
     * @return opaque-указатель на {@code IPLScene}
     */
    long peerForChildren() {
        return peer;
    }

    /**
     * Возвращает opaque-указатель сцены для статических мешей.
     *
     * @return opaque-указатель на {@code IPLScene}
     */
    long peerForStaticMesh() {
        return peer;
    }

    /**
     * Упаковывает массив вершин в прямой {@link ByteBuffer} с нативным
     * порядком байтов (формат {@code IPLVector3[]}).
     *
     * @param vertices массив вершин XYZ
     * @return прямой буфер на {@code vertices.length} float
     */
    private static ByteBuffer flatten(float[] vertices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * Float.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vertices);
        return buffer;
    }

    /**
     * Упаковывает массив индексов треугольников в прямой
     * {@link ByteBuffer} (формат {@code IPLTriangle[]}).
     *
     * @param triangles массив индексов, по 3 на треугольник
     * @return прямой буфер на {@code triangles.length} int
     */
    private static ByteBuffer flattenTriangles(int[] triangles) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(triangles.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asIntBuffer().put(triangles);
        return buffer;
    }

    /**
     * Упаковывает массив индексов материалов в прямой {@link ByteBuffer}.
     *
     * @param materialIndices индекс материала на каждый треугольник
     * @return прямой буфер на {@code materialIndices.length} int
     */
    private static ByteBuffer flatten(int[] materialIndices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(materialIndices.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        buffer.asIntBuffer().put(materialIndices);
        return buffer;
    }

    /**
     * Упаковывает материалы в прямой {@link ByteBuffer} с раскладкой
     * {@code IPLMaterial}: {@code absorption[3], scattering, transmission[3]} —
     * 7 float на материал.
     *
     * @param materials массив материалов
     * @return прямой буфер на {@code 7 * materials.length} float
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
     * Создает сцену через {@code iplSceneCreate}.
     *
     * @param contextPeer opaque-указатель контекста
     * @param type        тип сцены ({@code IPLSceneType})
     * @return opaque-указатель на созданную сцену
     */
    private static native long nCreate(long contextPeer, int type);

    /**
     * Создает статический меш через {@code iplStaticMeshCreate}.
     *
     * @param scenePeer    opaque-указатель сцены
     * @param numVertices  число вершин
     * @param numTriangles число треугольников
     * @param numMaterials число материалов
     * @param vertices     прямой буфер с вершинами ({@code IPLVector3[]})
     * @param triangles    прямой буфер с треугольниками ({@code IPLTriangle[]})
     * @param materialIndices прямой буфер с индексами материалов ({@code IPLint32[]})
     * @param materials    прямой буфер с материалами ({@code IPLMaterial[]})
     * @return opaque-указатель на созданный меш
     */
    private static native long nCreateStaticMesh(long scenePeer, int numVertices, int numTriangles, int numMaterials,
                                                 ByteBuffer vertices, ByteBuffer triangles,
                                                 ByteBuffer materialIndices, ByteBuffer materials);

    /**
     * Фиксирует сцену через {@code iplSceneCommit}.
     *
     * @param peer opaque-указатель сцены
     */
    private static native void nCommit(long peer);

    /**
     * Сохраняет сцену через {@code iplSceneSave}.
     *
     * @param peer       opaque-указатель сцены
     * @param destinationPeer opaque-указатель сериализованного объекта
     */
    private static native void nSave(long peer, long destinationPeer);

    /**
     * Загружает сцену через {@code iplSceneLoad}.
     *
     * @param contextPeer opaque-указатель контекста
     * @param type        тип сцены ({@code IPLSceneType})
     * @param sourcePeer  opaque-указатель сериализованного объекта
     * @return opaque-указатель на загруженную сцену
     */
    private static native long nLoad(long contextPeer, int type, long sourcePeer);

    /**
     * Освобождает сцену через {@code iplSceneRelease}.
     *
     * @param peer opaque-указатель сцены
     */
    private static native void nRelease(long peer);
}
