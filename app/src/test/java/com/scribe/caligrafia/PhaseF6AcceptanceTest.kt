package com.scribe.caligrafia

import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.expansions.styles.PressureCalibration
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import com.scribe.caligrafia.preferences.ActiveBreakReminder
import com.scribe.caligrafia.preferences.ScribePreferences
import com.scribe.caligrafia.preferences.ToolbarSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobertura determinística dos invariantes F6 que independem de aparelho físico.
 * Estes testes são adicionados ao repositório, porém sua execução continua pendente enquanto o
 * GitHub Actions estiver sem cota e não houver runner Gradle local disponível.
 */
class PhaseF6AcceptanceTest {

    @Test
    fun f6_02_handedness_recommendsOppositeToolbarSide_withoutOverridingExplicitChoice() {
        val rightHanded = ScribePreferences(isLeftHanded = false)
        val leftHanded = ScribePreferences(isLeftHanded = true)
        val explicit = ScribePreferences(isLeftHanded = true, toolbarSideOverride = ToolbarSide.LEFT)

        assertEquals(ToolbarSide.LEFT, rightHanded.effectiveToolbarSide)
        assertEquals(ToolbarSide.RIGHT, leftHanded.effectiveToolbarSide)
        assertEquals(ToolbarSide.LEFT, explicit.effectiveToolbarSide)
    }

    @Test
    fun f6_04_defaultInput_isStylusOnly_andFingerModeMustBeExplicit() {
        assertEquals(InputMode.STYLUS_ONLY, ScribePreferences().inputMode)
        assertEquals(InputMode.STYLUS_AND_FINGER, ScribePreferences(inputMode = InputMode.STYLUS_AND_FINGER).inputMode)
    }

    @Test
    fun f6_09_pressureCurves_neverMutateRawPressureSamples() {
        val points = listOf(
            StrokePoint(1f, 2f, 10L, pressure = 0f, tiltRad = 0f, orientationRad = 0f),
            StrokePoint(2f, 3f, 20L, pressure = 0.25f, tiltRad = 0.1f, orientationRad = -0.2f),
            StrokePoint(3f, 4f, 30L, pressure = 1f, tiltRad = null, orientationRad = null)
        )
        val before = points.map { it.copy() }

        val curves = listOf(PressureCurveType.LINEAR, PressureCurveType.SOFT, PressureCurveType.FIRM)
        curves.forEach { curve ->
            points.forEach { point ->
                point.pressure?.let { PressureCalibration.transform(it, curve) }
            }
        }

        assertEquals(before, points)
        assertEquals(0f, points.first().pressure)
        assertEquals(0f, points.first().tiltRad)
        assertEquals(0f, points.first().orientationRad)
    }

    @Test
    fun f6_10_breakReminder_countsOnlyActiveForegroundWriting() {
        val reminder = ActiveBreakReminder(enabled = true, intervalMinutes = 5)

        assertFalse(reminder.onActiveWriting(4 * 60_000L))
        reminder.onPause()
        assertFalse(reminder.onActiveWriting(2 * 60_000L))
        assertEquals(4 * 60_000L, reminder.accumulatedActiveMs)

        reminder.onResume()
        reminder.onBackground()
        assertFalse(reminder.onActiveWriting(2 * 60_000L))
        assertEquals(4 * 60_000L, reminder.accumulatedActiveMs)

        reminder.onForeground()
        assertTrue(reminder.onActiveWriting(60_000L))
        assertEquals(0L, reminder.accumulatedActiveMs)
    }

    @Test
    fun f6_10_disablingReminder_clearsAccumulatedTime() {
        val reminder = ActiveBreakReminder(enabled = true, intervalMinutes = 5)
        reminder.onActiveWriting(90_000L)
        assertTrue(reminder.accumulatedActiveMs > 0)

        reminder.enabled = false
        assertEquals(0L, reminder.accumulatedActiveMs)
        assertFalse(reminder.onActiveWriting(10 * 60_000L))
    }
}
