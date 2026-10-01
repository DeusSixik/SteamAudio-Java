#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                   jint frameSize, jint type, jint irSize, jint numChannels) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLReflectionEffectSettings effectSettings{};
    effectSettings.type = static_cast<IPLReflectionEffectType>(type);
    effectSettings.irSize = static_cast<IPLint32>(irSize);
    effectSettings.numChannels = static_cast<IPLint32>(numChannels);

    IPLReflectionEffect effect = nullptr;
    IPLerror status = iplReflectionEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jlong inPeer, jlong outPeer,
                                                  jlong sourcePeer, jlong mixerPeer) {
    IPLSimulationOutputs outputs{};
    iplSourceGetOutputs(static_cast<IPLSource>(sajni::asPointer(sourcePeer)),
                        IPL_SIMULATIONFLAGS_REFLECTIONS, &outputs);

    IPLAudioEffectState state = iplReflectionEffectApply(
            static_cast<IPLReflectionEffect>(sajni::asPointer(effectPeer)),
            &outputs.reflections,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)),
            (mixerPeer == 0) ? nullptr : static_cast<IPLReflectionMixer>(sajni::asPointer(mixerPeer)));

    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplReflectionEffectReset(static_cast<IPLReflectionEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplReflectionEffectGetTailSize(
            static_cast<IPLReflectionEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer,
                                                    jlong mixerPeer) {
    IPLAudioEffectState state = iplReflectionEffectGetTail(
            static_cast<IPLReflectionEffect>(sajni::asPointer(effectPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)),
            (mixerPeer == 0) ? nullptr : static_cast<IPLReflectionMixer>(sajni::asPointer(mixerPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_ReflectionEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLReflectionEffect effect = static_cast<IPLReflectionEffect>(sajni::asPointer(effectPeer));
    iplReflectionEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
