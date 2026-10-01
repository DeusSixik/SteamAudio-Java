#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nCreate(JNIEnv* env, jclass, jlong contextPeer, jfloat duration,
                                                         jint order) {
    IPLEnergyFieldSettings settings{};
    settings.duration = static_cast<IPLfloat32>(duration);
    settings.order = static_cast<IPLint32>(order);

    IPLEnergyField energyField = nullptr;
    IPLerror status = iplEnergyFieldCreate(sajni::asContext(contextPeer), &settings, &energyField);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(energyField);
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nGetNumChannels(JNIEnv*, jclass, jlong peer) {
    return static_cast<jint>(iplEnergyFieldGetNumChannels(static_cast<IPLEnergyField>(sajni::asPointer(peer))));
}

JNIEXPORT jint JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nGetNumBins(JNIEnv*, jclass, jlong peer) {
    return static_cast<jint>(iplEnergyFieldGetNumBins(static_cast<IPLEnergyField>(sajni::asPointer(peer))));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nGetData(JNIEnv* env, jclass, jlong peer, jfloatArray out) {
    auto energyField = static_cast<IPLEnergyField>(sajni::asPointer(peer));

    IPLint32 numChannels = iplEnergyFieldGetNumChannels(energyField);
    IPLint32 numBins = iplEnergyFieldGetNumBins(energyField);
    IPLfloat32* data = iplEnergyFieldGetData(energyField);

    if (data != nullptr && numChannels > 0 && numBins > 0) {
        jsize total = static_cast<jsize>(numChannels * IPL_NUM_BANDS * numBins);
        env->SetFloatArrayRegion(out, 0, total, data);
    }
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nReset(JNIEnv*, jclass, jlong peer) {
    iplEnergyFieldReset(static_cast<IPLEnergyField>(sajni::asPointer(peer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nCopy(JNIEnv*, jclass, jlong srcPeer, jlong dstPeer) {
    iplEnergyFieldCopy(static_cast<IPLEnergyField>(sajni::asPointer(srcPeer)),
                       static_cast<IPLEnergyField>(sajni::asPointer(dstPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nSwap(JNIEnv*, jclass, jlong aPeer, jlong bPeer) {
    iplEnergyFieldSwap(static_cast<IPLEnergyField>(sajni::asPointer(aPeer)),
                       static_cast<IPLEnergyField>(sajni::asPointer(bPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nAdd(JNIEnv*, jclass, jlong in1Peer, jlong in2Peer, jlong outPeer) {
    iplEnergyFieldAdd(static_cast<IPLEnergyField>(sajni::asPointer(in1Peer)),
                      static_cast<IPLEnergyField>(sajni::asPointer(in2Peer)),
                      static_cast<IPLEnergyField>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nScale(JNIEnv*, jclass, jlong inPeer, jfloat scalar, jlong outPeer) {
    iplEnergyFieldScale(static_cast<IPLEnergyField>(sajni::asPointer(inPeer)),
                        static_cast<IPLfloat32>(scalar),
                        static_cast<IPLEnergyField>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nScaleAccum(JNIEnv*, jclass, jlong inPeer, jfloat scalar, jlong outPeer) {
    iplEnergyFieldScaleAccum(static_cast<IPLEnergyField>(sajni::asPointer(inPeer)),
                             static_cast<IPLfloat32>(scalar),
                             static_cast<IPLEnergyField>(sajni::asPointer(outPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_EnergyField_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLEnergyField energyField = static_cast<IPLEnergyField>(sajni::asPointer(peer));
    iplEnergyFieldRelease(&energyField);
}

#ifdef __cplusplus
} // extern "C"
#endif
