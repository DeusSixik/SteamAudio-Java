package net.sixik.steamaudio.core;

/**
 * Exception thrown by the JNI layer when an {@code IPLerror} error code
 * is received from Steam Audio.
 */
public class SteamAudioException extends RuntimeException {

    /**
     * Creates the exception with an error description.
     *
     * @param message textual description of the Steam Audio error
     */
    public SteamAudioException(String message) {
        super(message);
    }
}
