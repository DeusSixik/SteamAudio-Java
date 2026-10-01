#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_DirectEffect_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                               jint frameSize, jint numChannels) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLDirectEffectSettings effectSettings{};
    effectSettings.numChannels = static_cast<IPLint32>(numChannels);

    IPLDirectEffect effect = nullptr;
    IPLerror status = iplDirectEffectCreate(sajni::asContext(contextPeer), &audioSettings,
                                            &effectSettings, &effect);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(effect);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_DirectEffect_nApply(JNIEnv* env, jclass, jlong effectPeer, jfloatArray directOutputs,
                                              jint effectFlags, jint transmissionType, jlong inPeer, jlong outPeer) {
    jfloat* outputsData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(directOutputs, nullptr));
    if (outputsData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return 0;
    }

    IPLDirectEffectParams params{};
    params.flags = static_cast<IPLDirectEffectFlags>(effectFlags);
    params.transmissionType = static_cast<IPLTransmissionType>(transmissionType);
    params.distanceAttenuation = outputsData[0];
    params.airAbsorption[0] = outputsData[1];
    params.airAbsorption[1] = outputsData[2];
    params.airAbsorption[2] = outputsData[3];
    params.directivity = outputsData[4];
    params.occlusion = outputsData[5];
    params.transmission[0] = outputsData[6];
    params.transmission[1] = outputsData[7];
    params.transmission[2] = outputsData[8];

    IPLAudioEffectState state = iplDirectEffectApply(
            static_cast<IPLDirectEffect>(sajni::asPointer(effectPeer)),
            &params,
            static_cast<IPLAudioBuffer*>(sajni::asPointer(inPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));

    env->ReleasePrimitiveArrayCritical(directOutputs, outputsData, 0);

    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_DirectEffect_nReset(JNIEnv*, jclass, jlong effectPeer) {
    iplDirectEffectReset(static_cast<IPLDirectEffect>(sajni::asPointer(effectPeer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_DirectEffect_nGetTailSize(JNIEnv*, jclass, jlong effectPeer) {
    IPLint32 tailSize = iplDirectEffectGetTailSize(static_cast<IPLDirectEffect>(sajni::asPointer(effectPeer)));
    return static_cast<jint>(tailSize);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_DirectEffect_nGetTail(JNIEnv*, jclass, jlong effectPeer, jlong outPeer) {
    IPLAudioEffectState state = iplDirectEffectGetTail(
            static_cast<IPLDirectEffect>(sajni::asPointer(effectPeer)),
            static_cast<IPLAudioBuffer*>(sajni::asPointer(outPeer)));
    return static_cast<jint>(state);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_DirectEffect_nRelease(JNIEnv*, jclass, jlong effectPeer) {
    IPLDirectEffect effect = static_cast<IPLDirectEffect>(sajni::asPointer(effectPeer));
    iplDirectEffectRelease(&effect);
}

#ifdef __cplusplus
} // extern "C"
#endif
