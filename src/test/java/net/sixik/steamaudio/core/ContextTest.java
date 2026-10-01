package net.sixik.steamaudio.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the Steam Audio context lifecycle: native library loading,
 * context creation, release.
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
