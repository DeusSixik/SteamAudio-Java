#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_HRTF_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate, jint frameSize,
                                       jint type, jfloat volume, jint normType) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLHRTFSettings hrtfSettings{};
    hrtfSettings.type = static_cast<IPLHRTFType>(type);
    hrtfSettings.sofaFileName = nullptr;
    hrtfSettings.sofaData = nullptr;
    hrtfSettings.sofaDataSize = 0;
    hrtfSettings.volume = static_cast<float>(volume);
    hrtfSettings.normType = static_cast<IPLHRTFNormType>(normType);

    IPLHRTF hrtf = nullptr;
    IPLerror status = iplHRTFCreate(sajni::asContext(contextPeer), &audioSettings, &hrtfSettings, &hrtf);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(hrtf);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_HRTF_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLHRTF hrtf = static_cast<IPLHRTF>(sajni::asPointer(peer));
    iplHRTFRelease(&hrtf);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_HRTF_nCreateSofaFile(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                               jint frameSize, jfloat volume, jint normType, jstring sofaFileName) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLHRTFSettings hrtfSettings{};
    hrtfSettings.type = IPL_HRTFTYPE_SOFA;
    hrtfSettings.volume = static_cast<float>(volume);
    hrtfSettings.normType = static_cast<IPLHRTFNormType>(normType);

    const char* fileName = env->GetStringUTFChars(sofaFileName, nullptr);
    if (fileName == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_FAILURE);
        return 0;
    }
    hrtfSettings.sofaFileName = fileName;
    hrtfSettings.sofaData = nullptr;
    hrtfSettings.sofaDataSize = 0;

    IPLHRTF hrtf = nullptr;
    IPLerror status = iplHRTFCreate(sajni::asContext(contextPeer), &audioSettings, &hrtfSettings, &hrtf);

    env->ReleaseStringUTFChars(sofaFileName, fileName);

    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(hrtf);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_HRTF_nCreateSofaData(JNIEnv* env, jclass, jlong contextPeer, jint samplingRate,
                                               jint frameSize, jfloat volume, jint normType, jbyteArray sofaData) {
    IPLAudioSettings audioSettings{};
    audioSettings.samplingRate = static_cast<IPLint32>(samplingRate);
    audioSettings.frameSize = static_cast<IPLint32>(frameSize);

    IPLHRTFSettings hrtfSettings{};
    hrtfSettings.type = IPL_HRTFTYPE_SOFA;
    hrtfSettings.volume = static_cast<float>(volume);
    hrtfSettings.normType = static_cast<IPLHRTFNormType>(normType);
    hrtfSettings.sofaFileName = nullptr;

    jbyte* data = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(sofaData, nullptr));
    if (data == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return 0;
    }
    hrtfSettings.sofaData = reinterpret_cast<const IPLuint8*>(data);
    hrtfSettings.sofaDataSize = env->GetArrayLength(sofaData);

    IPLHRTF hrtf = nullptr;
    IPLerror status = iplHRTFCreate(sajni::asContext(contextPeer), &audioSettings, &hrtfSettings, &hrtf);

    env->ReleasePrimitiveArrayCritical(sofaData, data, 0);

    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(hrtf);
}

#ifdef __cplusplus
} // extern "C"
#endif
