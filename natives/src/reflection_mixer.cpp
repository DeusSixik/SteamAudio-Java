#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_ReflectionMixer_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                  jint frameSize, jint type, jint irSize, jint numChannels) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLReflectionEffectSettings effectSettings{};
    effectSettings.type = static_cast<IPLReflectionEffectType>(type);
    effectSettings.irSize = static_cast<IPLint32>(irSize);
    effectSettings.numChannels = static_cast<IPLint32>(numChannels);

    IPLReflectionMixer mixer = nullptr;
    IPLerror status = iplReflectionMixerCreate(sajni::asContext(contextPeer), &audioSettings,
                                               &effectSettings, &mixer);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(mixer);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_ReflectionMixer_nApply(JNIEnv*, jclass, jlong peer, jlong outPeer, jlong sourcePeer) {
    IPLSimulationOutputs outputs{};
    iplSourceGetOutputs(static_cast<IPLSource>(sajni::asPointer(sourcePeer)),
                        IPL_SIMULATIONFLAGS_REFLECTIONS, &outputs);

    IPLAudioEffectState state = iplReflectionMixerApply(
            static_cast<IPLReflectionMixer>(sajni::asPointer(peer)),
            &outputs.reflections,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));

    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_ReflectionMixer_nReset(JNIEnv*, jclass, jlong peer) {
    iplReflectionMixerReset(static_cast<IPLReflectionMixer>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_ReflectionMixer_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLReflectionMixer mixer = static_cast<IPLReflectionMixer>(sajni::asPointer(peer));
    iplReflectionMixerRelease(&mixer);
}

#ifdef __cplusplus
} // extern "C"
#endif
