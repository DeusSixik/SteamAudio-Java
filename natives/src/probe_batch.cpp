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
Java_net_sixik_steamaudio_simulation_ProbeArray_nGetProbe(JNIEnv* env, jclass, jlong peer, jint index, jfloatArray out) {
    IPLSphere probe = iplProbeArrayGetProbe(static_cast<IPLProbeArray>(sajni::asPointer(peer)),
                                            static_cast<IPLint32>(index));

    jfloat values[4] = {probe.center.x, probe.center.y, probe.center.z, probe.radius};
    env->SetFloatArrayRegion(out, 0, 4, values);
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

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nGetDataSize(JNIEnv*, jclass, jlong peer, jint dataType, jint variation) {
    IPLBakedDataIdentifier identifier{};
    identifier.type = static_cast<IPLBakedDataType>(dataType);
    identifier.variation = static_cast<IPLBakedDataVariation>(variation);
    identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    identifier.endpointInfluence.radius = 0.0f;

    IPLsize size = iplProbeBatchGetDataSize(static_cast<IPLProbeBatch>(sajni::asPointer(peer)), &identifier);
    return static_cast<jlong>(size);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nRemoveData(JNIEnv*, jclass, jlong peer, jint dataType, jint variation) {
    IPLBakedDataIdentifier identifier{};
    identifier.type = static_cast<IPLBakedDataType>(dataType);
    identifier.variation = static_cast<IPLBakedDataVariation>(variation);
    identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    identifier.endpointInfluence.radius = 0.0f;

    iplProbeBatchRemoveData(static_cast<IPLProbeBatch>(sajni::asPointer(peer)), &identifier);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nGetReverb(JNIEnv* env, jclass, jlong peer, jint dataType,
                                                           jint variation, jint probeIndex, jfloatArray out) {
    IPLBakedDataIdentifier identifier{};
    identifier.type = static_cast<IPLBakedDataType>(dataType);
    identifier.variation = static_cast<IPLBakedDataVariation>(variation);
    identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    identifier.endpointInfluence.radius = 0.0f;

    IPLfloat32 reverbTimes[IPL_NUM_BANDS];
    iplProbeBatchGetReverb(static_cast<IPLProbeBatch>(sajni::asPointer(peer)), &identifier,
                           static_cast<IPLint32>(probeIndex), reverbTimes);

    env->SetFloatArrayRegion(out, 0, IPL_NUM_BANDS, reverbTimes);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_simulation_ProbeBatch_nGetEnergyField(JNIEnv*, jclass, jlong peer, jint dataType,
                                                                jint variation, jint probeIndex,
                                                                jlong energyFieldPeer) {
    IPLBakedDataIdentifier identifier{};
    identifier.type = static_cast<IPLBakedDataType>(dataType);
    identifier.variation = static_cast<IPLBakedDataVariation>(variation);
    identifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    identifier.endpointInfluence.radius = 0.0f;

    iplProbeBatchGetEnergyField(static_cast<IPLProbeBatch>(sajni::asPointer(peer)), &identifier,
                                static_cast<IPLint32>(probeIndex),
                                static_cast<IPLEnergyField>(sajni::asPointer(energyFieldPeer)));
}

#ifdef __cplusplus
} // extern "C"
#endif
