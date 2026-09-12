package com.scribe.caligrafia.expansions.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchCompanionAdapterTest {

    private class TestWatchBridge : IWatchCompanionBridge {
        override var postureAlertThresholdMinutes: Int = 15
        override var isPostureReminderEnabled: Boolean = true

        var alertTriggerCount = 0
        var phaseChangeCount = 0
        var timerSyncCount = 0
        var accumulatedWritingMs = 0L

        override fun isWatchConnected(): Boolean = false

        override fun sendTimerSync(phaseName: String, remainingSeconds: Int, isRunning: Boolean): Boolean {
            timerSyncCount++
            return true
        }

        override fun sendPhaseChangeHaptic(phaseName: String): Boolean {
            phaseChangeCount++
            return true
        }

        override fun sendPostureAlertHaptic(): Boolean {
            alertTriggerCount++
            return true
        }

        override fun onWritingActivityDetected(durationActiveMs: Long) {
            if (!isPostureReminderEnabled) return
            accumulatedWritingMs += durationActiveMs
            val threshold = postureAlertThresholdMinutes * 60 * 1000L
            if (accumulatedWritingMs >= threshold) {
                sendPostureAlertHaptic()
                accumulatedWritingMs = 0L
            }
        }

        override fun resetPostureTimer() {
            accumulatedWritingMs = 0L
        }
    }

    @Test
    fun bridge_tracksPostureAlertsBasedOnThreshold() {
        val bridge = TestWatchBridge()
        bridge.postureAlertThresholdMinutes = 10 // 600_000 ms

        // Simula 5 minutos de escrita ativa (300_000 ms)
        bridge.onWritingActivityDetected(300_000L)
        assertEquals(0, bridge.alertTriggerCount)

        // Simula mais 6 minutos de escrita ativa (360_000 ms) -> Total 11 min
        bridge.onWritingActivityDetected(360_000L)
        assertEquals(1, bridge.alertTriggerCount)

        // Reset do timer
        bridge.resetPostureTimer()
        assertEquals(0L, bridge.accumulatedWritingMs)
    }

    @Test
    fun bridge_disablesAlertsWhenPostureReminderIsTurnedOff() {
        val bridge = TestWatchBridge()
        bridge.isPostureReminderEnabled = false
        bridge.postureAlertThresholdMinutes = 5

        // Simula 10 minutos de escrita ativa
        bridge.onWritingActivityDetected(600_000L)
        assertEquals(0, bridge.alertTriggerCount)
    }

    @Test
    fun bridge_triggersPhaseChangeAndTimerSync() {
        val bridge = TestWatchBridge()
        assertTrue(bridge.sendPhaseChangeHaptic("Foco"))
        assertEquals(1, bridge.phaseChangeCount)

        assertTrue(bridge.sendTimerSync("Prática", 180, true))
        assertEquals(1, bridge.timerSyncCount)
    }
}
