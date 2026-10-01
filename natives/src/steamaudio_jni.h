#ifndef STEAMAUDIO_JNI_H
#define STEAMAUDIO_JNI_H

#include <jni.h>
#include <cstdint>
#include <cstdio>

#include <phonon.h>

namespace sajni {

/**
 * Converts a Java long (opaque handle) to a native pointer.
 */
inline void* asPointer(jlong peer) {
    return reinterpret_cast<void*>(static_cast<std::uintptr_t>(peer));
}

/**
 * Converts a native pointer to a Java long (opaque handle).
 */
inline jlong asPeer(const void* pointer) {
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(pointer));
}

/**
 * Converts a Java long (opaque handle) to an IPLContext pointer.
 */
inline IPLContext asContext(jlong peer) {
    return static_cast<IPLContext>(asPointer(peer));
}

/**
 * Converts an IPLContext pointer to a Java long (opaque handle).
 */
inline jlong asPeer(IPLContext context) {
    return asPeer(static_cast<const void*>(context));
}

/**
 * Returns a human-readable description of an IPLerror status code.
 */
inline const char* describeError(IPLerror status) {
    switch (status) {
        case IPL_STATUS_SUCCESS:      return "success";
        case IPL_STATUS_OUTOFMEMORY:  return "out of memory";
        case IPL_STATUS_INITIALIZATION: return "initialization of an external dependency failed";
        case IPL_STATUS_FAILURE:      return "unspecified failure";
        default:                      return "unknown error";
    }
}

/**
 * Throws net.sixik.steamaudio.SteamAudioException with a message describing
 * the given IPLerror status. Pending exception is left to be handled by the JVM.
 */
inline void throwSteamAudioException(JNIEnv* env, IPLerror status) {
    jclass cls = env->FindClass("net/sixik/steamaudio/SteamAudioException");
    if (cls == nullptr) {
        return;
    }
    char message[128];
    std::snprintf(message, sizeof(message), "Steam Audio error %d: %s",
                  static_cast<int>(status), describeError(status));
    env->ThrowNew(cls, message);
    env->DeleteLocalRef(cls);
}

/**
 * Builds an IPLVector3 from three consecutive floats.
 */
inline IPLVector3 makeVector3(const jfloat* data) {
    return IPLVector3{data[0], data[1], data[2]};
}

/**
 * Builds an IPLCoordinateSpace3 from a Java-side layout of 9 floats:
 * [0..2] = origin (position), [3..5] = ahead, [6..8] = up.
 * The right basis vector is computed as ahead x up (right-handed system).
 */
inline IPLCoordinateSpace3 makeCoordinateSpace(const jfloat* data) {
    IPLVector3 ahead = IPLVector3{data[3], data[4], data[5]};
    IPLVector3 up = IPLVector3{data[6], data[7], data[8]};

    IPLVector3 right = IPLVector3{
            ahead.y * up.z - ahead.z * up.y,
            ahead.z * up.x - ahead.x * up.z,
            ahead.x * up.y - ahead.y * up.x
    };

    IPLCoordinateSpace3 space{};
    space.right = right;
    space.up = up;
    space.ahead = ahead;
    space.origin = IPLVector3{data[0], data[1], data[2]};
    return space;
}

} // namespace sajni

#endif // STEAMAUDIO_JNI_H
