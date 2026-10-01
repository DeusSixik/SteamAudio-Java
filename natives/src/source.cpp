#include "steamaudio_jni.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_net_sixik_steamaudio_Source_nCreate(JNIEnv* env, jclass, jlong simulatorPeer, jint simulationFlags) {
    IPLSourceSettings settings{};
    settings.flags = static_cast<IPLSimulationFlags>(simulationFlags);

    IPLSource source = nullptr;
    IPLerror status = iplSourceCreate(static_cast<IPLSimulator>(sajni::asPointer(simulatorPeer)),
                                      &settings, &source);
    if (status != IPL_STATUS_SUCCESS) {
        sajni::throwSteamAudioException(env, status);
        return 0;
    }
    return sajni::asPeer(source);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nAdd(JNIEnv*, jclass, jlong peer, jlong simulatorPeer) {
    iplSourceAdd(static_cast<IPLSource>(sajni::asPointer(peer)),
                 static_cast<IPLSimulator>(sajni::asPointer(simulatorPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nRemove(JNIEnv*, jclass, jlong peer, jlong simulatorPeer) {
    iplSourceRemove(static_cast<IPLSource>(sajni::asPointer(peer)),
                    static_cast<IPLSimulator>(sajni::asPointer(simulatorPeer)));
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nSetDirectInputs(JNIEnv* env, jclass, jlong peer, jint directFlags,
                                                  jfloat sourceX, jfloat sourceY, jfloat sourceZ,
                                                  jfloat aheadX, jfloat aheadY, jfloat aheadZ,
                                                  jfloat upX, jfloat upY, jfloat upZ,
                                                  jint occlusionType, jfloat occlusionRadius,
                                                  jint numOcclusionSamples, jint numTransmissionRays) {
    jfloat sourceData[9] = {sourceX, sourceY, sourceZ, aheadX, aheadY, aheadZ, upX, upY, upZ};

    IPLSimulationInputs inputs{};
    inputs.flags = IPL_SIMULATIONFLAGS_DIRECT;
    inputs.directFlags = static_cast<IPLDirectSimulationFlags>(directFlags);
    inputs.source = sajni::makeCoordinateSpace(sourceData);

    // Модель затухания по расстоянию по умолчанию: обратная дистанция
    // с плато 1 м.
    inputs.distanceAttenuationModel.type = IPL_DISTANCEATTENUATIONTYPE_DEFAULT;
    inputs.distanceAttenuationModel.minDistance = 1.0f;
    inputs.distanceAttenuationModel.callback = nullptr;
    inputs.distanceAttenuationModel.userData = nullptr;
    inputs.distanceAttenuationModel.dirty = IPL_FALSE;

    // Модель поглощения воздухом по умолчанию.
    inputs.airAbsorptionModel.type = IPL_AIRABSORPTIONTYPE_DEFAULT;
    inputs.airAbsorptionModel.coefficients[0] = 1.0f;
    inputs.airAbsorptionModel.coefficients[1] = 1.0f;
    inputs.airAbsorptionModel.coefficients[2] = 1.0f;
    inputs.airAbsorptionModel.callback = nullptr;
    inputs.airAbsorptionModel.userData = nullptr;
    inputs.airAbsorptionModel.dirty = IPL_FALSE;

    // Всенаправленный источник (чистый омни, без диполя).
    inputs.directivity.dipoleWeight = 0.0f;
    inputs.directivity.dipolePower = 1.0f;
    inputs.directivity.callback = nullptr;
    inputs.directivity.userData = nullptr;

    inputs.occlusionType = static_cast<IPLOcclusionType>(occlusionType);
    inputs.occlusionRadius = static_cast<IPLfloat32>(occlusionRadius);
    inputs.numOcclusionSamples = static_cast<IPLint32>(numOcclusionSamples);

    inputs.reverbScale[0] = 1.0f;
    inputs.reverbScale[1] = 1.0f;
    inputs.reverbScale[2] = 1.0f;
    inputs.hybridReverbTransitionTime = 1.0f;
    inputs.hybridReverbOverlapPercent = 0.25f;
    inputs.baked = IPL_FALSE;
    inputs.bakedDataIdentifier.type = IPL_BAKEDDATATYPE_REFLECTIONS;
    inputs.bakedDataIdentifier.variation = IPL_BAKEDDATAVARIATION_REVERB;
    inputs.bakedDataIdentifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    inputs.bakedDataIdentifier.endpointInfluence.radius = 0.0f;
    inputs.pathingProbes = nullptr;
    inputs.visRadius = 1.0f;
    inputs.visThreshold = 0.3f;
    inputs.visRange = 10.0f;
    inputs.pathingOrder = 1.0f;
    inputs.enableValidation = IPL_TRUE;
    inputs.findAlternatePaths = IPL_TRUE;
    inputs.numTransmissionRays = static_cast<IPLint32>(numTransmissionRays);
    inputs.deviationModel = nullptr;

    iplSourceSetInputs(static_cast<IPLSource>(sajni::asPointer(peer)),
                       IPL_SIMULATIONFLAGS_DIRECT, &inputs);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nSetReflectionsInputs(JNIEnv* env, jclass, jlong peer, jfloatArray reverbScale) {
    jfloat* scaleData = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(reverbScale, nullptr));
    if (scaleData == nullptr) {
        sajni::throwSteamAudioException(env, IPL_STATUS_OUTOFMEMORY);
        return;
    }

    IPLSimulationInputs inputs{};
    inputs.flags = IPL_SIMULATIONFLAGS_REFLECTIONS;
    inputs.directFlags = static_cast<IPLDirectSimulationFlags>(0);
    inputs.source = IPLCoordinateSpace3{};
    inputs.distanceAttenuationModel.type = IPL_DISTANCEATTENUATIONTYPE_DEFAULT;
    inputs.distanceAttenuationModel.minDistance = 1.0f;
    inputs.distanceAttenuationModel.callback = nullptr;
    inputs.distanceAttenuationModel.userData = nullptr;
    inputs.distanceAttenuationModel.dirty = IPL_FALSE;
    inputs.airAbsorptionModel.type = IPL_AIRABSORPTIONTYPE_DEFAULT;
    inputs.airAbsorptionModel.coefficients[0] = 1.0f;
    inputs.airAbsorptionModel.coefficients[1] = 1.0f;
    inputs.airAbsorptionModel.coefficients[2] = 1.0f;
    inputs.airAbsorptionModel.callback = nullptr;
    inputs.airAbsorptionModel.userData = nullptr;
    inputs.airAbsorptionModel.dirty = IPL_FALSE;
    inputs.directivity.dipoleWeight = 0.0f;
    inputs.directivity.dipolePower = 1.0f;
    inputs.directivity.callback = nullptr;
    inputs.directivity.userData = nullptr;
    inputs.occlusionType = IPL_OCCLUSIONTYPE_RAYCAST;
    inputs.occlusionRadius = 0.0f;
    inputs.numOcclusionSamples = 0;
    inputs.reverbScale[0] = scaleData[0];
    inputs.reverbScale[1] = scaleData[1];
    inputs.reverbScale[2] = scaleData[2];
    inputs.hybridReverbTransitionTime = 1.0f;
    inputs.hybridReverbOverlapPercent = 0.25f;
    inputs.baked = IPL_FALSE;
    inputs.bakedDataIdentifier.type = IPL_BAKEDDATATYPE_REFLECTIONS;
    inputs.bakedDataIdentifier.variation = IPL_BAKEDDATAVARIATION_REVERB;
    inputs.bakedDataIdentifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    inputs.bakedDataIdentifier.endpointInfluence.radius = 0.0f;
    inputs.pathingProbes = nullptr;
    inputs.visRadius = 1.0f;
    inputs.visThreshold = 0.3f;
    inputs.visRange = 10.0f;
    inputs.pathingOrder = 1.0f;
    inputs.enableValidation = IPL_TRUE;
    inputs.findAlternatePaths = IPL_TRUE;
    inputs.numTransmissionRays = 0;
    inputs.deviationModel = nullptr;

    iplSourceSetInputs(static_cast<IPLSource>(sajni::asPointer(peer)),
                       IPL_SIMULATIONFLAGS_REFLECTIONS, &inputs);

    env->ReleasePrimitiveArrayCritical(reverbScale, scaleData, 0);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nSetPathingInputs(JNIEnv* env, jclass, jlong peer, jlong probeBatchPeer,
                                                   jint pathingOrder, jfloat visRadius, jfloat visThreshold,
                                                   jfloat visRange) {
    IPLSimulationInputs inputs{};
    inputs.flags = IPL_SIMULATIONFLAGS_PATHING;
    inputs.directFlags = static_cast<IPLDirectSimulationFlags>(0);
    inputs.source = IPLCoordinateSpace3{};
    inputs.distanceAttenuationModel.type = IPL_DISTANCEATTENUATIONTYPE_DEFAULT;
    inputs.distanceAttenuationModel.minDistance = 1.0f;
    inputs.distanceAttenuationModel.callback = nullptr;
    inputs.distanceAttenuationModel.userData = nullptr;
    inputs.distanceAttenuationModel.dirty = IPL_FALSE;
    inputs.airAbsorptionModel.type = IPL_AIRABSORPTIONTYPE_DEFAULT;
    inputs.airAbsorptionModel.coefficients[0] = 1.0f;
    inputs.airAbsorptionModel.coefficients[1] = 1.0f;
    inputs.airAbsorptionModel.coefficients[2] = 1.0f;
    inputs.airAbsorptionModel.callback = nullptr;
    inputs.airAbsorptionModel.userData = nullptr;
    inputs.airAbsorptionModel.dirty = IPL_FALSE;
    inputs.directivity.dipoleWeight = 0.0f;
    inputs.directivity.dipolePower = 1.0f;
    inputs.directivity.callback = nullptr;
    inputs.directivity.userData = nullptr;
    inputs.occlusionType = IPL_OCCLUSIONTYPE_RAYCAST;
    inputs.occlusionRadius = 0.0f;
    inputs.numOcclusionSamples = 0;
    inputs.reverbScale[0] = 1.0f;
    inputs.reverbScale[1] = 1.0f;
    inputs.reverbScale[2] = 1.0f;
    inputs.hybridReverbTransitionTime = 1.0f;
    inputs.hybridReverbOverlapPercent = 0.25f;

    // Pathing использует запеченные в probe batch данные.
    inputs.baked = IPL_TRUE;
    inputs.bakedDataIdentifier.type = IPL_BAKEDDATATYPE_PATHING;
    inputs.bakedDataIdentifier.variation = IPL_BAKEDDATAVARIATION_DYNAMIC;
    inputs.bakedDataIdentifier.endpointInfluence.center = IPLVector3{0.0f, 0.0f, 0.0f};
    inputs.bakedDataIdentifier.endpointInfluence.radius = 0.0f;
    inputs.pathingProbes = static_cast<IPLProbeBatch>(sajni::asPointer(probeBatchPeer));
    inputs.visRadius = static_cast<IPLfloat32>(visRadius);
    inputs.visThreshold = static_cast<IPLfloat32>(visThreshold);
    inputs.visRange = static_cast<IPLfloat32>(visRange);
    inputs.pathingOrder = static_cast<IPLint32>(pathingOrder);
    inputs.enableValidation = IPL_TRUE;
    inputs.findAlternatePaths = IPL_TRUE;
    inputs.numTransmissionRays = 0;
    inputs.deviationModel = nullptr;

    iplSourceSetInputs(static_cast<IPLSource>(sajni::asPointer(peer)),
                       IPL_SIMULATIONFLAGS_PATHING, &inputs);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nGetDirectOutputs(JNIEnv* env, jclass, jlong peer, jfloatArray out) {
    IPLSimulationOutputs outputs{};
    iplSourceGetOutputs(static_cast<IPLSource>(sajni::asPointer(peer)),
                        IPL_SIMULATIONFLAGS_DIRECT, &outputs);

    jfloat values[9];
    values[0] = outputs.direct.distanceAttenuation;
    values[1] = outputs.direct.airAbsorption[0];
    values[2] = outputs.direct.airAbsorption[1];
    values[3] = outputs.direct.airAbsorption[2];
    values[4] = outputs.direct.directivity;
    values[5] = outputs.direct.occlusion;
    values[6] = outputs.direct.transmission[0];
    values[7] = outputs.direct.transmission[1];
    values[8] = outputs.direct.transmission[2];

    env->SetFloatArrayRegion(out, 0, 9, values);
}

JNIEXPORT void JNICALL
Java_net_sixik_steamaudio_Source_nRelease(JNIEnv*, jclass, jlong peer) {
    IPLSource source = static_cast<IPLSource>(sajni::asPointer(peer));
    iplSourceRelease(&source);
}

#ifdef __cplusplus
} // extern "C"
#endif
