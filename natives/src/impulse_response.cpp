#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nCreate(JNIEnv* env, jclass, jlong contextPeer, jfloat duration,
                                                        jint order, jint samplingRate) {
    IPLImpulseResponseSettings settings{};
    settings.duration = static_cast<IPLfloat32>(duration);
    settings.order = static_cast<IPLint32>(order);
    settings.samplingRate = static_cast<IPLint32>(samplingRate);

    IPLImpulseResponse impulseResponse = nullptr;
    IPLerror status = iplImpulseResponseCreate(sajni::asContext(contextPeer), &settings, &impulseResponse);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(impulseResponse);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nGetNumChannels(JNIEnv*, jclass, jlong peer) {
    return static_cast<jint>(iplImpulseResponseGetNumChannels(
            static_cast<IPLImpulseResponse>(sajni::asPointer(peer))));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nGetNumSamples(JNIEnv*, jclass, jlong peer) {
    return static_cast<jint>(iplImpulseResponseGetNumSamples(
            static_cast<IPLImpulseResponse>(sajni::asPointer(peer))));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nGetData(JNIEnv* env, jclass, jlong peer, jfloatArray out) {
    auto impulseResponse = static_cast<IPLImpulseResponse>(sajni::asPointer(peer));

    IPLint32 numChannels = iplImpulseResponseGetNumChannels(impulseResponse);
    IPLint32 numSamples = iplImpulseResponseGetNumSamples(impulseResponse);
    IPLfloat32* data = iplImpulseResponseGetData(impulseResponse);

    if (data != nullptr && numChannels > 0 && numSamples > 0) {
        jsize total = static_cast<jsize>(numChannels * numSamples);
        env->SetFloatArrayRegion(out, 0, total, data);
    }
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nReset(JNIEnv*, jclass, jlong peer) {
    iplImpulseResponseReset(static_cast<IPLImpulseResponse>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nCopy(JNIEnv*, jclass, jlong srcPeer, jlong dstPeer) {
    iplImpulseResponseCopy(static_cast<IPLImpulseResponse>(sajni::asPointer(srcPeer)),
                           static_cast<IPLImpulseResponse>(sajni::asPointer(dstPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nSwap(JNIEnv*, jclass, jlong aPeer, jlong bPeer) {
    iplImpulseResponseSwap(static_cast<IPLImpulseResponse>(sajni::asPointer(aPeer)),
                           static_cast<IPLImpulseResponse>(sajni::asPointer(bPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nAdd(JNIEnv*, jclass, jlong in1Peer, jlong in2Peer, jlong outPeer) {
    iplImpulseResponseAdd(static_cast<IPLImpulseResponse>(sajni::asPointer(in1Peer)),
                          static_cast<IPLImpulseResponse>(sajni::asPointer(in2Peer)),
                          static_cast<IPLImpulseResponse>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nScale(JNIEnv*, jclass, jlong inPeer, jfloat scalar, jlong outPeer) {
    iplImpulseResponseScale(static_cast<IPLImpulseResponse>(sajni::asPointer(inPeer)),
                            static_cast<IPLfloat32>(scalar),
                            static_cast<IPLImpulseResponse>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nScaleAccum(JNIEnv*, jclass, jlong inPeer, jfloat scalar, jlong outPeer) {
    iplImpulseResponseScaleAccum(static_cast<IPLImpulseResponse>(sajni::asPointer(inPeer)),
                                 static_cast<IPLfloat32>(scalar),
                                 static_cast<IPLImpulseResponse>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_ImpulseResponse_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLImpulseResponse impulseResponse = static_cast<IPLImpulseResponse>(sajni::asPointer(peer));
    iplImpulseResponseRelease(&impulseResponse);
}

#ifdef __cplusplus
} // extern "C"
#endif
