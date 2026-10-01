#include <cstdlib>

#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_audio_AudioBuffer_nAllocate(JNIEnv* env, jclass, jlong contextPeer, jint numChannels, jint numSamples) {
    auto buffer = static_cast<IPLAudioBuffer*>(std::malloc(sizeof(IPLAudioBuffer)));
    if (buffer == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return 0;
    }

    IPLerror status = iplAudioBufferAllocate(sajni::asContext(contextPeer),
                                             static_cast<IPLint32>(numChannels),
                                             static_cast<IPLint32>(numSamples),
                                             buffer);
    if (status != IPL_STATUS_SUCCESS) {
        std::free(buffer);
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(buffer);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_AudioBuffer_nFree(JNIEnv*, jclass, jlong contextPeer, jlong peer) {
    auto buffer = static_cast<IPLAudioBuffer*>(sajni::asPointer(peer));
    iplAudioBufferFree(sajni::asContext(contextPeer), buffer);
    std::free(buffer);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_AudioBuffer_nInterleave(JNIEnv* env, jclass, jlong contextPeer, jlong peer, jfloatArray dst) {
    auto buffer = static_cast<IPLAudioBuffer*>(sajni::asPointer(peer));

    jfloat* dstData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(dst, nullptr));
    if (dstData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return;
    }

    iplAudioBufferInterleave(sajni::asContext(contextPeer), buffer, dstData);

    env->ReleasePrimitiveArrayCritical(dst, dstData, 0);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_audio_AudioBuffer_nDeinterleave(JNIEnv* env, jclass, jlong contextPeer, jlong peer, jfloatArray src) {
    auto buffer = static_cast<IPLAudioBuffer*>(sajni::asPointer(peer));

    jfloat* srcData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(src, nullptr));
    if (srcData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return;
    }

    iplAudioBufferDeinterleave(sajni::asContext(contextPeer), srcData, buffer);

    env->ReleasePrimitiveArrayCritical(src, srcData, 0);
}

#ifdef __cplusplus
} // extern "C"
#endif
