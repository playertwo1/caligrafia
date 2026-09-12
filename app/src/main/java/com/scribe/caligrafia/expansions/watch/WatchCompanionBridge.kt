package com.scribe.caligrafia.expansions.watch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Interface desacoplada (Adapter Pattern) para comunicação do Scribe com Wear OS / Galaxy Watch.
 */
interface IWatchCompanionBridge {
    fun isWatchConnected(): Boolean
    fun sendTimerSync(phaseName: String, remainingSeconds: Int, isRunning: Boolean): Boolean
    fun sendPhaseChangeHaptic(phaseName: String): Boolean
    fun sendPostureAlertHaptic(): Boolean
    fun onWritingActivityDetected(durationActiveMs: Long)
    fun resetPostureTimer()
    var postureAlertThresholdMinutes: Int
    var isPostureReminderEnabled: Boolean
}

/**
 * Implementação do adaptador para Galaxy Watch e Wear OS com fallback tátil no próprio aparelho.
 * Mantém o Scribe 100% autônomo sem dependências obrigatórias de hardware externo.
 */
class WatchCompanionAdapter(
    private val context: Context
) : IWatchCompanionBridge {

    override var postureAlertThresholdMinutes: Int = 15
    override var isPostureReminderEnabled: Boolean = true

    private var accumulatedActiveWritingMs: Long = 0L
    private var lastPostureAlertTimestampMs: Long = 0L

    override fun isWatchConnected(): Boolean {
        // Modo desacoplado: pronto para registrar nós do Wearable Data Layer quando pareado
        return false
    }

    override fun sendTimerSync(phaseName: String, remainingSeconds: Int, isRunning: Boolean): Boolean {
        return try {
            val intent = Intent("com.scribe.caligrafia.action.WATCH_TIMER_SYNC").apply {
                putExtra("extra_phase_name", phaseName)
                putExtra("extra_remaining_seconds", remainingSeconds)
                putExtra("extra_is_running", isRunning)
            }
            context.sendBroadcast(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun sendPhaseChangeHaptic(phaseName: String): Boolean {
        // Envia pulso rítmico duplo de transição de fase (ex: 70ms pulso, 60ms pausa, 70ms pulso)
        return triggerDeviceVibration(longArrayOf(0, 70, 60, 70), intArrayOf(0, 180, 0, 180))
    }

    override fun sendPostureAlertHaptic(): Boolean {
        // Alerta postural ergonômico sutil: 3 pulsos suaves espaçados
        return triggerDeviceVibration(longArrayOf(0, 100, 100, 100, 100, 100), intArrayOf(0, 150, 0, 150, 0, 150))
    }

    override fun onWritingActivityDetected(durationActiveMs: Long) {
        if (!isPostureReminderEnabled) return

        accumulatedActiveWritingMs += durationActiveMs
        val thresholdMs = postureAlertThresholdMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        if (accumulatedActiveWritingMs >= thresholdMs && (now - lastPostureAlertTimestampMs > 5 * 60 * 1000L)) {
            sendPostureAlertHaptic()
            lastPostureAlertTimestampMs = now
            accumulatedActiveWritingMs = 0L
        }
    }

    override fun resetPostureTimer() {
        accumulatedActiveWritingMs = 0L
        lastPostureAlertTimestampMs = 0L
    }

    private fun triggerDeviceVibration(timings: LongArray, amplitudes: IntArray): Boolean {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
