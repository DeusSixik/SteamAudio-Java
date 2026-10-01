#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_AmbisonicsEncodeEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                         jint frameSize, jint maxOrder) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLAmbisonicsEncodeEffectSettings effectSettings{};
    effectSettings.maxOrder = static_cast<IPLint32>(maxOrder);

    IPLAmbisonicsEncodeEffect effect = nullptr;
    IPLerror status = iplAmbisonicsEncodeEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                      &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_AmbisonicsEncodeEffect_nApply(JNIEnv*, jclass, jlong effectPeer,
                                                        jfloat dirX, jfloat dirY, jfloat dirZ, jint order,
                                                        jlong inPeer, jlong outPeer) {
    IPLAmbisonicsEncodeEffectParams params{};
    params.direction = IPLVector3{dirX, dirY, dirZ};
    params.order = static_cast<IPLint32>(order);

    IPLAudioEffectState state = iplAmbisonicsEncodeEffectApply(
            static_cast<IPLAmbisonicsEncodeEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_AmbisonicsEncodeEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLAmbisonicsEncodeEffect effect = static_cast<IPLAmbisonicsEncodeEffect>(sajni::asPointer(effectPeer));
    iplAmbisonicsEncodeEffectRelease(&effect);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_AmbisonicsRotationEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                           jint frameSize, jint maxOrder) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLAmbisonicsRotationEffectSettings effectSettings{};
    effectSettings.maxOrder = static_cast<IPLint32>(maxOrder);

    IPLAmbisonicsRotationEffect effect = nullptr;
    IPLerror status = iplAmbisonicsRotationEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                        &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_AmbisonicsRotationEffect_nApply(JNIEnv*, jclass, jlong effectPeer,
                                                          jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                          jfloat aheadX, jfloat aheadY, jfloat aheadZ,
                                                          jfloat upX, jfloat upY, jfloat upZ,
                                                          jint order, jlong inPeer, jlong outPeer) {
    jfloat listenerData[9] = {listenerX, listenerY, listenerZ, aheadX, aheadY, aheadZ, upX, upY, upZ};

    IPLAmbisonicsRotationEffectParams params{};
    params.orientation = sajni::makeCoordinateSpace(listenerData);
    params.order = static_cast<IPLint32>(order);

    IPLAudioEffectState state = iplAmbisonicsRotationEffectApply(
            static_cast<IPLAmbisonicsRotationEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_AmbisonicsRotationEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLAmbisonicsRotationEffect effect = static_cast<IPLAmbisonicsRotationEffect>(sajni::asPointer(effectPeer));
    iplAmbisonicsRotationEffectRelease(&effect);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_AmbisonicsPanningEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                          jint frameSize, jint speakerLayoutType, jint maxOrder) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLAmbisonicsPanningEffectSettings effectSettings{};
    effectSettings.speakerLayout.type = static_cast<IPLSpeakerLayoutType>(speakerLayoutType);
    effectSettings.speakerLayout.numSpeakers = 0;
    effectSettings.speakerLayout.speakers = nullptr;
    effectSettings.maxOrder = static_cast<IPLint32>(maxOrder);

    IPLAmbisonicsPanningEffect effect = nullptr;
    IPLerror status = iplAmbisonicsPanningEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                       &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_AmbisonicsPanningEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jint order,
                                                         jlong inPeer, jlong outPeer) {
    IPLAmbisonicsPanningEffectParams params{};
    params.order = static_cast<IPLint32>(order);

    IPLAudioEffectState state = iplAmbisonicsPanningEffectApply(
            static_cast<IPLAmbisonicsPanningEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_AmbisonicsPanningEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLAmbisonicsPanningEffect effect = static_cast<IPLAmbisonicsPanningEffect>(sajni::asPointer(effectPeer));
    iplAmbisonicsPanningEffectRelease(&effect);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_AmbisonicsBinauralEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                                           jint frameSize, jlong hrtfPeer, jint maxOrder) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLAmbisonicsBinauralEffectSettings effectSettings{};
    effectSettings.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));
    effectSettings.maxOrder = static_cast<IPLint32>(maxOrder);

    IPLAmbisonicsBinauralEffect effect = nullptr;
    IPLerror status = iplAmbisonicsBinauralEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                                        &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_AmbisonicsBinauralEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jlong contextPeer,
                                                          jlong hrtfPeer, jint order, jlong inPeer, jlong outPeer) {
    IPLAmbisonicsBinauralEffectParams params{};
    params.hrtf = static_cast<IPLHRTF>(sajni::asPointer(hrtfPeer));
    params.order = static_cast<IPLint32>(order);

    IPLAudioEffectState state = iplAmbisonicsBinauralEffectApply(
            static_cast<IPLAmbisonicsBinauralEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_AmbisonicsBinauralEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLAmbisonicsBinauralEffect effect = static_cast<IPLAmbisonicsBinauralEffect>(sajni::asPointer(effectPeer));
    iplAmbisonicsBinauralEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
