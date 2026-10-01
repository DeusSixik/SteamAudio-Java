#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_PathEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                             jint frameSize, jint maxOrder) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLPathEffectSettings effectSettings{};
    effectSettings.maxOrder = static_cast<IPLint32>(maxOrder);
    effectSettings.spatialize = IPL_FALSE;
    effectSettings.speakerLayout.type = IPL_SPEAKERLAYOUTTYPE_STEREO;
    effectSettings.speakerLayout.numSpeakers = 0;
    effectSettings.speakerLayout.speakers = nullptr;
    effectSettings.hrtf = nullptr;

    IPLPathEffect effect = nullptr;
    IPLerror status = iplPathEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                          &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_PathEffect_nApply(JNIEnv*, jclass, jlong effectPeer, jlong inPeer, jlong outPeer,
                                            jlong sourcePeer, jint order) {
    IPLSimulationOutputs outputs{};
    iplSourceGetOutputs(static_cast<IPLSource>(sajni::asPointer(sourcePeer)),
                        IPL_SIMULATIONFLAGS_PATHING, &outputs);

    IPLPathEffectParams params{};
    params.eqCoeffs[0] = outputs.pathing.eqCoeffs[0];
    params.eqCoeffs[1] = outputs.pathing.eqCoeffs[1];
    params.eqCoeffs[2] = outputs.pathing.eqCoeffs[2];
    params.shCoeffs = outputs.pathing.shCoeffs;
    params.order = static_cast<IPLint32>(order);
    params.binaural = IPL_FALSE;
    params.hrtf = nullptr;
    params.listener = IPLCoordinateSpace3{};
    params.normalizeEQ = IPL_FALSE;

    IPLAudioEffectState state = iplPathEffectApply(
            static_cast<IPLPathEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));

    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_PathEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplPathEffectReset(static_cast<IPLPathEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_PathEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplPathEffectGetTailSize(static_cast<IPLPathEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_PathEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer) {
    IPLAudioEffectState state = iplPathEffectGetTail(
            static_cast<IPLPathEffect>(sajni::asPointer(effectPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_PathEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLPathEffect effect = static_cast<IPLPathEffect>(sajni::asPointer(effectPeer));
    iplPathEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
