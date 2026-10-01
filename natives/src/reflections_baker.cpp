#include "steamaudio_jni.h"

// Upstream bug ValveSoftware/steam-audio#523 applies to the reflections
// baker as well: the progress callback is invoked unconditionally, so a
// no-op callback is always passed instead of NULL.
static void noopProgressCallback(IPLfloat32, void*) {
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ReflectionsBaker_nBake(JNIEnv*, jclass, jlong contextPeer, jlong scenePeer,
                                                            jlong probeBatchPeer, jint dataType, jint variation,
                                                            jint bakeFlags, jint numRays, jint numBounces,
                                                            jint numDiffuseSamples, jfloat simulatedDuration,
                                                            jfloat savedDuration, jint order,
                                                            jfloat irradianceMinDistance, jint numThreads,
                                                            jint rayBatchSize) {
    IPLReflectionsBakeParams params{};
    params.scene = static_cast<IPLScene>(sajni::asPointer(scenePeer));
    params.probeBatch = static_cast<IPLProbeBatch>(sajni::asPointer(probeBatchPeer));
    params.sceneType = IPL_SCENETYPE_DEFAULT;
    params.identifier.type = static_cast<IPLBakedDataType>(dataType);
    params.identifier.variation = static_cast<IPLBakedDataVariation>(variation);
    params.identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    params.identifier.endpointInfluence.radius = 0.0f;
    params.bakeFlags = static_cast<IPLReflectionsBakeFlags>(bakeFlags);
    params.numRays = static_cast<IPLint32>(numRays);
    params.numBounces = static_cast<IPLint32>(numBounces);
    params.numDiffuseSamples = static_cast<IPLint32>(numDiffuseSamples);
    params.simulatedDuration = static_cast<IPLfloat32>(simulatedDuration);
    params.savedDuration = static_cast<IPLfloat32>(savedDuration);
    params.order = static_cast<IPLint32>(order);
    params.irradianceMinDistance = static_cast<IPLfloat32>(irradianceMinDistance);
    params.numThreads = static_cast<IPLint32>(numThreads);
    params.rayBatchSize = static_cast<IPLint32>(rayBatchSize);
    params.bakeBatchSize = 1;
    params.openCLDevice = nullptr;
    params.radeonRaysDevice = nullptr;

    iplReflectionsBakerBake(sajni::asContext(contextPeer), &params, noopProgressCallback, nullptr);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ReflectionsBaker_nCancelBake(JNIEnv*, jclass, jlong contextPeer) {
    iplReflectionsBakerCancelBake(sajni::asContext(contextPeer));
}

#ifdef __cplusplus
} // extern "C"
#endif
