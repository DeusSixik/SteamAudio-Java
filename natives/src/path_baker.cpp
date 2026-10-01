#include "steamaudio_jni.h"

// Workaround for upstream bug ValveSoftware/steam-audio#523 (fixed by #524):
// iplPathBakerBake invokes the progress callback unconditionally, so a NULL
// callback crashes with an access violation. A no-op callback is always passed.
static void noopProgressCallback(IPLfloat32, void*) {
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_PathBaker_nBake(JNIEnv* env, jclass, jlong contextPeer, jlong scenePeer,
                                          jlong probeBatchPeer, jint numSamples, jfloat radius, jfloat threshold,
                                          jfloat visRange, jfloat pathRange, jint numThreads) {
    IPLPathBakeParams params{};
    params.scene = static_cast<IPLScene>(sajni::asPointer(scenePeer));
    params.probeBatch = static_cast<IPLProbeBatch>(sajni::asPointer(probeBatchPeer));
    params.identifier.type = IPL_BAKEDDATATYPE_PATHING;
    params.identifier.variation = IPL_BAKEDDATAVARIATION_DYNAMIC;
    params.identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    params.identifier.endpointInfluence.radius = 0.0f;
    params.numSamples = static_cast<IPLint32>(numSamples);
    params.radius = static_cast<IPLfloat32>(radius);
    params.threshold = static_cast<IPLfloat32>(threshold);
    params.visRange = static_cast<IPLfloat32>(visRange);
    params.pathRange = static_cast<IPLfloat32>(pathRange);
    params.numThreads = static_cast<IPLint32>(numThreads);

    iplPathBakerBake(sajni::asContext(contextPeer), &params, noopProgressCallback, nullptr);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_PathBaker_nCancelBake(JNIEnv*, jclass, jlong contextPeer) {
    iplPathBakerCancelBake(sajni::asContext(contextPeer));
}

#ifdef __cplusplus
} // extern "C"
#endif
