#include <cstring>
#include <string>

#include "steamaudio_jni.h"

namespace {

jstring toJString(JNIEnv* env, const char* value) {
    if (value == nullptr) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(value);
}

} // namespace

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_gpu_EmbreeDevice_nCreate(JNIEnv* env, jclass, jlong contextPeer) {
    IPLEmbreeDeviceSettings settings{};
    settings.reserved = 0;

    IPLEmbreeDevice device = nullptr;
    IPLerror status = iplEmbreeDeviceCreate(sajni::asContext(contextPeer), &settings, &device);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(device);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_gpu_EmbreeDevice_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLEmbreeDevice device = static_cast<IPLEmbreeDevice>(sajni::asPointer(peer));
    iplEmbreeDeviceRelease(&device);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDeviceList_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint deviceType,
                                                       jint numCUsToReserve, jfloat fractionCUsForIRUpdate,
                                                       jboolean requiresTAN) {
    IPLOpenCLDeviceSettings settings{};
    settings.type = static_cast<IPLOpenCLDeviceType>(deviceType);
    settings.numCUsToReserve = static_cast<IPLint32>(numCUsToReserve);
    settings.fractionCUsForIRUpdate = static_cast<IPLfloat32>(fractionCUsForIRUpdate);
    settings.requiresTAN = (requiresTAN == JNI_TRUE) ? IPL_TRUE : IPL_FALSE;

    IPLOpenCLDeviceList deviceList = nullptr;
    IPLerror status = iplOpenCLDeviceListCreate(sajni::asContext(contextPeer), &settings, &deviceList);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(deviceList);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDeviceList_nGetNumDevices(JNIEnv*, jclass, jlong peer) {
    return static_cast<jint>(iplOpenCLDeviceListGetNumDevices(
            static_cast<IPLOpenCLDeviceList>(sajni::asPointer(peer))));
}

JNIEXPORT jobject JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDeviceList_nGetDeviceDesc(JNIEnv* env, jclass, jlong peer, jint index) {
    auto deviceList = static_cast<IPLOpenCLDeviceList>(sajni::asPointer(peer));

    IPLOpenCLDeviceDesc desc{};
    iplOpenCLDeviceListGetDeviceDesc(deviceList, static_cast<IPLint32>(index), &desc);

    jclass cls = env->FindClass("net/sixik/steamaudio/gpu/OpenCLDeviceList$DeviceDesc");
    if (cls == nullptr) {
        return nullptr;
    }
    jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
    if (constructor == nullptr) {
        env->DeleteLocalRef(cls);
        return nullptr;
    }
    jobject result = env->NewObject(cls, constructor);
    env->DeleteLocalRef(cls);

    jstring platformName = toJString(env, desc.platformName);
    jstring platformVendor = toJString(env, desc.platformVendor);
    jstring platformVersion = toJString(env, desc.platformVersion);
    jstring deviceName = toJString(env, desc.deviceName);
    jstring deviceVendor = toJString(env, desc.deviceVendor);
    jstring deviceVersion = toJString(env, desc.deviceVersion);

    jclass resultCls = env->GetObjectClass(result);
    auto setString = [&](const char* name, jstring value) {
        jfieldID field = env->GetFieldID(resultCls, name, "Ljava/lang/String;");
        if (field != nullptr) {
            env->SetObjectField(result, field, value);
        }
        env->DeleteLocalRef(value);
    };
    setString("platformName", platformName);
    setString("platformVendor", platformVendor);
    setString("platformVersion", platformVersion);
    setString("deviceName", deviceName);
    setString("deviceVendor", deviceVendor);
    setString("deviceVersion", deviceVersion);

    auto setInt = [&](const char* name, jint value) {
        jfieldID field = env->GetFieldID(resultCls, name, "I");
        if (field != nullptr) {
            env->SetIntField(result, field, value);
        }
    };
    setInt("type", static_cast<jint>(desc.type));
    setInt("numConvolutionCUs", static_cast<jint>(desc.numConvolutionCUs));
    setInt("numIRUpdateCUs", static_cast<jint>(desc.numIRUpdateCUs));
    setInt("granularity", static_cast<jint>(desc.granularity));

    jfieldID perfScoreField = env->GetFieldID(resultCls, "perfScore", "F");
    if (perfScoreField != nullptr) {
        env->SetFloatField(result, perfScoreField, static_cast<jfloat>(desc.perfScore));
    }

    env->DeleteLocalRef(resultCls);
    return result;
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDeviceList_nCreateDevice(JNIEnv* env, jclass, jlong contextPeer, jlong peer,
                                                             jint index) {
    IPLOpenCLDevice device = nullptr;
    IPLerror status = iplOpenCLDeviceCreate(sajni::asContext(contextPeer),
                                            static_cast<IPLOpenCLDeviceList>(sajni::asPointer(peer)),
                                            static_cast<IPLint32>(index), &device);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(device);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDeviceList_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLOpenCLDeviceList deviceList = static_cast<IPLOpenCLDeviceList>(sajni::asPointer(peer));
    iplOpenCLDeviceListRelease(&deviceList);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_gpu_OpenCLDevice_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLOpenCLDevice device = static_cast<IPLOpenCLDevice>(sajni::asPointer(peer));
    iplOpenCLDeviceRelease(&device);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_gpu_RadeonRaysDevice_nCreate(JNIEnv* env, jclass, jlong openCLDevicePeer) {
    IPLRadeonRaysDeviceSettings settings{};
    settings.reserved = 0;

    IPLRadeonRaysDevice device = nullptr;
    IPLerror status = iplRadeonRaysDeviceCreate(
            static_cast<IPLOpenCLDevice>(sajni::asPointer(openCLDevicePeer)), &settings, &device);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(device);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_gpu_RadeonRaysDevice_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLRadeonRaysDevice device = static_cast<IPLRadeonRaysDevice>(sajni::asPointer(peer));
    iplRadeonRaysDeviceRelease(&device);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_gpu_TrueAudioNextDevice_nCreate(JNIEnv* env, jclass, jlong openCLDevicePeer, jint frameSize,
                                                          jint irSize, jint order, jint maxSources) {
    IPLTrueAudioNextDeviceSettings settings{};
    settings.frameSize = static_cast<IPLint32>(frameSize);
    settings.irSize = static_cast<IPLint32>(irSize);
    settings.order = static_cast<IPLint32>(order);
    settings.maxSources = static_cast<IPLint32>(maxSources);

    IPLTrueAudioNextDevice device = nullptr;
    IPLerror status = iplTrueAudioNextDeviceCreate(
            static_cast<IPLOpenCLDevice>(sajni::asPointer(openCLDevicePeer)), &settings, &device);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(device);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_gpu_TrueAudioNextDevice_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLTrueAudioNextDevice device = static_cast<IPLTrueAudioNextDevice>(sajni::asPointer(peer));
    iplTrueAudioNextDeviceRelease(&device);
}

#ifdef __cplusplus
} // extern "C"
#endif
