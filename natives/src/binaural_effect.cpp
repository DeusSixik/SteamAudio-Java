#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                 jint frameSize, jlong hrtfPeer) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLBinauralEffectSettings effectSettings{};
    effectSettings.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));

    IPLBinauralEffect effect = nullptr;
    IPLerror status = iplBinauralEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                              &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jlong contextPeer, jlong hrtfPeer,
                                                jfloat dirX, jfloat dirY, jfloat dirZ,
                                                jint interpolation, jfloat spatialBlend,
                                                jlong inPeer, jlong outPeer) {
    IPLBinauralEffectParams params{};
    params.direction = IPLVector3{static_cast<float>(dirX), static_cast<float>(dirY), static_cast<float>(dirZ)};
    params.interpolation = static_cast<IPLHRTFInterpolation>(interpolation);
    params.spatialBlend = static_cast<float>(spatialBlend);
    params.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));
    params.peakDelays = nullptr;

    IPLAudioEffectState state = iplBinauralEffectApply(static_cast<IPLBinauralEffect>(sajni::asPointer(effectPeer)),
                                                       &params,
                                                       static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
                                                       static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplBinauralEffectReset(static_cast<IPLBinauralEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplBinauralEffectGetTailSize(static_cast<IPLBinauralEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer) {
    IPLAudioEffectState state = iplBinauralEffectGetTail(static_cast<IPLBinauralEffect>(sajni::asPointer(effectPeer)),
                                                         static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_BinauralEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLBinauralEffect effect = static_cast<IPLBinauralEffect>(sajni::asPointer(effectPeer));
    iplBinauralEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
