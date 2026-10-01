#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_InstancedMesh_nCreate(JNIEnv* env, jclass, jlong scenePeer, jlong subScenePeer,
                                                         jfloatArray transform) {
    IPLInstancedMeshSettings settings{};
    settings.subScene = static_cast<IPLScene>(sajni::asPointer(subScenePeer));

    if (transform != nullptr) {
        jfloat* transformData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(transform, nullptr));
        if (transformData == nullptr) {
            sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
            return 0;
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                settings.transform.elements[row][col] = transformData[row * 4 + col];
            }
        }
        env->ReleasePrimitiveArrayCritical(transform, transformData, 0);
    } else {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                settings.transform.elements[row][col] = (row == col) ? 1.0f : 0.0f;
            }
        }
    }

    IPLInstancedMesh instancedMesh = nullptr;
    IPLerror status = iplInstancedMeshCreate(static_cast<IPLScene>(sajni::asPointer(scenePeer)),
                                             &settings, &instancedMesh);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(instancedMesh);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_InstancedMesh_nAdd(JNIEnv*, jclass, jlong peer, jlong scenePeer) {
    iplInstancedMeshAdd(static_cast<IPLInstancedMesh>(sajni::asPointer(peer)),
                        static_cast<IPLScene>(sajni::asPointer(scenePeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_InstancedMesh_nRemove(JNIEnv*, jclass, jlong peer, jlong scenePeer) {
    iplInstancedMeshRemove(static_cast<IPLInstancedMesh>(sajni::asPointer(peer)),
                           static_cast<IPLScene>(sajni::asPointer(scenePeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_InstancedMesh_nUpdateTransform(JNIEnv* env, jclass, jlong peer, jlong scenePeer,
                                                                  jfloatArray transform) {
    IPLMatrix4x4 matrix{};

    jfloat* transformData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(transform, nullptr));
    if (transformData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return;
    }
    for (int row = 0; row < 4; row++) {
        for (int col = 0; col < 4; col++) {
            matrix.elements[row][col] = transformData[row * 4 + col];
        }
    }
    env->ReleasePrimitiveArrayCritical(transform, transformData, 0);

    iplInstancedMeshUpdateTransform(static_cast<IPLInstancedMesh>(sajni::asPointer(peer)),
                                    static_cast<IPLScene>(sajni::asPointer(scenePeer)), matrix);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_InstancedMesh_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLInstancedMesh instancedMesh = static_cast<IPLInstancedMesh>(sajni::asPointer(peer));
    iplInstancedMeshRelease(&instancedMesh);
}

#ifdef __cplusplus
} // extern "C"
#endif
