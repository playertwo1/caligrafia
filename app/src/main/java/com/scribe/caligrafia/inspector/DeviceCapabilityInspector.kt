package com.scribe.caligrafia.inspector

import android.content.Context
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.inspector.model.DeviceCapabilities
import com.scribe.caligrafia.inspector.model.InputDeviceInfo
import com.scribe.caligrafia.inspector.model.LiveProbeSample
import com.scribe.caligrafia.inspector.model.MotionRangeInfo

object DeviceCapabilityInspector {

    fun inspect(context: Context): DeviceCapabilities {
        val deviceList = mutableListOf<InputDeviceInfo>()
        var globalPressureRange: MotionRangeInfo? = null
        var globalTiltRange: MotionRangeInfo? = null
        var globalOrientationRange: MotionRangeInfo? = null
        var globalDistanceRange: MotionRangeInfo? = null

        val deviceIds = try {
            InputDevice.getDeviceIds() ?: intArrayOf()
        } catch (_: Throwable) {
            intArrayOf()
        }

        for (id in deviceIds) {
            val device = try {
                InputDevice.getDevice(id)
            } catch (_: Throwable) {
                null
            } ?: continue

            val sources = extractSources(device.sources)
            val hasStylus = (device.sources and InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS ||
                    (device.sources and InputDevice.SOURCE_BLUETOOTH_STYLUS) == InputDevice.SOURCE_BLUETOOTH_STYLUS

            val motionRanges = mutableListOf<MotionRangeInfo>()
            val axesToCheck = listOf(
                MotionEvent.AXIS_X to "AXIS_X",
                MotionEvent.AXIS_Y to "AXIS_Y",
                MotionEvent.AXIS_PRESSURE to "AXIS_PRESSURE",
                MotionEvent.AXIS_TILT to "AXIS_TILT",
                MotionEvent.AXIS_ORIENTATION to "AXIS_ORIENTATION",
                MotionEvent.AXIS_DISTANCE to "AXIS_DISTANCE"
            )

            for ((axis, name) in axesToCheck) {
                val range = try {
                    device.getMotionRange(axis)
                } catch (_: Throwable) {
                    null
                }
                if (range != null) {
                    val info = MotionRangeInfo(
                        axisName = name,
                        min = range.min,
                        max = range.max,
                        flat = range.flat,
                        fuzz = range.fuzz,
                        resolution = range.resolution
                    )
                    motionRanges.add(info)

                    if (hasStylus || globalPressureRange == null) {
                        when (axis) {
                            MotionEvent.AXIS_PRESSURE -> globalPressureRange = info
                            MotionEvent.AXIS_TILT -> globalTiltRange = info
                            MotionEvent.AXIS_ORIENTATION -> globalOrientationRange = info
                            MotionEvent.AXIS_DISTANCE -> globalDistanceRange = info
                        }
                    }
                }
            }

            deviceList.add(
                InputDeviceInfo(
                    id = device.id,
                    name = device.name ?: "Unknown Device",
                    descriptor = try { device.descriptor ?: "" } catch (_: Throwable) { "" },
                    isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try { device.isExternal } catch (_: Throwable) { false }
                    } else {
                        false
                    },
                    isVirtual = try { device.isVirtual } catch (_: Throwable) { false },
                    sources = sources,
                    hasStylusSource = hasStylus,
                    motionRanges = motionRanges
                )
            )
        }

        val stylusDevices = deviceList.filter { it.hasStylusSource }
        val samsungAssessment = assessSamsung(context)
        val inkApiAssessment = assessInkApi(context)

        return DeviceCapabilities(
            devices = deviceList,
            stylusDevicesCount = stylusDevices.size,
            hasStylusHardware = stylusDevices.isNotEmpty(),
            pressureRange = globalPressureRange,
            tiltRange = globalTiltRange,
            orientationRange = globalOrientationRange,
            distanceRange = globalDistanceRange,
            samsungAssessment = samsungAssessment,
            inkApiAssessment = inkApiAssessment
        )
    }

    fun assessSamsung(context: Context): com.scribe.caligrafia.inspector.model.SamsungSdkAssessment {
        val manufacturer = android.os.Build.MANUFACTURER ?: "Unknown"
        val model = android.os.Build.MODEL ?: "Unknown"
        val isSamsung = manufacturer.equals("samsung", ignoreCase = true)
        val isS25Ultra = isSamsung && (model.contains("S938", ignoreCase = true) || model.contains("S25 Ultra", ignoreCase = true))

        val hasSpenFeature = try {
            context.packageManager.hasSystemFeature("com.sec.feature.spen_usp") ||
                    context.packageManager.hasSystemFeature("com.sec.feature.spen")
        } catch (_: Throwable) {
            false
        }

        val notes = if (isS25Ultra) {
            "Aparelho-alvo Galaxy S25 Ultra identificado. A S Pen original é passiva (EMR Wacom sem Bluetooth/bateria). S Pen Remote SDK não é aplicável. Samsung Pen SDK é legado/descontinuado. Captura deve usar MotionEvent nativo / Android Ink API via adapter."
        } else if (isSamsung) {
            "Dispositivo Samsung identificado. S Pen Remote SDK não serve para captura de traços. Captura de tinta via MotionEvent / Android Ink API."
        } else {
            "Dispositivo não-Samsung (emulador ou outro fabricante). APIs padrão Android de Stylus ativas."
        }

        return com.scribe.caligrafia.inspector.model.SamsungSdkAssessment(
            manufacturer = manufacturer,
            model = model,
            isSamsungDevice = isSamsung,
            isTargetS25Ultra = isS25Ultra,
            hasSpenFeature = hasSpenFeature,
            spenRemoteSdkApplicable = false,
            samsungPenSdkApplicable = false,
            notes = notes
        )
    }

