#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nCreateStaticMesh(JNIEnv* env, jclass, jlong scenePeer,
                                                  jint numVertices, jint numTriangles, jint numMaterials,
                                                  jobject vertices, jobject triangles,
                                                  jobject materialIndices, jobject materials) {
    auto vertexData = static_cast<IPLVector3*>(env->GetDirectBufferAddress(vertices));
    auto triangleData = static_cast<IPLTriangle*>(env->GetDirectBufferAddress(triangles));
    auto materialIndexData = static_cast<IPLint32*>(env->GetDirectBufferAddress(materialIndices));
    auto materialData = static_cast<IPLMaterial*>(env->GetDirectBufferAddress(materials));

    if (vertexData == nullptr || triangleData == nullptr
            || materialIndexData == nullptr || materialData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_FAILURE);
        return 0;
    }

    IPLStaticMeshSettings settings{};
    settings.numVertices = static_cast<IPLint32>(numVertices);
    settings.numTriangles = static_cast<IPLint32>(numTriangles);
    settings.numMaterials = static_cast<IPLint32>(numMaterials);
    settings.vertices = vertexData;
    settings.triangles = triangleData;
    settings.materialIndices = materialIndexData;
    settings.materials = materialData;

    IPLStaticMesh staticMesh = nullptr;
    IPLerror status = iplStaticMeshCreate(static_cast<IPLScene>(sajni::asPointer(scenePeer)),
                                          &settings, &staticMesh);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(staticMesh);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nSetMaterial(JNIEnv* env, jclass, jlong peer, jlong scenePeer,
                                                           jobject newMaterial, jint index) {
    auto materialData = static_cast<IPLMaterial*>(env->GetDirectBufferAddress(newMaterial));
    if (materialData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_FAILURE);
        return;
    }

    iplStaticMeshSetMaterial(static_cast<IPLStaticMesh>(sajni::asPointer(peer)),
                             static_cast<IPLScene>(sajni::asPointer(scenePeer)),
                             materialData, static_cast<IPLint32>(index));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nSave(JNIEnv*, jclass, jlong peer, jlong destinationPeer) {
    iplStaticMeshSave(static_cast<IPLStaticMesh>(sajni::asPointer(peer)),
                      static_cast<IPLSerializedObject>(sajni::asPointer(destinationPeer)));
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nLoad(JNIEnv* env, jclass, jlong scenePeer, jlong sourcePeer) {
    IPLStaticMesh staticMesh = nullptr;
    IPLerror status = iplStaticMeshLoad(static_cast<IPLScene>(sajni::asPointer(scenePeer)),
                                        static_cast<IPLSerializedObject>(sajni::asPointer(sourcePeer)),
                                        nullptr, nullptr, &staticMesh);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(staticMesh);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nAdd(JNIEnv*, jclass, jlong peer, jlong scenePeer) {
    iplStaticMeshAdd(static_cast<IPLStaticMesh>(sajni::asPointer(peer)),
                     static_cast<IPLScene>(sajni::asPointer(scenePeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nRemove(JNIEnv*, jclass, jlong peer, jlong scenePeer) {
    iplStaticMeshRemove(static_cast<IPLStaticMesh>(sajni::asPointer(peer)),
                        static_cast<IPLScene>(sajni::asPointer(scenePeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_StaticMesh_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLStaticMesh staticMesh = static_cast<IPLStaticMesh>(sajni::asPointer(peer));
    iplStaticMeshRelease(&staticMesh);
}

#ifdef __cplusplus
} // extern "C"
#endif
