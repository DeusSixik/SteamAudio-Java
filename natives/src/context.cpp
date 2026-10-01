#include <cstdio>

#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_core_Context_nCreate(JNIEnv* env, jclass, jint version, jint simdLevel, jint flags) {
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
Java_net_sixik_steamaudio_core_Context_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLContext context = sajni::asContext(peer);
    iplContextRelease(&context);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_Context_nCalculateRelativeDirection(JNIEnv* env, jclass, jlong peer,
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

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_Context_nCalculateDistanceAttenuation(JNIEnv* env, jclass, jlong peer, jint modelType,
                                                                     jfloat minDistance,
                                                                     jfloat sourceX, jfloat sourceY, jfloat sourceZ,
                                                                     jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                                     jfloatArray out) {
    IPLDistanceAttenuationModel model{};
    model.type = static_cast<IPLDistanceAttenuationModelType>(modelType);
    model.minDistance = static_cast<IPLfloat32>(minDistance);
    model.callback = nullptr;
    model.userData = nullptr;
    model.dirty = IPL_FALSE;

    IPLfloat32 attenuation = iplDistanceAttenuationCalculate(sajni::asContext(peer),
                                                             IPLVector3{sourceX, sourceY, sourceZ},
                                                             IPLVector3{listenerX, listenerY, listenerZ},
                                                             &model);

    jfloat values[1] = {attenuation};
    env->SetFloatArrayRegion(out, 0, 1, values);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_Context_nCalculateAirAbsorption(JNIEnv* env, jclass, jlong peer,
                                                               jfloat sourceX, jfloat sourceY, jfloat sourceZ,
                                                               jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                               jfloatArray out) {
    IPLAirAbsorptionModel model{};
    model.type = IPL_AIRABSORPTIONTYPE_DEFAULT;
    model.coefficients[0] = 1.0f;
    model.coefficients[1] = 1.0f;
    model.coefficients[2] = 1.0f;
    model.callback = nullptr;
    model.userData = nullptr;
    model.dirty = IPL_FALSE;

    IPLfloat32 coefficients[IPL_NUM_BANDS];
    iplAirAbsorptionCalculate(sajni::asContext(peer),
                              IPLVector3{sourceX, sourceY, sourceZ},
                              IPLVector3{listenerX, listenerY, listenerZ},
                              &model, coefficients);

    env->SetFloatArrayRegion(out, 0, IPL_NUM_BANDS, coefficients);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_Context_nCalculateDirectivity(JNIEnv* env, jclass, jlong peer,
                                                             jfloat dipoleWeight, jfloat dipolePower,
                                                             jfloat sourceX, jfloat sourceY, jfloat sourceZ,
                                                             jfloat sourceAheadX, jfloat sourceAheadY, jfloat sourceAheadZ,
                                                             jfloat sourceUpX, jfloat sourceUpY, jfloat sourceUpZ,
                                                             jfloat listenerX, jfloat listenerY, jfloat listenerZ,
                                                             jfloatArray out) {
    jfloat sourceData[9] = {sourceX, sourceY, sourceZ,
                            sourceAheadX, sourceAheadY, sourceAheadZ,
                            sourceUpX, sourceUpY, sourceUpZ};

    IPLDirectivity model{};
    model.dipoleWeight = static_cast<IPLfloat32>(dipoleWeight);
    model.dipolePower = static_cast<IPLfloat32>(dipolePower);
    model.callback = nullptr;
    model.userData = nullptr;

    IPLfloat32 directivity = iplDirectivityCalculate(sajni::asContext(peer),
                                                     sajni::makeCoordinateSpace(sourceData),
                                                     IPLVector3{listenerX, listenerY, listenerZ},
                                                     &model);

    jfloat values[1] = {directivity};
    env->SetFloatArrayRegion(out, 0, 1, values);
}

#ifdef __cplusplus
} // extern "C"
#endif
