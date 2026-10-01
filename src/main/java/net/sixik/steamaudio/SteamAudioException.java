package net.sixik.steamaudio;

/**
 * Исключение, бросаемое JNI-слоем при получении кода ошибки
 * {@code IPLerror} из Steam Audio.
 */
public class SteamAudioException extends RuntimeException {

    /**
     * Создает исключение с описанием ошибки.
     *
     * @param message текстовое описание ошибки Steam Audio
     */
    public SteamAudioException(String message) {
        super(message);
    }
}
