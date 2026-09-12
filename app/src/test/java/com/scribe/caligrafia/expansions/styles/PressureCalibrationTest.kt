package com.scribe.caligrafia.expansions.styles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PressureCalibrationTest {

    @Test
    fun linearCurve_preservesRawPressure() {
        assertEquals(0.0f, PressureCalibration.transform(0.0f, PressureCurveType.LINEAR), 0.001f)
        assertEquals(0.5f, PressureCalibration.transform(0.5f, PressureCurveType.LINEAR), 0.001f)
        assertEquals(1.0f, PressureCalibration.transform(1.0f, PressureCurveType.LINEAR), 0.001f)
    }

    @Test
    fun softCurve_amplifiesLowPressureForLightTouch() {
        val raw = 0.3f
        val transformed = PressureCalibration.transform(raw, PressureCurveType.SOFT)
        assertTrue("Curva suave deve amplificar pressões baixas", transformed > raw)
        assertEquals(0.0f, PressureCalibration.transform(0.0f, PressureCurveType.SOFT), 0.001f)
        assertEquals(1.0f, PressureCalibration.transform(1.0f, PressureCurveType.SOFT), 0.001f)
    }

    @Test
    fun firmCurve_reducesLowPressureForHeavyTouch() {
        val raw = 0.3f
        val transformed = PressureCalibration.transform(raw, PressureCurveType.FIRM)
        assertTrue("Curva firme deve atenuar pressões baixas", transformed < raw)
        assertEquals(0.0f, PressureCalibration.transform(0.0f, PressureCurveType.FIRM), 0.001f)
        assertEquals(1.0f, PressureCalibration.transform(1.0f, PressureCurveType.FIRM), 0.001f)
    }

    @Test
    fun sigmoidCurve_preservesBoundariesAndMaintainsMonotonicity() {
        assertEquals(0.0f, PressureCalibration.transform(0.0f, PressureCurveType.SIGMOID_CALLIGRAPHIC), 0.01f)
        assertEquals(1.0f, PressureCalibration.transform(1.0f, PressureCurveType.SIGMOID_CALLIGRAPHIC), 0.01f)

        var previous = 0.0f
        for (i in 1..10) {
            val p = i / 10.0f
            val curr = PressureCalibration.transform(p, PressureCurveType.SIGMOID_CALLIGRAPHIC)
            assertTrue("Curva deve ser estritamente não-decrescente", curr >= previous)
            previous = curr
        }
    }
}
