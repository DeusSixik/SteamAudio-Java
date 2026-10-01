package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты сцены и статических мешей: создание геометрии, commit,
 * сериализация, валидация входных данных.
 */
class SceneTest {

    /** Квадрат из 4 вершин и 2 треугольников (CCW winding order). */
    private static final float[] QUAD_VERTICES = {
            -1, 0, -1,
            -1, 0, 1,
            1, 0, 1,
            1, 0, -1,
    };

    private static final int[] QUAD_TRIANGLES = {
            0, 1, 2,
            0, 2, 3,
    };

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static int[] allZeroMaterialIndices(int count) {
        int[] indices = new int[count];
        java.util.Arrays.fill(indices, 0);
        return indices;
    }

    @Test
    void meshLifecycle() {
        try (Scene scene = new Scene(context)) {
            assertTrue(scene.isOpen());

            StaticMesh mesh = scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES,
                    allZeroMaterialIndices(2), new Material[]{Material.concrete()});
            assertTrue(mesh.isOpen());

            scene.commit();

            mesh.removeFrom(scene);
            scene.commit();

            mesh.close();
            assertFalse(mesh.isOpen());
        }
    }

    @Test
    void serializationRoundtrip() {
        try (Scene scene = new Scene(context)) {
            scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES,
                    allZeroMaterialIndices(2), new Material[]{Material.concrete()});
            scene.commit();

            try (SerializedObject serialized = new SerializedObject(context)) {
                scene.save(serialized);

                // Оборачиваем уже сериализованные данные нельзя напрямую
                // (нужен IPLSerializedObjectSettings.data), поэтому просто
                // повторно сериализуем загруженную сцену и сравниваем размеры.
                Scene loaded = Scene.load(context, serialized);
                assertTrue(loaded.isOpen());
                try (SerializedObject second = new SerializedObject(context)) {
                    loaded.save(second);
                    assertEquals(serialized.getSize(), second.getSize(),
                            "re-serialized scene must have the same size");
                }
                loaded.close();
            }
        }
    }

    @Test
    void invalidGeometryRejected() {
        try (Scene scene = new Scene(context)) {
            assertThrows(IllegalArgumentException.class,
                    () -> scene.createStaticMesh(new float[4], QUAD_TRIANGLES,
                            allZeroMaterialIndices(2), new Material[]{Material.concrete()}));
            assertThrows(IllegalArgumentException.class,
                    () -> scene.createStaticMesh(QUAD_VERTICES, new int[]{0, 1, 9},
                            new int[]{0}, new Material[]{Material.concrete()}));
            assertThrows(IllegalArgumentException.class,
                    () -> scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES,
                            new int[]{0, 5}, new Material[]{Material.concrete()}));
            assertThrows(IllegalArgumentException.class,
                    () -> scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES,
                            allZeroMaterialIndices(2), new Material[]{}));
        }
    }

    @Test
    void closedSceneIsNotUsable() {
        Scene scene = new Scene(context);
        scene.close();
        assertFalse(scene.isOpen());
        assertThrows(IllegalStateException.class, scene::commit);
        assertDoesNotThrow(scene::close);
    }
}
