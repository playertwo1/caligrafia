package com.scribe.caligrafia.inspector.model

import com.scribe.caligrafia.core.model.ToolType

data class MotionRangeInfo(
    val axisName: String,
    val min: Float,
    val max: Float,
    val flat: Float,
    val fuzz: Float,
    val resolution: Float
)

data class InputDeviceInfo(
    val id: Int,
    val name: String,
    val descriptor: String,
    val isExternal: Boolean,
    val isVirtual: Boolean,
    val sources: List<String>,
    val hasStylusSource: Boolean,
    val motionRanges: List<MotionRangeInfo>
)

data class SamsungSdkAssessment(
    val manufacturer: String,
    val model: String,
    val isSamsungDevice: Boolean,
    val isTargetS25Ultra: Boolean,
    val hasSpenFeature: Boolean,
    val spenRemoteSdkApplicable: Boolean,
    val samsungPenSdkApplicable: Boolean,
    val notes: String
)

data class InkApiRequirementsAssessment(
    val minSdkMet: Boolean,
    val deviceApiLevel: Int,
    val isHardwareAccelerated: Boolean,
    val openGlEsVersion: String,
    val isReadyForPrototyping: Boolean,
    val notes: String
)

data class DeviceCapabilities(
    val devices: List<InputDeviceInfo>,
    val stylusDevicesCount: Int,
    val hasStylusHardware: Boolean,
    val pressureRange: MotionRangeInfo?,
    val tiltRange: MotionRangeInfo?,
    val orientationRange: MotionRangeInfo?,
    val distanceRange: MotionRangeInfo?,
    val samsungAssessment: SamsungSdkAssessment,
    val inkApiAssessment: InkApiRequirementsAssessment
)


data class LiveProbeSample(
    val action: String,
    val toolType: ToolType,
    val pointerId: Int,
    val x: Float,
    val y: Float,
    val pressure: Float?,
    val tiltRad: Float?,
    val orientationRad: Float?,
    val distance: Float?,
    val historicalCount: Int,
    val buttonState: Int,
    val timestampMs: Long
)
