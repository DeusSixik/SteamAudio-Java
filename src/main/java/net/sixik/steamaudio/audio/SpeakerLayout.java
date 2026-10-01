package net.sixik.steamaudio.audio;

/**
 * Supported speaker layouts, mirroring {@code IPLSpeakerLayoutType}.
 * <p>
 * Custom layouts (an explicit list of speaker directions) are not exposed
 * yet; standard layouts cover the common playback setups.
 */
public final class SpeakerLayout {

    /** Mono. */
    public static final int MONO = 0;

    /** Stereo (left, right). */
    public static final int STEREO = 1;

    /** Quadraphonic: front left, front right, rear left, rear right. */
    public static final int QUADRAPHONIC = 2;

    /** 5.1: front left, front right, front center, LFE, rear left, rear right. */
    public static final int SURROUND_5_1 = 3;

    /** 7.1: front left, front right, front center, LFE, rear left, rear right, side left, side right. */
    public static final int SURROUND_7_1 = 4;

    /** User-defined speaker layout (not exposed yet). */
    public static final int CUSTOM = 5;

    private SpeakerLayout() {
    }

    /**
     * Returns the number of speakers (audio channels) for a standard layout.
     *
     * @param layoutType one of the {@code MONO..SURROUND_7_1} constants
     * @return channel count for the layout
     * @throws IllegalArgumentException for {@link #CUSTOM} or unknown values
     */
    public static int numSpeakers(int layoutType) {
        switch (layoutType) {
            case MONO: return 1;
            case STEREO: return 2;
            case QUADRAPHONIC: return 4;
            case SURROUND_5_1: return 6;
            case SURROUND_7_1: return 8;
            default:
                throw new IllegalArgumentException("custom or unknown speaker layout: " + layoutType);
        }
    }
}
