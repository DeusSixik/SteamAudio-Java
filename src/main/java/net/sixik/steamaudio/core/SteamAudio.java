package net.sixik.steamaudio.core;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loader for the Steam Audio native libraries.
 * <p>
 * Before first use of any class containing JNI methods, {@link #load()}
 * must be called. The libraries are loaded in a strict order: first
 * {@code phonon} (the main Steam Audio C library), then
 * {@code SteamAudioJni} (the JNI wrapper), because the wrapper links
 * against phonon at load time.
 * <p>
 * Native library lookup order:
 * <ol>
 *   <li>the directory from the {@code steamaudio.natives} system property;</li>
 *   <li>standard build directories relative to the working directory
 *       (development mode);</li>
 *   <li>extraction from the jar: the
 *       {@code net/sixik/steamaudio/natives/<platform>} resources are copied
 *       into a subdirectory of the system temporary directory whose name
 *       includes a hash of the DLL contents — this guarantees no conflicts
 *       between processes and no stale file versions.</li>
 * </ol>
 */
public final class SteamAudio {

    /** Path to the native library resources inside the jar. */
    private static final String NATIVES_RESOURCE_PREFIX = "net/sixik/steamaudio/natives/";

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private SteamAudio() {
    }

    /**
     * Loads the Steam Audio native libraries if they are not loaded yet.
     * Repeated calls do not trigger a reload.
     *
     * @throws UnsatisfiedLinkError if the libraries are not found or are
     *                              incompatible with the current platform
     */
    public static void load() {
        if (LOADED.get()) {
            return;
        }
        if (!LOADED.compareAndSet(false, true)) {
            return;
        }
        try {
            loadFrom(resolveNativesDir());
        } catch (RuntimeException e) {
            LOADED.set(false);
            throw e;
        }
    }

    /**
     * Loads the native libraries from the given directory: phonon first,
     * then the JNI wrapper.
     *
     * @param dir directory containing the native libraries
     */
    private static void loadFrom(File dir) {
        System.load(new File(dir, libraryName("phonon")).getAbsolutePath());
        System.load(new File(dir, libraryName("SteamAudioJni")).getAbsolutePath());
    }

    /**
     * Resolves the native library directory: the {@code steamaudio.natives}
     * system property first, then the standard build directories, then
     * extraction from the jar resources.
     *
     * @return absolute path to the directory containing the native libraries
     * @throws UnsatisfiedLinkError if the directory could not be resolved
     */
    private static File resolveNativesDir() {
        String property = System.getProperty("steamaudio.natives");
        if (property != null && !property.isEmpty()) {
            return new File(property);
        }
        for (String candidate : buildDirCandidates()) {
            File dir = new File(candidate);
            if (new File(dir, libraryName("phonon")).isFile()) {
                return dir;
            }
        }
        return extractNatives();
    }

    /**
     * Returns the list of standard build directories checked in
     * development mode.
     *
     * @return relative candidate paths
     */
    private static String[] buildDirCandidates() {
        String separator = File.separator;
        return new String[]{
                "natives" + separator + "build" + separator + "bin" + separator + "Release",
                "natives" + separator + "build" + separator + "bin" + separator + "Debug",
                "natives" + separator + "build" + separator + "Release",
                "natives" + separator + "build" + separator + "Debug",
                "natives" + separator + "build" + separator + "bin",
                "natives" + separator + "cmake-build-debug" + separator + "bin",
        };
    }

    /**
     * Extracts the native libraries from the jar resources into a temporary
     * directory and returns it. The directory name includes an SHA-256 hash
     * of the library contents: reloading the same version reuses the cache
     * without overwriting (important when the files are locked by another
     * process), while a new build version gets a separate directory.
     *
     * @return directory with the extracted libraries
     * @throws UnsatisfiedLinkError if the resources are not found or the
     *                              platform is not supported
     */
    static File extractNatives() {
        String platform = platformDir();
        byte[][] libraries = new byte[][]{
                readResource(libraryName("phonon")),
                readResource(libraryName("SteamAudioJni")),
        };

        File cacheDir = new File(new File(System.getProperty("java.io.tmpdir"),
                "steamaudio-java-natives"), platform + "-" + contentHash(libraries));
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            throw new UnsatisfiedLinkError("Failed to create natives cache dir: " + cacheDir);
        }

        String[] names = {libraryName("phonon"), libraryName("SteamAudioJni")};
        for (int i = 0; i < libraries.length; i++) {
            Path target = new File(cacheDir, names[i]).toPath();
            try {
                Files.write(target, libraries[i]);
            } catch (Exception e) {
                throw new UnsatisfiedLinkError("Failed to extract native library " + names[i]
                        + ": " + e.getMessage());
            }
        }
        return cacheDir;
    }

    /**
     * Reads a native library resource from the classpath fully into memory.
     *
     * @param fileName library file name within the resources
     * @return resource contents
     * @throws UnsatisfiedLinkError if the resource is not found
     */
    private static byte[] readResource(String fileName) {
        String path = NATIVES_RESOURCE_PREFIX + platformDir() + "/" + fileName;
        try (InputStream in = SteamAudio.class.getResourceAsStream("/" + path)) {
            if (in == null) {
                throw new UnsatisfiedLinkError(
                        "Native library resource not found on classpath: " + path
                                + "; is the natives jar included in dependencies?");
            }
            return in.readAllBytes();
        } catch (UnsatisfiedLinkError e) {
            throw e;
        } catch (Exception e) {
            throw new UnsatisfiedLinkError("Failed to read native library resource " + path
                    + ": " + e.getMessage());
        }
    }

    /**
     * Computes the SHA-256 hash of the concatenation of the library contents.
     *
     * @param libraries library byte arrays
     * @return hash string (first 16 hex characters)
     */
    private static String contentHash(byte[][] libraries) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsatisfiedLinkError("SHA-256 not available: " + e.getMessage());
        }
        for (byte[] library : libraries) {
            digest.update(library);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.substring(0, 16);
    }

    /**
     * Returns the platform directory inside the native resources based on
     * the current {@code os.name} and {@code os.arch}.
     *
     * @return platform directory name
     * @throws UnsatisfiedLinkError if the platform is not supported
     */
    static String platformDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean is64 = arch.contains("64") || arch.contains("aarch64");
        if (os.contains("win")) {
            return is64 ? "windows-x64" : "windows-x86";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "osx";
        }
        if (os.contains("nux") || os.contains("nix")) {
            return is64 ? "linux-x64" : "linux-x86";
        }
        throw new UnsatisfiedLinkError("Unsupported platform for Steam Audio: " + os + " / " + arch);
    }

    /**
     * Returns the dynamic library file name for the current platform.
     *
     * @param base library base name
     * @return file name with the OS-dependent extension
     */
    private static String libraryName(String base) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return base + ".dll";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "lib" + base + ".dylib";
        }
        return "lib" + base + ".so";
    }
}
