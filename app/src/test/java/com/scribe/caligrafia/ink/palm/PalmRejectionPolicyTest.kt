package com.scribe.caligrafia.ink.palm

import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PalmRejectionPolicyTest {

    private lateinit var policy: PalmRejectionPolicy

    @Before
    fun setUp() {
        policy = PalmRejectionPolicy(
            inputMode = InputMode.STYLUS_ONLY,
            hoverCooldownMs = 500L
        )
    }

    @Test
    fun testStylusOnlyModeRejectsFinger() {
        val decision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1000L)
        assertTrue(decision is PalmDecision.Reject)
        assertEquals(1L, policy.rejectedPalmTouchesCount)
        assertTrue(policy.lastRejectionReason?.contains("Modo Stylus-Only") == true)
    }

    @Test
    fun testStylusOnlyModeAllowsStylusAndEraser() {
        val stylusDecision = policy.evaluateTouch(ToolType.STYLUS, eventTimeMs = 1000L)
        val eraserDecision = policy.evaluateTouch(ToolType.ERASER, eventTimeMs = 1005L)

        assertTrue(stylusDecision is PalmDecision.Allow)
        assertTrue(eraserDecision is PalmDecision.Allow)
        assertEquals(0L, policy.rejectedPalmTouchesCount)
    }

    @Test
    fun testStylusAndFingerModeAllowsFingerWhenIdle() {
        policy.inputMode = InputMode.STYLUS_AND_FINGER
        val decision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1000L)

        assertTrue(decision is PalmDecision.Allow)
        assertEquals(0L, policy.rejectedPalmTouchesCount)
    }

    @Test
    fun testStylusAndFingerModeRejectsFingerWhileStylusDown() {
        policy.inputMode = InputMode.STYLUS_AND_FINGER
        policy.onStylusDown(eventTimeMs = 1000L)

        val decision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1050L)
        assertTrue(decision is PalmDecision.Reject)
        assertEquals(1L, policy.rejectedPalmTouchesCount)
        assertTrue(policy.lastRejectionReason?.contains("escrita ativa") == true)
    }

    @Test
    fun testStylusAndFingerModeRejectsFingerWhileHovering() {
        policy.inputMode = InputMode.STYLUS_AND_FINGER
        policy.onStylusHoverEnter(eventTimeMs = 1000L)

        assertTrue(policy.isStylusHovering)
        val decision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1050L)
        assertTrue(decision is PalmDecision.Reject)
        assertEquals(1L, policy.rejectedPalmTouchesCount)
        assertTrue(policy.lastRejectionReason?.contains("proximidade") == true)
    }

    @Test
    fun testStylusAndFingerModeRejectsFingerDuringCooldown() {
        policy.inputMode = InputMode.STYLUS_AND_FINGER
        // Hover sai em t = 1000ms
        policy.onStylusHoverExit(eventTimeMs = 1000L)

        // Dedo toca em t = 1300ms (delta = 300ms < 500ms cooldown)
        val earlyDecision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1300L)
        assertTrue(earlyDecision is PalmDecision.Reject)
        assertTrue(policy.lastRejectionReason?.contains("Cooldown") == true)

        // Dedo toca em t = 1600ms (delta = 600ms >= 500ms cooldown)
        val lateDecision = policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1600L)
        assertTrue(lateDecision is PalmDecision.Allow)
    }

    @Test
    fun testResetMetricsClearsRejectionCounts() {
        policy.evaluateTouch(ToolType.FINGER, eventTimeMs = 1000L)
        assertEquals(1L, policy.rejectedPalmTouchesCount)

        policy.resetMetrics()
        assertEquals(0L, policy.rejectedPalmTouchesCount)
        assertEquals(null, policy.lastRejectionReason)
    }
}
