package net.sixik.steamaudio;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Загрузчик нативных библиотек Steam Audio.
 * <p>
 * Перед первым использованием любого класса с JNI-методами необходимо вызвать
 * {@link #load()}. Библиотеки загружаются в строгом порядке: сначала
 * {@code phonon} (основная C-библиотека Steam Audio), затем
 * {@code SteamAudioJni} (JNI-обвязка), так как обвязка линкуется с phonon
 * во время загрузки.
 * <p>
 * Порядок поиска нативов:
 * <ol>
 *   <li>каталог из системного свойства {@code steamaudio.natives};</li>
 *   <li>стандартные каталоги сборки относительно рабочего каталога
 *       (режим разработки);</li>
 *   <li>извлечение из jar: ресурсы
 *       {@code net/sixik/steamaudio/natives/<платформа>} копируются в
 *       подкаталог системного временного каталога, имя которого включает
 *       хеш содержимого DLL — это гарантирует отсутствие конфликтов между
 *       процессами и устаревших версий файлов.</li>
 * </ol>
 */
public final class SteamAudio {

    /** Путь к ресурсам нативов внутри jar. */
    private static final String NATIVES_RESOURCE_PREFIX = "net/sixik/steamaudio/natives/";

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private SteamAudio() {
    }

    /**
     * Загружает нативные библиотеки Steam Audio, если они ещё не загружены.
     * Повторные вызовы не приводят к повторной загрузке.
     *
     * @throws UnsatisfiedLinkError если библиотеки не найдены либо несовместимы
     *                              с текущей платформой
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
     * Загружает нативные библиотеки из указанного каталога: сначала phonon,
     * затем JNI-обвязку.
     *
     * @param dir каталог с нативными библиотеками
     */
    private static void loadFrom(File dir) {
        System.load(new File(dir, libraryName("phonon")).getAbsolutePath());
        System.load(new File(dir, libraryName("SteamAudioJni")).getAbsolutePath());
    }

    /**
     * Определяет каталог нативных библиотек: системное свойство
     * {@code steamaudio.natives}, затем стандартные каталоги сборки, затем
     * извлечение из ресурсов jar.
     *
     * @return абсолютный путь к каталогу с нативными библиотеками
     * @throws UnsatisfiedLinkError если каталог определить не удалось
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
     * Возвращает список стандартных каталогов сборки, проверяемых в
     * режиме разработки.
     *
     * @return относительные пути кандидатов
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
     * Извлекает нативные библиотеки из ресурсов jar во временный каталог и
     * возвращает его. Имя каталога включает SHA-256 хеш содержимого
     * библиотек: повторная загрузка той же версии переиспользует кэш без
     * перезаписи (важно, если файлы залочены другим процессом), а новая
     * версия сборки попадает в отдельный каталог.
     *
     * @return каталог с извлеченными библиотеками
     * @throws UnsatisfiedLinkError если ресурсы не найдены или платформа
     *                              не поддерживается
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
     * Читает ресурс нативной библиотеки из classpath полностью в память.
     *
     * @param fileName имя файла библиотеки в ресурсах
     * @return содержимое ресурса
     * @throws UnsatisfiedLinkError если ресурс не найден
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
     * Вычисляет SHA-256 хеш конкатенации содержимого библиотек.
     *
     * @param libraries массивы байтов библиотек
     * @return строка хеша (первые 16 hex-символов)
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
     * Возвращает каталог платформы внутри ресурсов нативов по текущим
     * {@code os.name} и {@code os.arch}.
     *
     * @return имя каталога платформы
     * @throws UnsatisfiedLinkError если платформа не поддерживается
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
     * Возвращает имя файла динамической библиотеки для текущей платформы.
     *
     * @param base базовое имя библиотеки
     * @return имя файла с расширением, зависящим от ОС
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
