#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_core_SerializedObject_nCreate(JNIEnv* env, jclass, jlong contextPeer) {
    IPLSerializedObjectSettings settings{};
    settings.data = nullptr;
    settings.size = 0;

    IPLSerializedObject serializedObject = nullptr;
    IPLerror status = iplSerializedObjectCreate(sajni::asContext(contextPeer), &settings, &serializedObject);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(serializedObject);
}

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_core_SerializedObject_nGetSize(JNIEnv*, jclass, jlong peer) {
    IPLsize size = iplSerializedObjectGetSize(static_cast<IPLSerializedObject>(sajni::asPointer(peer)));
    return static_cast<jlong>(size);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_SerializedObject_nGetData(JNIEnv* env, jclass, jlong peer, jbyteArray out) {
    auto serializedObject = static_cast<IPLSerializedObject>(sajni::asPointer(peer));

    IPLsize size = iplSerializedObjectGetSize(serializedObject);
    IPLbyte* data = iplSerializedObjectGetData(serializedObject);

    if (data != nullptr && size > 0) {
        env->SetByteArrayRegion(out, 0, static_cast<jsize>(size),
                                reinterpret_cast<const jbyte*>(data));
    }
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_core_SerializedObject_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLSerializedObject serializedObject = static_cast<IPLSerializedObject>(sajni::asPointer(peer));
    iplSerializedObjectRelease(&serializedObject);
}

#ifdef __cplusplus
} // extern "C"
#endif