    fun assessInkApi(context: Context): com.scribe.caligrafia.inspector.model.InkApiRequirementsAssessment {
        val apiLevel = android.os.Build.VERSION.SDK_INT
        val minSdkMet = apiLevel >= 26

        val actManager = try {
            context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        } catch (_: Throwable) {
            null
        }
        val glEsVersion = actManager?.deviceConfigurationInfo?.glEsVersion ?: "Desconhecido"
        val isReady = minSdkMet

        val notes = if (isReady) {
            "Requisitos atendidos (API $apiLevel >= 26, GLES $glEsVersion). Pronto para prototipagem da Android Ink API no SCR-004."
        } else {
            "API Level insuficiente para Ink API (requer API 26+, atual $apiLevel)."
        }

        return com.scribe.caligrafia.inspector.model.InkApiRequirementsAssessment(
            minSdkMet = minSdkMet,
            deviceApiLevel = apiLevel,
            isHardwareAccelerated = true,
            openGlEsVersion = glEsVersion,
            isReadyForPrototyping = isReady,
            notes = notes
        )
    }

    /**
     * Extrai uma amostra sem inferir ausência de sensor a partir do valor lido.
     *
     * Pressão/tilt/orientação podem legitimamente valer 0.0. A disponibilidade é decidida pela
     * presença do MotionRange no InputDevice; se não houver device/range, o campo fica null e a UI
     * deve apresentar "a verificar/indisponível", nunca sintetizar suporte pelo valor.
     */
    fun extractSample(event: MotionEvent, pointerIndex: Int = 0): LiveProbeSample {
        val safeIndex = if (pointerIndex in 0 until event.pointerCount) pointerIndex else 0
        val tool = ToolType.fromMotionEvent(event.getToolType(safeIndex))
        val actionName = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            MotionEvent.ACTION_HOVER_ENTER -> "ACTION_HOVER_ENTER"
            MotionEvent.ACTION_HOVER_MOVE -> "ACTION_HOVER_MOVE"
            MotionEvent.ACTION_HOVER_EXIT -> "ACTION_HOVER_EXIT"
            MotionEvent.ACTION_POINTER_DOWN -> "ACTION_POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"
            else -> "ACTION_${event.actionMasked}"
        }

        val device = try { event.device } catch (_: Throwable) { null }
        fun hasAxis(axis: Int): Boolean {
            if (device == null) return false
            return try {
                device.getMotionRange(axis, event.source) != null || device.getMotionRange(axis) != null
            } catch (_: Throwable) {
                false
            }
        }

        val pressure = if (hasAxis(MotionEvent.AXIS_PRESSURE)) event.getPressure(safeIndex) else null
        val tiltRad = if (hasAxis(MotionEvent.AXIS_TILT)) {
            event.getAxisValue(MotionEvent.AXIS_TILT, safeIndex)
        } else null
        val orientationRad = if (hasAxis(MotionEvent.AXIS_ORIENTATION)) {
            event.getOrientation(safeIndex)
        } else null
        val distance = if (hasAxis(MotionEvent.AXIS_DISTANCE)) {
            event.getAxisValue(MotionEvent.AXIS_DISTANCE, safeIndex)
        } else null

        return LiveProbeSample(
            action = actionName,
            toolType = tool,
            pointerId = event.getPointerId(safeIndex),
            x = event.getX(safeIndex),
            y = event.getY(safeIndex),
            pressure = pressure,
            tiltRad = tiltRad,
            orientationRad = orientationRad,
            distance = distance,
            historicalCount = event.historySize,
            buttonState = event.buttonState,
            timestampMs = event.eventTime
        )
    }

    private fun extractSources(sourceBits: Int): List<String> {
        val sources = mutableListOf<String>()
        if ((sourceBits and InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS) sources.add("STYLUS")
        if ((sourceBits and InputDevice.SOURCE_BLUETOOTH_STYLUS) == InputDevice.SOURCE_BLUETOOTH_STYLUS) sources.add("BLUETOOTH_STYLUS")
        if ((sourceBits and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) sources.add("TOUCHSCREEN")
        if ((sourceBits and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD) sources.add("TOUCHPAD")
        if ((sourceBits and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) sources.add("MOUSE")
        if ((sourceBits and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) sources.add("KEYBOARD")
        if (sources.isEmpty()) sources.add("UNKNOWN_SOURCE ($sourceBits")
        return sources
    }
}
