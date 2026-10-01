# SteamAudio-Java — Roadmap

JNI bindings for [Steam Audio](https://valvesoftware.github.io/steam-audio/) (C API 4.8.1),
targeting Java 17+.

## API coverage

Steam Audio 4.8.1 exposes **210 `ipl*` functions**. Covered: **~185 (88%)**. The
remainder is the GPU stack (see "Next: GPU acceleration").

### Done

| Area | Classes |
|---|---|
| Context & math | `core.Context` (+ `calculateDistanceAttenuation` / `calculateAirAbsorption` / `calculateDirectivity`), `core.Vector3`, `core.SerializedObject` |
| Audio buffers & HRTF | `audio.AudioBuffer` (+ `mix`, `downmixFrom`, `convertAmbisonicsFrom`), `audio.HRTF` (+ SOFA file/memory loading), `audio.SpeakerLayout`, `audio.AmbisonicsType`, `audio.ImpulseResponse`, `audio.Reconstructor` |
| Geometry | `geometry.Scene` (+ `saveOBJ`), `geometry.StaticMesh` (+ `save`/`load`/`setMaterial`), `geometry.InstancedMesh`, `geometry.Material` |
| Simulation | `simulation.Simulator`, `simulation.Source`, `simulation.ProbeArray` (+ `getProbe`), `simulation.ProbeBatch` (+ `getDataSize`/`removeData`/`getReverb`/`getEnergyField`), `simulation.PathBaker`, `simulation.ReflectionsBaker`, `simulation.EnergyField` |
| Effects | `effects.BinauralEffect`, `effects.DirectEffect`, `effects.PanningEffect`, `effects.VirtualSurroundEffect`, `effects.ReflectionEffect`, `effects.ReflectionMixer`, `effects.PathEffect`, `effects.Ambisonics{Encode,Rotation,Panning,Binaural,Decode}Effect` (all with full reset/getTail lifecycle) |
| Loader | Auto-build via CMake (`configureNatives`/`buildNatives`), natives bundled into the jar, extraction to a content-hashed temp dir with `-Dsteamaudio.natives` override |
| Benchmarks | `src/jmh` (`SteamAudioBenchmark`), zero-alloc verified via `-prof gc` |

### P1 — audio rendering: DONE

Completed: `PanningEffect`, `VirtualSurroundEffect` (requires an `HRTF` in 4.8.1),
`AmbisonicsDecodeEffect`, the three compute helpers on `Context`, and the full
reset/getTailSize/getTail lifecycle for all ambisonics effects.

Note: `AmbisonicsEncodeEffect` and `PanningEffect` crossfade from the previous
frame's direction; after creation/reset the first `apply` is a warm-up pass,
measure from the second frame (see `P1EffectsTest`).

### P2 — useful extensions: DONE

Completed: `AudioBuffer.mix/downmixFrom/convertAmbisonicsFrom` (+ `audio.AmbisonicsType`),
`geometry.InstancedMesh` (create/add/remove/updateTransform), `StaticMesh.save/load/setMaterial`,
`ProbeBatch.getDataSize/removeData/getReverb/getEnergyField` (with baked-layer identifier constants).

### P3 — low-level primitives: DONE

Completed: `simulation.EnergyField` (create/inspect/reset/copy/swap/add/scale/scaleAccum),
`audio.ImpulseResponse` (same arithmetic), `audio.Reconstructor`
(energy field → impulse response), `simulation.ReflectionsBaker`
(parametric/convolution reflections bake over probes, CPU ray tracer; the #523
no-op-callback workaround applies here too), `ProbeArray.getProbe`, `Scene.saveOBJ`.

Note: `iplSceneSaveOBJ` creates a directory (or files) derived from the base
file name; see `P3Test.sceneSaveObj` for the cleanup pattern.

### Next: GPU acceleration (P4)

- `EmbreeDevice` — Intel Embree ray tracing backend (`iplEmbreeDevice*`).
- `OpenCLDeviceList` / `OpenCLDevice` — device enumeration and selection
  (`iplOpenCLDeviceList*`, `iplOpenCLDevice*`).
- `RadeonRaysDevice` — GPU ray tracing (`iplRadeonRaysDevice*`).
- `TrueAudioNextDevice` — GPU convolution (`iplTrueAudioNextDevice*`).
- Wire the devices through `Scene` (scene type), `Simulator` (reflection type TAN,
  RadeonRays scene type) and `ReflectionsBaker` (OpenCL/RadeonRays parameters).
- Requires shipping `embree.dll` / OpenCL runtime / `GPUUtilities.dll` /
  `TrueAudioNext.dll` alongside `phonon.dll` (already present in
  `natives/steamaudio/lib/windows-x64/`).

### Remaining odds and ends

- `Retain` variants for shared ownership (`iplSceneRetain`, `iplSourceRetain`,
  `iplHRTFRetain`, ...) — add when a use case needs cross-object sharing.

## Known upstream issues (Steam Audio 4.8.1)

- **ValveSoftware/steam-audio#523** — `iplPathBakerBake` segfaults when `progressCallback == NULL`.
  Workaround in `natives/src/path_baker.cpp`: a no-op callback is always passed.
- **ValveSoftware/steam-audio#578** — AVX2 SIMD path in `ArrayMath::multiplyAccumulate`
  uses an aligned load on unaligned data (crash). Not yet hit by our tests, but
  worth watching when `SIMD_LEVEL_AVX2` is the default.

## Conventions for new bindings

- Opaque handles are `long peer` fields, never `JObject`s; `AutoCloseable` everywhere.
- Structs are flattened at the JNI boundary (e.g. `IPLVector3` → 3 floats,
  `IPLSimulationInputs` is assembled natively); no `GetFieldID` in the hot path.
- `DirectByteBuffer` + `GetDirectBufferAddress` for bulk geometry; zero-copy where possible.
- Simulation results that only live on the native side (`IPLReflectionEffectParams`,
  `shCoeffs`, pathing EQ) are forwarded inside the native call, never marshalled to Java.
- Mutable `Vector3` + primitive overloads for hot-path methods; allocating
  convenience overloads are allowed but must be benchmarked against the zero-alloc path.
- Every module ships a JUnit smoke/integration test in the same task; JNI signature
  mismatches are **not** caught by the JVM, so after adding a native method always
  cross-check the Java declaration against the C++ definition argument-by-argument
  (`dumpbin /exports` + signature review), then run the tests.
- Hot-path code additionally gets a JMH benchmark (`src/jmh`, `-prof gc`, old/new
  implementations side by side in one class).
