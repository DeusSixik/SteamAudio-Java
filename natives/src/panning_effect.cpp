#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                        jint frameSize, jint speakerLayoutType) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLPanningEffectSettings effectSettings{};
    effectSettings.speakerLayout.type = static_cast<IPLSpeakerLayoutType>(speakerLayoutType);
    effectSettings.speakerLayout.numSpeakers = 0;
    effectSettings.speakerLayout.speakers = nullptr;

    IPLPanningEffect effect = nullptr;
    IPLerror status = iplPanningEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                             &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nApply(JNIEnv*, jclass, jlong effectPeer,
                                                       jfloat dirX, jfloat dirY, jfloat dirZ,
                                                       jlong inPeer, jlong outPeer) {
    IPLPanningEffectParams params{};
    params.direction = IPLVector3{dirX, dirY, dirZ};

    IPLAudioEffectState state = iplPanningEffectApply(
            static_cast<IPLPanningEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplPanningEffectReset(static_cast<IPLPanningEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplPanningEffectGetTailSize(static_cast<IPLPanningEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer) {
    IPLAudioEffectState state = iplPanningEffectGetTail(
            static_cast<IPLPanningEffect>(sajni::asPointer(effectPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_PanningEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLPanningEffect effect = static_cast<IPLPanningEffect>(sajni::asPointer(effectPeer));
    iplPanningEffectRelease(&effect);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                                jint frameSize, jint speakerLayoutType, jlong hrtfPeer) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLVirtualSurroundEffectSettings effectSettings{};
    effectSettings.speakerLayout.type = static_cast<IPLSpeakerLayoutType>(speakerLayoutType);
    effectSettings.speakerLayout.numSpeakers = 0;
    effectSettings.speakerLayout.speakers = nullptr;
    effectSettings.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));

    IPLVirtualSurroundEffect effect = nullptr;
    IPLerror status = iplVirtualSurroundEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                     &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jlong hrtfPeer,
                                                               jlong inPeer, jlong outPeer) {
    IPLVirtualSurroundEffectParams params{};
    params.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));

    IPLAudioEffectState state = iplVirtualSurroundEffectApply(
            static_cast<IPLVirtualSurroundEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplVirtualSurroundEffectReset(static_cast<IPLVirtualSurroundEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplVirtualSurroundEffectGetTailSize(
            static_cast<IPLVirtualSurroundEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer) {
    IPLAudioEffectState state = iplVirtualSurroundEffectGetTail(
            static_cast<IPLVirtualSurroundEffect>(sajni::asPointer(effectPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_effects_VirtualSurroundEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLVirtualSurroundEffect effect = static_cast<IPLVirtualSurroundEffect>(sajni::asPointer(effectPeer));
    iplVirtualSurroundEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
