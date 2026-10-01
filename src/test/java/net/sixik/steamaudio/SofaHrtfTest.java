package net.sixik.steamaudio;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты загрузки HRTF из SOFA-файлов (CIPIC 124 из дистрибутива Steam
 * Audio): загрузка из файла и из памяти, рендеринг через такой HRTF,
 * отбраковка битых данных.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SofaHrtfTest {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;

    private Path sofaFile;
    private Path tempDir;

    @BeforeAll
    void extractSofaFile() throws IOException {
        tempDir = Files.createTempDirectory("steamaudio-sofa");
        sofaFile = tempDir.resolve("cipic_124.sofa");
        try (InputStream in = SofaHrtfTest.class.getResourceAsStream("/hrtf/cipic_124.sofa")) {
            assertNotNull(in, "test SOFA asset must be on the classpath");
            Files.copy(in, sofaFile);
        }
    }

    @AfterAll
    void cleanup() throws IOException {
        if (sofaFile != null) {
            Files.deleteIfExists(sofaFile);
        }
        if (tempDir != null) {
            Files.deleteIfExists(tempDir);
        }
    }

    private static float energy(float[] samples) {
        float result = 0.0f;
        for (float sample : samples) {
            result += sample * sample;
        }
        return result;
    }

    private static float[] sine(int numSamples) {
        float[] input = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return input;
    }

    @Test
    void loadFromFileAndRender() {
        try (Context context = new Context();
             HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE,
                     sofaFile.toAbsolutePath().toString(), 1.0f, HRTF.NORM_TYPE_NONE);
             BinauralEffect effect = new BinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf);
             AudioBuffer in = new AudioBuffer(context, 1, FRAME_SIZE);
             AudioBuffer out = new AudioBuffer(context, 2, FRAME_SIZE)) {

            assertTrue(hrtf.isOpen());

            in.deinterleaveFrom(sine(FRAME_SIZE));
            effect.apply(in, out, new Vector3(1, 0, 0),
                    BinauralEffect.INTERPOLATION_NEAREST, 1.0f);

            float[] rendered = new float[2 * FRAME_SIZE];
            out.interleaveTo(rendered);

            float leftEnergy = 0.0f;
            float rightEnergy = 0.0f;
            for (int i = 0; i < rendered.length; i += 2) {
                leftEnergy += rendered[i] * rendered[i];
                rightEnergy += rendered[i + 1] * rendered[i + 1];
            }
            assertTrue(leftEnergy > 0.0f && rightEnergy > 0.0f, "sofa HRTF must render audio");
            assertTrue(rightEnergy > leftEnergy, "source to the right must be louder in the right ear");
        }
    }

    @Test
    void loadFromMemory() {
        try (Context context = new Context()) {
            byte[] sofaData = assertDoesNotThrow(() -> Files.readAllBytes(sofaFile));

            try (HRTF hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE,
                    sofaData, 1.0f, HRTF.NORM_TYPE_NONE)) {
                assertTrue(hrtf.isOpen());
            }
        }
    }

    @Test
    void corruptDataRejected() {
        try (Context context = new Context()) {
            assertThrows(SteamAudioException.class, () -> new HRTF(context, SAMPLING_RATE, FRAME_SIZE,
                    new byte[]{1, 2, 3, 4}, 1.0f, HRTF.NORM_TYPE_NONE));
        }
    }

    @Test
    void missingFileRejected() {
        try (Context context = new Context()) {
            Path nonExistent = tempDir.resolve("no_such_file.sofa");
            assertFalse(Files.exists(nonExistent));
            assertThrows(SteamAudioException.class, () -> new HRTF(context, SAMPLING_RATE, FRAME_SIZE,
                    nonExistent.toString(), 1.0f, HRTF.NORM_TYPE_NONE));
        }
    }
}
