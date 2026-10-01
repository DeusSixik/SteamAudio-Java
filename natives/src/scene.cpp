#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nCreate(JNIEnv* env, jclass, jlong contextPeer, jint type) {
    IPLSceneSettings settings{};
    settings.type = static_cast<IPLSceneType>(type);
    settings.closestHitCallback = nullptr;
    settings.anyHitCallback = nullptr;
    settings.batchedClosestHitCallback = nullptr;
    settings.batchedAnyHitCallback = nullptr;
    settings.userData = nullptr;
    settings.embreeDevice = nullptr;
    settings.radeonRaysDevice = nullptr;

    IPLScene scene = nullptr;
    IPLerror status = iplSceneCreate(sajni::asContext(contextPeer), &settings, &scene);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(scene);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nCreateWithEmbreeDevice(JNIEnv* env, jclass, jlong contextPeer, jint type,
                                                                 jlong embreeDevicePeer) {
    IPLSceneSettings settings{};
    settings.type = static_cast<IPLSceneType>(type);
    settings.closestHitCallback = nullptr;
    settings.anyHitCallback = nullptr;
    settings.batchedClosestHitCallback = nullptr;
    settings.batchedAnyHitCallback = nullptr;
    settings.userData = nullptr;
    settings.embreeDevice = static_cast<IPLEmbreeDevice>(sajni::asPointer(embreeDevicePeer));
    settings.radeonRaysDevice = nullptr;

    IPLScene scene = nullptr;
    IPLerror status = iplSceneCreate(sajni::asContext(contextPeer), &settings, &scene);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(scene);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nCommit(JNIEnv*, jclass, jlong peer) {
    iplSceneCommit(static_cast<IPLScene>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nSave(JNIEnv*, jclass, jlong peer, jlong destinationPeer) {
    iplSceneSave(static_cast<IPLScene>(sajni::asPointer(peer)),
                 static_cast<IPLSerializedObject>(sajni::asPointer(destinationPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nSaveOBJ(JNIEnv* env, jclass, jlong peer, jstring fileBaseName) {
    const char* fileName = env->GetStringUTFChars(fileBaseName, nullptr);
    if (fileName == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_FAILURE);
        return;
    }

    iplSceneSaveOBJ(static_cast<IPLScene>(sajni::asPointer(peer)), fileName);

    env->ReleaseStringUTFChars(fileBaseName, fileName);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nLoad(JNIEnv* env, jclass, jlong contextPeer, jint type, jlong sourcePeer) {
    IPLSceneSettings settings{};
    settings.type = static_cast<IPLSceneType>(type);
    settings.closestHitCallback = nullptr;
    settings.anyHitCallback = nullptr;
    settings.batchedClosestHitCallback = nullptr;
    settings.batchedAnyHitCallback = nullptr;
    settings.userData = nullptr;
    settings.embreeDevice = nullptr;
    settings.radeonRaysDevice = nullptr;

    IPLScene scene = nullptr;
    IPLerror status = iplSceneLoad(sajni::asContext(contextPeer), &settings,
                                   static_cast<IPLSerializedObject>(sajni::asPointer(sourcePeer)),
                                   nullptr, nullptr, &scene);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(scene);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_geometry_Scene_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLScene scene = static_cast<IPLScene>(sajni::asPointer(peer));
    iplSceneRelease(&scene);
}

#ifdef __cplusplus
} // extern "C"
#endif
