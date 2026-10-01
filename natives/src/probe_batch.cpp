#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_ProbeArray_nCreate(JNIEnv* env, jclass, jlong contextPeer) {
    IPLProbeArray probeArray = nullptr;
    IPLerror status = iplProbeArrayCreate(sajni::asContext(contextPeer), &probeArray);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(probeArray);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeArray_nGenerateProbes(JNIEnv* env, jclass, jlong peer, jlong scenePeer, jint type,
                                                     jfloat spacing, jfloat height, jfloatArray transform) {
    IPLProbeGenerationParams params{};
    params.type = static_cast<IPLProbeGenerationType>(type);
    params.spacing = static_cast<IPLfloat32>(spacing);
    params.height = static_cast<IPLfloat32>(height);

    if (transform != nullptr) {
        jfloat* transformData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(transform, nullptr));
        if (transformData == nullptr) {
            sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
            return;
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                params.transform.elements[row][col] = transformData[row * 4 + col];
            }
        }
        env->ReleasePrimitiveArrayCritical(transform, transformData, 0);
    } else {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                params.transform.elements[row][col] = (row == col) ? 1.0f : 0.0f;
            }
        }
    }

    iplProbeArrayGenerateProbes(static_cast<IPLProbeArray>(sajni::asPointer(peer)),
                                static_cast<IPLScene>(sajni::asPointer(scenePeer)), &params);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_simulation_ProbeArray_nGetNumProbes(JNIEnv*, jclass, jlong peer) {
    IPLint32 numProbes = iplProbeArrayGetNumProbes(static_cast<IPLProbeArray>(sajni::asPointer(peer)));
    return static_cast<jint>(numProbes);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeArray_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLProbeArray probeArray = static_cast<IPLProbeArray>(sajni::asPointer(peer));
    iplProbeArrayRelease(&probeArray);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nCreate(JNIEnv* env, jclass, jlong contextPeer) {
    IPLProbeBatch probeBatch = nullptr;
    IPLerror status = iplProbeBatchCreate(sajni::asContext(contextPeer), &probeBatch);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(probeBatch);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nAddProbeArray(JNIEnv*, jclass, jlong peer, jlong probeArrayPeer) {
    iplProbeBatchAddProbeArray(static_cast<IPLProbeBatch>(sajni::asPointer(peer)),
                               static_cast<IPLProbeArray>(sajni::asPointer(probeArrayPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nAddProbe(JNIEnv*, jclass, jlong peer,
                                               jfloat centerX, jfloat centerY, jfloat centerZ, jfloat radius) {
    IPLSphere probe{};
    probe.center = IPLVector3{centerX, centerY, centerZ};
    probe.radius = static_cast<IPLfloat32>(radius);

    iplProbeBatchAddProbe(static_cast<IPLProbeBatch>(sajni::asPointer(peer)), probe);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nCommit(JNIEnv*, jclass, jlong peer) {
    iplProbeBatchCommit(static_cast<IPLProbeBatch>(sajni::asPointer(peer)));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nGetNumProbes(JNIEnv*, jclass, jlong peer) {
    IPLint32 numProbes = iplProbeBatchGetNumProbes(static_cast<IPLProbeBatch>(sajni::asPointer(peer)));
    return static_cast<jint>(numProbes);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nSave(JNIEnv*, jclass, jlong peer, jlong destinationPeer) {
    iplProbeBatchSave(static_cast<IPLProbeBatch>(sajni::asPointer(peer)),
                      static_cast<IPLSerializedObject>(sajni::asPointer(destinationPeer)));
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nLoad(JNIEnv* env, jclass, jlong contextPeer, jlong sourcePeer) {
    IPLProbeBatch probeBatch = nullptr;
    IPLerror status = iplProbeBatchLoad(sajni::asContext(contextPeer),
                                        static_cast<IPLSerializedObject>(sajni::asPointer(sourcePeer)),
                                        &probeBatch);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(probeBatch);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLProbeBatch probeBatch = static_cast<IPLProbeBatch>(sajni::asPointer(peer));
    iplProbeBatchRelease(&probeBatch);
}

#ifdef __cplusplus
} // extern "C"
#endif
