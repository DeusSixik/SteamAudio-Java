# SteamAudio-Java — Roadmap

JNI bindings for [Steam Audio](https://valvesoftware.github.io/steam-audio/) (C API 4.8.1),
targeting Java 17+.

## API coverage

Steam Audio 4.8.1 exposes **210 `ipl*` functions**. Covered: **~205 (98%)**.
The remainder is `iplOpenCLDeviceCreateFromExisting` (wrapping caller-owned
`cl_command_queue`s) and a few `Retain` variants.

### Done

| Area | Classes |
|---|---|
| Context & math | `core.Context` (+ `calculateDistanceAttenuation` / `calculateAirAbsorption` / `calculateDirectivity`), `core.Vector3`, `core.SerializedObject` |
| Audio buffers & HRTF | `audio.AudioBuffer` (+ `mix`, `downmixFrom`, `convertAmbisonicsFrom`), `audio.HRTF` (+ SOFA file/memory loading), `audio.SpeakerLayout`, `audio.AmbisonicsType`, `audio.ImpulseResponse`, `audio.Reconstructor` |
| Geometry | `geometry.Scene` (+ `saveOBJ`, Embree/RadeonRays scene types), `geometry.StaticMesh` (+ `save`/`load`/`setMaterial`), `geometry.InstancedMesh`, `geometry.Material` |
| Simulation | `simulation.Simulator` (+ GPU device constructor), `simulation.Source`, `simulation.ProbeArray` (+ `getProbe`), `simulation.ProbeBatch` (+ `getDataSize`/`removeData`/`getReverb`/`getEnergyField`), `simulation.PathBaker`, `simulation.ReflectionsBaker` (+ GPU baking), `simulation.EnergyField` |
| Effects | `effects.BinauralEffect`, `effects.DirectEffect`, `effects.PanningEffect`, `effects.VirtualSurroundEffect`, `effects.ReflectionEffect`, `effects.ReflectionMixer`, `effects.PathEffect`, `effects.Ambisonics{Encode,Rotation,Panning,Binaural,Decode}Effect` (all with full reset/getTail lifecycle) |
| GPU | `gpu.EmbreeDevice`, `gpu.OpenCLDeviceList` (+ `DeviceDesc`), `gpu.OpenCLDevice`, `gpu.RadeonRaysDevice`, `gpu.TrueAudioNextDevice`; wired into `Scene`, `Simulator` and `ReflectionsBaker` |
| Loader | Auto-build via CMake (`configureNatives`/`buildNatives`), natives bundled into the jar, extraction to a content-hashed temp dir with `-Dsteamaudio.natives` override |
| Benchmarks | `src/jmh` (`SteamAudioBenchmark`), zero-alloc verified via `-prof gc` |

### P4 — GPU acceleration: DONE

Completed: `gpu.EmbreeDevice`, `gpu.OpenCLDeviceList` (enumeration with CU
reservation requirements + device descriptors), `gpu.OpenCLDevice`,
`gpu.RadeonRaysDevice`, `gpu.TrueAudioNextDevice`; scene types EMBREE /
RADEONRAYS wired through `Scene`, a full `Simulator` constructor accepting
OpenCL/RadeonRays/TAN devices, and GPU-accelerated `ReflectionsBaker.bake`.

Hardware notes (see `gpu.GpuDevicesTest`):

- `embree.dll` must be shipped next to `phonon.dll`; creation fails cleanly
  (SteamAudioException) when absent.
- An OpenCL runtime must be installed for device enumeration; otherwise
  creation fails cleanly.
- Radeon Rays and TrueAudio Next only support AMD GPUs; on other vendors
  phonon may **crash** instead of returning an error, so the tests only
  exercise them when an AMD device is present. Do not create RR/TAN devices
  on non-AMD hardware.
- Creating an EMBREE-type scene with a NULL Embree device crashes inside
  phonon — always supply a device for that scene type.

### Remaining odds and ends

- `Retain` variants for shared ownership (`iplSceneRetain`, `iplSourceRetain`,
  `iplHRTFRetain`, ...) — add when a use case needs cross-object sharing.
- `iplOpenCLDeviceCreateFromExisting` — wrapping caller-owned
  `cl_command_queue` pairs.

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
