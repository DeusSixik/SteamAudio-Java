#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint flags, jint sceneType,
                                            jint reflectionType, jint maxNumOcclusionSamples, jint maxNumRays,
                                            jint numDiffuseSamples, jfloat maxDuration, jint maxOrder,
                                            jint maxNumSources, jint numThreads, jint rayBatchSize,
                                            jint numVisSamples, jint samplingRate, jint frameSize) {
    IPLSimulationSettings settings{};
    settings.flags = static_cast<IPLSimulationFlags>(flags);
    settings.sceneType = static_cast<IPLSceneType>(sceneType);
    settings.reflectionType = static_cast<IPLReflectionEffectType>(reflectionType);
    settings.maxNumOcclusionSamples = static_cast<IPLint32>(maxNumOcclusionSamples);
    settings.maxNumRays = static_cast<IPLint32>(maxNumRays);
    settings.numDiffuseSamples = static_cast<IPLint32>(numDiffuseSamples);
    settings.maxDuration = static_cast<IPLfloat32>(maxDuration);
    settings.maxOrder = static_cast<IPLint32>(maxOrder);
    settings.maxNumSources = static_cast<IPLint32>(maxNumSources);
    settings.numThreads = static_cast<IPLint32>(numThreads);
    settings.rayBatchSize = static_cast<IPLint32>(rayBatchSize);
    settings.numVisSamples = static_cast<IPLint32>(numVisSamples);
    settings.samplingRate = static_cast<IPLint32>(samplingRate);
    settings.frameSize = static_cast<IPLint32>(frameSize);
    settings.openCLDevice = nullptr;
    settings.radeonRaysDevice = nullptr;
    settings.tanDevice = nullptr;

    IPLSimulator simulator = nullptr;
    IPLerror status = iplSimulatorCreate(sajni::asContext(contextPeer), &settings, &simulator);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(simulator);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nSetScene(JNIEnv*, jclass, jlong peer, jlong scenePeer) {
    iplSimulatorSetScene(static_cast<IPLSimulator>(sajni::asPointer(peer)),
                         static_cast<IPLScene>(sajni::asPointer(scenePeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nSetSharedInputs(JNIEnv* env, jclass, jlong peer, jint flags,
                                                     jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                     jfloat aheadX, jfloat aheadY, jfloat aheadZ,
                                                     jfloat upX, jfloat upY, jfloat upZ,
                                                     jint numRays, jint numBounces, jfloat duration,
                                                     jint order, jfloat irradianceMinDistance) {
    jfloat listenerData[9] = {listenerX, listenerY, listenerZ, aheadX, aheadY, aheadZ, upX, upY, upZ};

    IPLSimulationSharedInputs sharedInputs{};
    sharedInputs.listener = sajni::makeCoordinateSpace(listenerData);
    sharedInputs.numRays = static_cast<IPLint32>(numRays);
    sharedInputs.numBounces = static_cast<IPLint32>(numBounces);
    sharedInputs.duration = static_cast<IPLfloat32>(duration);
    sharedInputs.order = static_cast<IPLint32>(order);
    sharedInputs.irradianceMinDistance = static_cast<IPLfloat32>(irradianceMinDistance);
    sharedInputs.pathingVisCallback = nullptr;
    sharedInputs.pathingUserData = nullptr;

    iplSimulatorSetSharedInputs(static_cast<IPLSimulator>(sajni::asPointer(peer)),
                                static_cast<IPLSimulationFlags>(flags), &sharedInputs);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nCommit(JNIEnv*, jclass, jlong peer) {
    iplSimulatorCommit(static_cast<IPLSimulator>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nRunDirect(JNIEnv*, jclass, jlong peer) {
    iplSimulatorRunDirect(static_cast<IPLSimulator>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nAddProbeBatch(JNIEnv*, jclass, jlong peer, jlong probeBatchPeer) {
    iplSimulatorAddProbeBatch(static_cast<IPLSimulator>(sajni::asPointer(peer)),
                              static_cast<IPLProbeBatch>(sajni::asPointer(probeBatchPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nRemoveProbeBatch(JNIEnv*, jclass, jlong peer, jlong probeBatchPeer) {
    iplSimulatorRemoveProbeBatch(static_cast<IPLSimulator>(sajni::asPointer(peer)),
                                 static_cast<IPLProbeBatch>(sajni::asPointer(probeBatchPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nRunReflections(JNIEnv*, jclass, jlong peer) {
    iplSimulatorRunReflections(static_cast<IPLSimulator>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nRunPathing(JNIEnv*, jclass, jlong peer) {
    iplSimulatorRunPathing(static_cast<IPLSimulator>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_Simulator_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLSimulator simulator = static_cast<IPLSimulator>(sajni::asPointer(peer));
    iplSimulatorRelease(&simulator);
}

#ifdef __cplusplus
} // extern "C"
#endif
