package net.sixik.steamaudio;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import net.sixik.steamaudio.audio.AudioBuffer;
import net.sixik.steamaudio.audio.HRTF;
import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudio;
import net.sixik.steamaudio.core.Vector3;
import net.sixik.steamaudio.effects.BinauralEffect;
import net.sixik.steamaudio.effects.DirectEffect;


/**
 * JMH benchmarks for the hot path of the Steam Audio JNI binding.
 * <p>
 * Benchmarked: binaural rendering (float- and Vector3-based overloads),
 * the direct effect, interleaving/deinterleaving of audio buffers, and
 * relative direction calculation in zero-alloc and allocating versions.
 * The gc profile verifies that all paths remain allocation-free
 * ({@code alloc.rate.norm} should be ~0 B/op).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SteamAudioBenchmark {

    private static final int SAMPLING_RATE = 48000;
    private static final int FRAME_SIZE = 1024;

    private Context context;
    private HRTF hrtf;
    private BinauralEffect binaural;
    private DirectEffect direct;

    private AudioBuffer inMono;
    private AudioBuffer outStereo;
    private AudioBuffer outMono;

    /** Preallocated input data and output arrays — the benchmark itself
     * must not allocate anything. */
    private Vector3 direction;
    private Vector3 sourcePosition;
    private Vector3 listenerPosition;
    private Vector3 listenerAhead;
    private Vector3 listenerUp;

    private float[] out3;
    private float[] directOutputs;
    private float[] interleavedStereo;
    private float[] interleavedMono;

    @Setup(Level.Trial)
    public void setup() {
        SteamAudio.load();

        context = new Context(Context.SIMD_LEVEL_AVX2, 0);
        hrtf = new HRTF(context, SAMPLING_RATE, FRAME_SIZE);
        binaural = new BinauralEffect(context, SAMPLING_RATE, FRAME_SIZE, hrtf);
        direct = new DirectEffect(context, SAMPLING_RATE, FRAME_SIZE, 1);

        inMono = new AudioBuffer(context, 1, FRAME_SIZE);
        outStereo = new AudioBuffer(context, 2, FRAME_SIZE);
        outMono = new AudioBuffer(context, 1, FRAME_SIZE);

        inMono.deinterleaveFrom(sine());

        direction = new Vector3(1.0f, 0.0f, 0.0f);
        sourcePosition = new Vector3(0.0f, 0.0f, -5.0f);
        listenerPosition = new Vector3(0.0f, 0.0f, 0.0f);
        listenerAhead = new Vector3(0.0f, 0.0f, -1.0f);
        listenerUp = new Vector3(0.0f, 1.0f, 0.0f);

        out3 = new float[3];

        // Plausible direct simulation results: att 1/3, air absorption/directivity/
        // occlusion/transmission — neutral units.
        directOutputs = new float[]{1.0f / 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

        interleavedStereo = new float[2 * FRAME_SIZE];
        interleavedMono = new float[FRAME_SIZE];
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        inMono.close();
        outStereo.close();
        outMono.close();
        direct.close();
        binaural.close();
        hrtf.close();
        context.close();
    }

    private static float[] sine() {
        float[] input = new float[FRAME_SIZE];
        for (int i = 0; i < FRAME_SIZE; i++) {
            input[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / SAMPLING_RATE);
        }
        return input;
    }

    /**
     * Binaural rendering, primitive-based overload.
     */
    @Benchmark
    public int binauralApply() {
        return binaural.apply(inMono, outStereo, 1.0f, 0.0f, 0.0f,
                BinauralEffect.INTERPOLATION_NEAREST, 1.0f);
    }

    /**
     * Binaural rendering, mutable Vector3 overload.
     */
    @Benchmark
    public int binauralApplyVector3() {
        return binaural.apply(inMono, outStereo, direction,
                BinauralEffect.INTERPOLATION_NEAREST, 1.0f);
    }

    /**
     * Direct effect with flattened parameters.
     */
    @Benchmark
    public int directEffectApply() {
        return direct.apply(inMono, outMono, directOutputs,
                DirectEffect.APPLY_DISTANCE_ATTENUATION | DirectEffect.APPLY_OCCLUSION,
                DirectEffect.TRANSMISSION_FREQ_INDEPENDENT);
    }

    /**
     * Interleaving a stereo frame from the native buffer into a Java array.
     */
    @Benchmark
    public void interleaveStereo() {
        outStereo.interleaveTo(interleavedStereo);
    }

    /**
     * Deinterleaving a mono frame from a Java array into the native buffer.
     */
    @Benchmark
    public void deinterleaveMono() {
        inMono.deinterleaveFrom(interleavedMono);
    }

    /**
     * Relative direction calculation: zero-alloc version.
     */
    @Benchmark
    public void relativeDirectionZeroAlloc() {
        context.calculateRelativeDirection(sourcePosition, listenerPosition, listenerAhead, listenerUp, out3);
    }

    /**
     * Relative direction calculation: allocating convenience version.
     * Comparing with {@link #relativeDirectionZeroAlloc()} shows the cost of
     * allocation + Vector3 construction.
     */
    @Benchmark
    public Vector3 relativeDirectionAllocating() {
        return context.calculateRelativeDirection(sourcePosition, listenerPosition, listenerAhead, listenerUp);
    }
}
