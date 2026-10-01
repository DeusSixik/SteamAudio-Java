#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_audio_Reconstructor_nCreate(JNIEnv* env, jclass, jlong contextPeer, jfloat maxDuration,
                                                      jint maxOrder, jint samplingRate) {
    IPLReconstructorSettings settings{};
    settings.maxDuration = static_cast<IPLfloat32>(maxDuration);
    settings.maxOrder = static_cast<IPLint32>(maxOrder);
    settings.samplingRate = static_cast<IPLint32>(samplingRate);

    IPLReconstructor reconstructor = nullptr;
    IPLerror status = iplReconstructorCreate(sajni::asContext(contextPeer), &settings, &reconstructor);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(reconstructor);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_Reconstructor_nReconstruct(JNIEnv*, jclass, jlong peer, jlong energyFieldPeer,
                                                           jfloat duration, jint order, jlong outPeer) {
    IPLReconstructorInputs inputs{};
    inputs.energyField = static_cast<IPLEnergyField>(sajni::asPointer(energyFieldPeer));

    IPLReconstructorSharedInputs sharedInputs{};
    sharedInputs.duration = static_cast<IPLfloat32>(duration);
    sharedInputs.order = static_cast<IPLint32>(order);

    IPLReconstructorOutputs outputs{};
    outputs.impulseResponse = static_cast<IPLImpulseResponse>(sajni::asPointer(outPeer));

    iplReconstructorReconstruct(static_cast<IPLReconstructor>(sajni::asPointer(peer)),
                                1, &inputs, &sharedInputs, &outputs);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_Reconstructor_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLReconstructor reconstructor = static_cast<IPLReconstructor>(sajni::asPointer(peer));
    iplReconstructorRelease(&reconstructor);
}

#ifdef __cplusplus
} // extern "C"
#endif
