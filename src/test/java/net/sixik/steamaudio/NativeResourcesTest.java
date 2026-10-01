package net.sixik.steamaudio;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты упаковки нативных библиотек в jar-ресурсы и их извлечения.
 */
class NativeResourcesTest {

    private static String resourcePath(String fileName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String lib = os.contains("win") ? fileName + ".dll"
                : os.contains("mac") ? "lib" + fileName + ".dylib"
                : "lib" + fileName + ".so";
        return "/net/sixik/steamaudio/natives/" + SteamAudio.platformDir() + "/" + lib;
    }

    @Test
    void nativeResourcesAreBundled() {
        try (InputStream phonon = SteamAudio.class.getResourceAsStream(resourcePath("phonon"));
             InputStream jni = SteamAudio.class.getResourceAsStream(resourcePath("SteamAudioJni"))) {
            assertNotNull(phonon, "phonon native must be packaged into resources");
            assertNotNull(jni, "SteamAudioJni native must be packaged into resources");
            assertTrue(phonon.readAllBytes().length > 0, "phonon native must not be empty");
            assertTrue(jni.readAllBytes().length > 0, "SteamAudioJni native must not be empty");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void extractionProducesLoadableFiles() {
        File dir = assertDoesNotThrow(SteamAudio::extractNatives);
        assertTrue(dir.isDirectory(), "extraction dir must exist: " + dir);

        String os = System.getProperty("os.name", "").toLowerCase();
        String phononName = os.contains("win") ? "phonon.dll"
                : os.contains("mac") ? "libphonon.dylib" : "libphonon.so";
        String jniName = os.contains("win") ? "SteamAudioJni.dll"
                : os.contains("mac") ? "libSteamAudioJni.dylib" : "libSteamAudioJni.so";

        File phonon = new File(dir, phononName);
        File jni = new File(dir, jniName);
        assertTrue(phonon.isFile() && phonon.length() > 0, "extracted phonon must exist: " + phonon);
        assertTrue(jni.isFile() && jni.length() > 0, "extracted SteamAudioJni must exist: " + jni);
    }
}
