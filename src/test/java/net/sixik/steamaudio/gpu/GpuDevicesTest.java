package net.sixik.steamaudio.gpu;

import org.junit.jupiter.api.Test;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;
import net.sixik.steamaudio.geometry.Scene;
import net.sixik.steamaudio.simulation.Simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the GPU backends: {@link EmbreeDevice}, {@link OpenCLDeviceList},
 * {@link OpenCLDevice}, {@link RadeonRaysDevice} and
 * {@link TrueAudioNextDevice}.
 * <p>
 * These backends depend on runtime libraries (embree.dll, an OpenCL runtime,
 * AMD GPUUtilities/TrueAudioNext). Tests are written to be robust on any
 * machine: when a backend is unavailable, creation must fail cleanly with a
 * {@link SteamAudioException} (never a JVM crash); when available, the
 * device lifecycle is exercised.
 */
class GpuDevicesTest {

    @Test
    void embreeDeviceCreationIsClean() {
        try (Context context = new Context()) {
            try (EmbreeDevice device = new EmbreeDevice(context)) {
                // Embree installed: exercise the device lifecycle with a scene.
                assertTrue(device.isOpen());
                try (Scene scene = new Scene(context, Scene.SCENE_TYPE_EMBREE, device)) {
                    assertTrue(scene.isOpen());
                    scene.close();
                }
            } catch (SteamAudioException e) {
                // Embree not installed: creation must fail cleanly.
            }
        }
    }

    @Test
    void openclDeviceEnumerationIsClean() {
        try (Context context = new Context()) {
            int totalDevices = 0;
            try (OpenCLDeviceList list = new OpenCLDeviceList(context,
                    OpenCLDeviceList.DEVICE_TYPE_ANY, 0, 0.0f, false)) {

                totalDevices = list.getNumDevices();
                for (int i = 0; i < totalDevices; i++) {
                    OpenCLDeviceList.DeviceDesc desc = list.getDeviceDesc(i);
                    assertTrue(desc.deviceName != null && !desc.deviceName.isEmpty(),
                            "device name must be populated");
                }

                if (totalDevices > 0) {
                    try (OpenCLDevice device = list.createDevice(0)) {
                        assertTrue(device.isOpen());
                    }
                }
            } catch (SteamAudioException e) {
                // No OpenCL runtime: creation must fail cleanly.
            }
            assertTrue(totalDevices >= 0);
        }
    }

    @Test
    void radeonRaysAndTanFailCleanlyWithoutSupport() {
        try (Context context = new Context()) {
            try (OpenCLDeviceList list = new OpenCLDeviceList(context,
                    OpenCLDeviceList.DEVICE_TYPE_GPU, 0, 0.0f, false)) {
                if (list.getNumDevices() == 0) {
                    return; // No GPU OpenCL device: nothing to test.
                }

                // Radeon Rays and TrueAudio Next only support AMD GPUs; on
                // other vendors phonon may crash instead of returning an
                // error, so only exercise them on AMD hardware.
                boolean isAmd = false;
                for (int i = 0; i < list.getNumDevices(); i++) {
                    String vendor = list.getDeviceDesc(i).deviceVendor;
                    if (vendor != null && (vendor.contains("AMD") || vendor.contains("Advanced Micro"))) {
                        isAmd = true;
                        break;
                    }
                }
                if (!isAmd) {
                    return;
                }

                try (OpenCLDevice device = list.createDevice(0)) {
                    try (RadeonRaysDevice ignored = new RadeonRaysDevice(device)) {
                        // Radeon Rays available.
                    } catch (SteamAudioException expected) {
                        // Unsupported configuration.
                    }
                    try (TrueAudioNextDevice ignored = new TrueAudioNextDevice(device, 1024, 4096, 1, 8)) {
                        // TrueAudio Next available.
                    } catch (SteamAudioException expected) {
                        // Unsupported configuration.
                    }
                }
            } catch (SteamAudioException e) {
                // No OpenCL runtime at all.
            }
        }
    }

    @Test
    void simulatorWithSceneTypeWiring() {
        try (Context context = new Context()) {
            // The default scene type must still work through the full
            // constructor with all device arguments null.
            try (Simulator simulator = new Simulator(context, Simulator.FLAGS_DIRECT,
                    Scene.SCENE_TYPE_DEFAULT, 0 /* convolution reflections */,
                    48000, 1024, null, null, null)) {
                assertTrue(simulator.isOpen());
            }
        }
    }

    // Note: creating an EMBREE-type scene with a NULL Embree device crashes
    // inside phonon (the API contract requires a device for that scene
    // type), so there is deliberately no test for that combination.
    // Use embreeDeviceCreationIsClean() which wires a real device when
    // embree.dll is available.

    @Test
    void openclDeviceTypesMatchPhonon() {
        assertEquals(0, OpenCLDeviceList.DEVICE_TYPE_ANY);
        assertEquals(1, OpenCLDeviceList.DEVICE_TYPE_CPU);
        assertEquals(2, OpenCLDeviceList.DEVICE_TYPE_GPU);
    }
}
