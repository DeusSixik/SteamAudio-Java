# SteamAudio-Java

Java bindings (JNI) for [Steam Audio](https://valvesoftware.github.io/steam-audio/)
4.8.1 — Valve's physics-based spatial audio SDK. Targets **Java 17+**, ships the
native libraries inside the jar, and covers **~98% of the C API** (205 of 210
functions).

```java
try (Context context = new Context();                     // iplContextCreate
     Scene scene = new Scene(context)) {                  // iplSceneCreate

    scene.createStaticMesh(vertices, triangles,
            materialIndices, new Material[]{Material.concrete()});
    scene.commit();                                       // iplSceneCommit

    try (Simulator simulator = LowSpecSimulator.create(context, 48000, 1024);
         Source source = new Source(simulator, Simulator.FLAGS_DIRECT);
         HRTF hrtf = new HRTF(context, 48000, 1024);
         BinauralEffect hrtfRender = new BinauralEffect(context, 48000, 1024, hrtf)) {

        simulator.setScene(scene);
        simulator.commit();
        source.add();
        simulator.commit();
        source.setDirectInputs(Source.DIRECT_SIM_DISTANCE_ATTENUATION
                        | Source.DIRECT_SIM_OCCLUSION,
                new Vector3(3, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
                Source.OCCLUSION_RAYCAST, 0.5f, 8, 1);

        // Each frame: simulate once, render per source.
        simulator.runDirect();
        source.getDirectOutputsInto(directOutputs);       // float[9]
        binaural.apply(in, out, direction, ...);           // zero-alloc hot path
    }
}
```

## How it works

```
Java (net.sixik.steamaudio.*)          JNI (natives/src/*.cpp)         Steam Audio
┌─────────────────────────┐   JNIEnv   ┌──────────────────────┐   ┌────────────┐
│ Context, Scene, ...     │ ─────────► │ SteamAudioJni.dll    │──►│ phonon.dll │
│ long peer + AutoCloseable│  jlong /  │ extern "C" functions │   │ (Valve)    │
│ float[] / ByteBuffer     │  float[]  │ struct marshalling   │   └────────────┘
└─────────────────────────┘            └──────────────────────┘
```

- **Opaque handles stay native.** Every C object (`IPLContext`, `IPLScene`, ...)
  is held by a Java wrapper as a `long peer` — never as a `JObject`. Wrappers
  are `AutoCloseable`; `close()` maps to the corresponding `ipl*Release`, and
  double-close is always safe.
- **Structs are flattened at the JNI boundary.** `IPLVector3` becomes three
  `float` arguments or a mutable `Vector3`; the giant `IPLSimulationInputs`
  is assembled inside the native call (with documented defaults) behind the
  focused methods `Source.setDirectInputs / setReflectionsInputs /
  setPathingInputs`. No `GetFieldID` anywhere near the hot path.
- **Bulk data moves without copies.** Geometry (vertices, triangles,
  materials) is packed into direct `ByteBuffer`s and read on the native side
  via `GetDirectBufferAddress`. Audio arrays use
  `GetPrimitiveArrayCritical` (JMH-verified: `alloc.rate.norm ≈ 0 B/op`).
- **Errors become exceptions.** Every `IPLerror` from Steam Audio is thrown
  as `SteamAudioException` (message includes the status); misuse of closed
  wrappers throws `IllegalStateException`; invalid indices throw
  `IllegalArgumentException`. In C the same mistakes are segfaults or silent
  error codes.
- **Natives are loaded automatically** (`core.SteamAudio`), in strict order
  `phonon` → `SteamAudioJni` (the JNI library links against phonon):
  1. `-Dsteamaudio.natives=<dir>` — explicit override;
  2. well-known build output directories (`natives/build/bin/Release`, ...)
     for development;
  3. extraction from the jar: `net/sixik/steamaudio/natives/<platform>/` is
     unpacked into `%TEMP%/steamaudio-java-natives/<platform>-<sha256-16>`
     — the content hash prevents stale-version conflicts and lets several
     processes share the cache without fighting over locked DLLs.

### Build integration

`gradlew jar` triggers the whole chain automatically:
`configureNatives` (CMake configure) → `buildNatives` (Release build) →
`copyNatives` (DLLs into jar resources) → `processResources`. Running from
the IDE works too, because the loader falls back to the CMake output
directory. CMake requires a JDK (`find_package(JNI)`) and MSVC on Windows.

## Package layout

| Package | Contents |
|---|---|
| `core` | `SteamAudio` (loader), `Context`, `Vector3`, `SerializedObject`, `SteamAudioException` |
| `audio` | `AudioBuffer` (+ mix/downmix/ambisonics conversion), `HRTF` (built-in + SOFA), `ImpulseResponse`, `Reconstructor`, `SpeakerLayout`, `AmbisonicsType` |
| `geometry` | `Scene` (+ OBJ export, Embree/RadeonRays backends), `StaticMesh`, `InstancedMesh`, `Material` (+ presets) |
| `simulation` | `Simulator`, `Source`, `ProbeArray`, `ProbeBatch`, `PathBaker`, `ReflectionsBaker`, `EnergyField`, `LowSpecSimulator` |
| `effects` | `BinauralEffect`, `DirectEffect`, `PanningEffect`, `VirtualSurroundEffect`, `ReflectionEffect`, `ReflectionMixer`, `PathEffect`, `Ambisonics{Encode,Rotation,Panning,Binaural,Decode}Effect` |
| `gpu` | `EmbreeDevice`, `OpenCLDeviceList` (+ `DeviceDesc`), `OpenCLDevice`, `RadeonRaysDevice`, `TrueAudioNextDevice` |

## Differences from the C API

This is **"Steam Audio for Java", not a transliteration of `phonon.h`**. The
function coverage is near-complete, but the surface is deliberately
idiomatic.

### Mapped 1:1

- All enum **values** (`SIMD_LEVEL_*`, `SpeakerLayout.*`, `DATA_TYPE_*`,
  occlusion types, ...) use the same integers as `phonon.h`.
- The simulation call sequence is identical:
  `setScene → setSharedInputs → commit → runDirect → getOutputs`.
- Nearly every `ipl*` function has a Java counterpart with the same
  semantics (see `ROADMAP.md` for the exact coverage).

### Deliberately different

| C API | This library |
|---|---|
| `iplContextCreate(&settings, &ctx)` / `iplContextRelease(&ctx)` | `new Context(...)` / `AutoCloseable.close()` |
| `IPLerror` return codes | `SteamAudioException` (message carries the status) |
| Structs by value / by pointer (`IPLVector3`, `IPLCoordinateSpace3`) | Mutable `Vector3` or flattened `float` arguments (allocation-free hot path overloads) |
| One giant `IPLSimulationInputs` struct | Three focused methods with sane native-side defaults: `setDirectInputs`, `setReflectionsInputs`, `setPathingInputs` |
| Raw `IPLfloat32**` audio data | `float[]` via critical sections; direct `ByteBuffer` for geometry |
| Invalid handles / indices = UB or silent codes | Java-side validation: `isOpen()`, `IllegalStateException`, `IllegalArgumentException` |
| `iplAudioBuffer*Interleave(ctx, buf, ptr)` | `buffer.interleaveTo(float[])` / `buffer.deinterleaveFrom(float[])` |

### Hardcoded / omitted

- **No user callbacks are exposed**: log function, allocator/deallocator,
  distance-attenuation / air-absorption / directivity / deviation callbacks
  (their default models are used; `Source` directivity is omnidirectional),
  bake progress callbacks (a no-op is always passed — required, see below),
  pathing visualization, and custom ray tracer callbacks (so
  `SCENE_TYPE_CUSTOM` is not usable yet).
- `Simulator` tuning knobs are fixed in the native constructor:
  `maxNumOcclusionSamples=256`, `maxNumRays=4096`, `numDiffuseSamples=32`,
  `maxDuration=2s`, `maxOrder=1`, `maxNumSources=8`, `numThreads=1`,
  `rayBatchSize=8`, `numVisSamples=8`.
- `PathEffect` renders unrotated Ambisonics only (`spatialize=false`);
  combine with `AmbisonicsRotationEffect` + `AmbisonicsBinauralEffect` for
  head tracking.
- `iplOpenCLDeviceCreateFromExisting` (caller-owned `cl_command_queue` pairs)
  is not wrapped.
- `AudioBuffer` memory is always allocated by Steam Audio; wrapping
  caller-owned sample data is not exposed.
- `Retain` variants are not exposed (Java wrappers own their handles).

### Added beyond the C API

- Native loader with jar packaging and a content-hashed extraction cache.
- `-Dsteamaudio.simdLevel` and `Context.SIMD_LEVEL_AUTO` (default **AVX** —
  the safe minimum for pre-Haswell CPUs such as the i5-3470; the official
  `phonon.dll` additionally caps the level at what the CPU supports via IPP).
- `simulation.LowSpecSimulator` — a preset for weak 4-core CPUs: parametric
  reverb (≈10x cheaper than convolution), 64 rays, 2 bounces, order-1
  ambisonics.
- `geometry.Material` presets from the Steam Audio documentation
  (`concrete()`, `brick()`, `metal()`, ...).
- Java-side validation everywhere; JNI signature mismatches cannot be caught
  by the JVM, so the tests deliberately cross every boundary
  (`ROADMAP.md` → "Conventions for new bindings").

## Low-end CPU support (i5-3470 class)

```java
Context context = new Context();                       // SIMD auto → AVX
Simulator simulator = LowSpecSimulator.create(context, 48000, 1024);
// sources: Source.OCCLUSION_RAYCAST; reflections rendering:
// ReflectionEffect.TYPE_PARAMETRIC (≈10x cheaper than convolution)
```

The per-frame hot path (direct + binaural, 1024-sample frames) measures
~3 µs/source on a modern desktop and comfortably fits a ~21 ms frame budget
even multiplied by 3–4x for older hardware. Avoid convolution reverb on many
sources; use `ReflectionMixer` on a separate thread if it is required.

## GPU backends

| Backend | Class | Requires |
|---|---|---|
| Embree ray tracing | `gpu.EmbreeDevice` + `Scene(context, SCENE_TYPE_EMBREE, device)` | `embree.dll` next to `phonon.dll` |
| OpenCL | `gpu.OpenCLDeviceList` → `OpenCLDevice` | an OpenCL runtime |
| Radeon Rays (GPU ray tracing) | `gpu.RadeonRaysDevice` | AMD GPU + OpenCL |
| TrueAudio Next (GPU convolution) | `gpu.TrueAudioNextDevice` | AMD GPU + OpenCL |

Hardware caveats (also documented in `gpu.GpuDevicesTest`):

- Creation failures are clean (`SteamAudioException`) when a runtime DLL is
  missing — except on non-AMD GPUs, where creating Radeon Rays / TrueAudio
  Next devices **may crash inside phonon**; the tests only exercise them on
  AMD hardware.
- Creating an EMBREE-type scene with a NULL device crashes inside phonon —
  always pass a device.

## Building, testing, benchmarking

```bat
gradlew test          :: builds natives via CMake, then runs the JUnit suite
gradlew jar           :: same + packages natives into the jar
gradlew jmh           :: JMH benchmarks (src/jmh), results in build/reports/jmh
```

Requirements: JDK 17+ (21 tested), CMake 3.20+, MSVC on Windows. Steam Audio
headers/libs live in `natives/steamaudio/` (Apache-2.0, © Valve Corporation).

## Known upstream issues

- [steam-audio#523](https://github.com/ValveSoftware/steam-audio/issues/523) —
  `iplPathBakerBake` (and the reflections baker) invoke the progress callback
  unconditionally; a NULL callback segfaults. Our native layer always passes
  a no-op callback.
- [steam-audio#578](https://github.com/ValveSoftware/steam-audio/issues/578) —
  AVX2 alignment bug in `ArrayMath::multiplyAccumulate` (4.8.1); not hit by
  our tests, but worth knowing.
- `AmbisonicsEncodeEffect` and `PanningEffect` crossfade from the previous
  frame's direction: the first `apply` after creation/reset is a warm-up
  pass (see `P1EffectsTest`).

See [ROADMAP.md](ROADMAP.md) for the full API coverage table, versioning of
milestones, and conventions used for new bindings.
