package com.scribe.caligrafia.inspector

import android.view.MotionEvent
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.inspector.model.DeviceCapabilities
import com.scribe.caligrafia.inspector.model.MotionRangeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityInspectorTest {

    @Test
    fun testToolTypeMappingFromMotionEvent() {
        assertEquals(ToolType.STYLUS, ToolType.fromMotionEvent(MotionEvent.TOOL_TYPE_STYLUS))
        assertEquals(ToolType.ERASER, ToolType.fromMotionEvent(MotionEvent.TOOL_TYPE_ERASER))
        assertEquals(ToolType.FINGER, ToolType.fromMotionEvent(MotionEvent.TOOL_TYPE_FINGER))
        assertEquals(ToolType.MOUSE, ToolType.fromMotionEvent(MotionEvent.TOOL_TYPE_MOUSE))
        assertEquals(ToolType.UNKNOWN, ToolType.fromMotionEvent(MotionEvent.TOOL_TYPE_UNKNOWN))
        assertEquals(ToolType.UNKNOWN, ToolType.fromMotionEvent(999))
    }

    @Test
    fun testDeviceCapabilitiesStylusDetection() {
        val defaultSamsung = com.scribe.caligrafia.inspector.model.SamsungSdkAssessment(
            manufacturer = "Google",
            model = "Pixel",
            isSamsungDevice = false,
            isTargetS25Ultra = false,
            hasSpenFeature = false,
            spenRemoteSdkApplicable = false,
            samsungPenSdkApplicable = false,
            notes = "Dispositivo de teste"
        )
        val defaultInkApi = com.scribe.caligrafia.inspector.model.InkApiRequirementsAssessment(
            minSdkMet = true,
            deviceApiLevel = 35,
            isHardwareAccelerated = true,
            openGlEsVersion = "3.2",
            isReadyForPrototyping = true,
            notes = "Pronto"
        )

        val emptyCapabilities = DeviceCapabilities(
            devices = emptyList(),
            stylusDevicesCount = 0,
            hasStylusHardware = false,
            pressureRange = null,
            tiltRange = null,
            orientationRange = null,
            distanceRange = null,
            samsungAssessment = defaultSamsung,
            inkApiAssessment = defaultInkApi
        )

        assertFalse(emptyCapabilities.hasStylusHardware)
        assertEquals(0, emptyCapabilities.stylusDevicesCount)
        assertNull(emptyCapabilities.pressureRange)
        assertNull(emptyCapabilities.tiltRange)
        assertNull(emptyCapabilities.orientationRange)
    }

    @Test
    fun testDeviceCapabilitiesWithStylusHardware() {
        val pressureRange = MotionRangeInfo(
            axisName = "AXIS_PRESSURE",
            min = 0.0f,
            max = 1.0f,
            flat = 0.0f,
            fuzz = 0.0f,
            resolution = 0.001f
        )
        val tiltRange = MotionRangeInfo(
            axisName = "AXIS_TILT",
            min = 0.0f,
            max = 1.57f,
            flat = 0.0f,
            fuzz = 0.0f,
            resolution = 0.01f
        )

        val s25Assessment = com.scribe.caligrafia.inspector.model.SamsungSdkAssessment(
            manufacturer = "samsung",
            model = "SM-S938B",
            isSamsungDevice = true,
            isTargetS25Ultra = true,
            hasSpenFeature = true,
            spenRemoteSdkApplicable = false,
            samsungPenSdkApplicable = false,
            notes = "Galaxy S25 Ultra"
        )
        val inkApiAssessment = com.scribe.caligrafia.inspector.model.InkApiRequirementsAssessment(
            minSdkMet = true,
            deviceApiLevel = 35,
            isHardwareAccelerated = true,
            openGlEsVersion = "3.2",
            isReadyForPrototyping = true,
            notes = "Pronto para prototipagem"
        )

        val capabilities = DeviceCapabilities(
            devices = emptyList(),
            stylusDevicesCount = 1,
            hasStylusHardware = true,
            pressureRange = pressureRange,
            tiltRange = tiltRange,
            orientationRange = null,
            distanceRange = null,
            samsungAssessment = s25Assessment,
            inkApiAssessment = inkApiAssessment
        )

        assertTrue(capabilities.hasStylusHardware)
        assertEquals(1, capabilities.stylusDevicesCount)
        assertEquals(0.0f, capabilities.pressureRange?.min)
        assertEquals(1.0f, capabilities.pressureRange?.max)
        assertEquals(1.57f, capabilities.tiltRange?.max)
        assertNull(capabilities.orientationRange)
        assertTrue(capabilities.samsungAssessment.isSamsungDevice)
        assertTrue(capabilities.samsungAssessment.isTargetS25Ultra)
        assertFalse(capabilities.samsungAssessment.spenRemoteSdkApplicable)
        assertTrue(capabilities.inkApiAssessment.isReadyForPrototyping)
    }

}
