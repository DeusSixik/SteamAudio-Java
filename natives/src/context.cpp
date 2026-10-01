#include <cstdio>

#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_Context_nCreate(JNIEnv* env, jclass, jint version, jint simdLevel, jint flags) {
    IPLContextSettings settings{};
    settings.version = static_cast<IPLuint32>(version);
    settings.logCallback = nullptr;
    settings.allocateCallback = nullptr;
    settings.freeCallback = nullptr;
    settings.simdLevel = static_cast<IPLSIMDLevel>(simdLevel);
    settings.flags = static_cast<IPLContextFlags>(flags);

    IPLContext context = nullptr;
    IPLerror status = iplContextCreate(&settings, &context);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(context);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Context_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLContext context = sajni::asContext(peer);
    iplContextRelease(&context);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Context_nCalculateRelativeDirection(JNIEnv* env, jclass, jlong peer,
                                                              jfloat sourceX, jfloat sourceY, jfloat sourceZ,
                                                              jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                              jfloat aheadX, jfloat aheadY, jfloat aheadZ,
                                                              jfloat upX, jfloat upY, jfloat upZ,
                                                              jfloatArray out) {
    IPLVector3 relative = iplCalculateRelativeDirection(sajni::asContext(peer),
                                                        IPLVector3{sourceX, sourceY, sourceZ},
                                                        IPLVector3{listenerX, listenerY, listenerZ},
                                                        IPLVector3{aheadX, aheadY, aheadZ},
                                                        IPLVector3{upX, upY, upZ});

    jfloat values[3] = {relative.x, relative.y, relative.z};
    env->SetFloatArrayRegion(out, 0, 3, values);
}

#ifdef __cplusplus
} // extern "C"
#endif
