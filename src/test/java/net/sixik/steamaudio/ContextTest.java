package net.sixik.steamaudio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke-тест жизненного цикла контекста Steam Audio: загрузка нативных
 * библиотек, создание контекста, освобождение.
 */
class ContextTest {

    @Test
    void contextLifecycle() {
        assertDoesNotThrow(SteamAudio::load);

        Context context = new Context();
        assertTrue(context.isOpen());
        context.close();
        assertFalse(context.isOpen());
    }

    @Test
    void doubleCloseIsSafe() {
        Context context = new Context();
        context.close();
        assertDoesNotThrow(context::close);
        assertFalse(context.isOpen());
    }
}
