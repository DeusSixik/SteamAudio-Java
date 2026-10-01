package net.sixik.steamaudio.geometry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SerializedObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the P2 geometry additions: {@link InstancedMesh} lifecycle and
 * {@link StaticMesh} save/load/setMaterial.
 */
class P2GeometryTest {

    /** A unit quad on the floor. */
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

    private Scene quadScene(Context ctx) {
        Scene scene = new Scene(ctx);
        scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES, new int[2],
                new Material[]{Material.concrete()});
        scene.commit();
        return scene;
    }

    @Test
    void instancedMeshLifecycle() {
        try (Scene parent = new Scene(context);
             Scene subScene = quadScene(context)) {

            float[] transform = new float[16];
            transform[0] = 1; transform[5] = 1; transform[10] = 1; transform[15] = 1;
            transform[3] = 5; // translate by +5 x

            InstancedMesh mesh = InstancedMesh.create(parent, subScene, transform);
            assertTrue(mesh.isOpen());

            mesh.addTo(parent);
            parent.commit();

            // Move the instance.
            transform[3] = -5;
            mesh.updateTransform(parent, transform);
            parent.commit();

            mesh.removeFrom(parent);
            parent.commit();

            mesh.close();

            assertThrows(IllegalStateException.class, () -> mesh.addTo(parent));
        }
    }

    @Test
    void instancedMeshTransformValidation() {
        try (Scene parent = new Scene(context);
             Scene subScene = quadScene(context)) {
            assertThrows(IllegalArgumentException.class,
                    () -> InstancedMesh.create(parent, subScene, new float[4]));
            InstancedMesh mesh = InstancedMesh.create(parent, subScene, null);
            assertThrows(IllegalArgumentException.class, () -> mesh.updateTransform(parent, new float[3]));
            mesh.close();
        }
    }

    @Test
    void staticMeshSaveLoadRoundtrip() {
        try (Scene scene = new Scene(context)) {
            StaticMesh original = scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES, new int[2],
                    new Material[]{Material.wood()});

            try (SerializedObject serialized = new SerializedObject(context)) {
                original.save(serialized);
                assertTrue(serialized.getSize() > 0, "serialized mesh must not be empty");

                StaticMesh restored = StaticMesh.load(scene, serialized);
                assertTrue(restored.isOpen());
                restored.addTo(scene);
                scene.commit();
                restored.close();
            }

            original.close();
        }
    }

    @Test
    void staticMeshSetMaterial() {
        try (Scene scene = new Scene(context)) {
            StaticMesh mesh = scene.createStaticMesh(QUAD_VERTICES, QUAD_TRIANGLES, new int[2],
                    new Material[]{Material.concrete()});

            mesh.setMaterial(scene, Material.metal(), 0);
            scene.commit();

            mesh.close();
        }
    }
}
