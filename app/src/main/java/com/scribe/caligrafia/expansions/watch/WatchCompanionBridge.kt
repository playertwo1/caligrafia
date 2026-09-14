package com.scribe.caligrafia.expansions.watch

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.scribe.caligrafia.preferences.ScribePreferencesStore

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
 * Implementação local-first para Galaxy Watch / Wear OS com fallback tátil no telefone.
 *
 * Importante: conexão física é reportada somente quando o Android expõe um dispositivo Wearable
 * atualmente conectado via GATT. Associação/pareamento isolado não conta como "conectado".
 * A ausência da permissão BLUETOOTH_CONNECT não é contornada nem inventada: nesse caso o probe
 * retorna false e o fallback continua no telefone.
 */
class WatchCompanionAdapter(
    private val context: Context
) : IWatchCompanionBridge {

    private val preferencesStore = ScribePreferencesStore(context)
    private val initialPreferences = preferencesStore.load()

    override var postureAlertThresholdMinutes: Int = initialPreferences.breakIntervalMinutes
        set(value) {
            field = value.coerceIn(5, 60)
            runCatching {
                preferencesStore.update { it.copy(breakIntervalMinutes = field) }
            }
        }

    override var isPostureReminderEnabled: Boolean = initialPreferences.breakReminderEnabled
        set(value) {
            field = value
            runCatching {
                preferencesStore.update { it.copy(breakReminderEnabled = value) }
            }
            if (!value) resetPostureTimer()
        }

    private var accumulatedActiveWritingMs: Long = 0L
    private var lastPostureAlertElapsedMs: Long = 0L

    override fun isWatchConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                ?: return false
            val devices = manager.getConnectedDevices(BluetoothProfile.GATT)
            devices.any { device ->
                val name = runCatching { device.name.orEmpty() }.getOrDefault("")
                val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
                majorClass == BluetoothClass.Device.Major.WEARABLE ||
                    name.contains("Galaxy Watch", ignoreCase = true) ||
                    name.contains("Wear OS", ignoreCase = true) ||
                    name.contains("Watch", ignoreCase = true)
            }
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    override fun sendTimerSync(phaseName: String, remainingSeconds: Int, isRunning: Boolean): Boolean {
        if (!isWatchConnected()) return false
        return try {
            // Adapter boundary: um companion Wear pode observar este evento local e encaminhar via Data Layer.
            // A recepção no relógio só é considerada comprovada por teste físico separado.
            val intent = Intent("com.scribe.caligrafia.action.WATCH_TIMER_SYNC").apply {
                setPackage(context.packageName)
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
        // Fallback local permanece válido sem Watch: pulso duplo de transição de fase.
        return triggerDeviceVibration(longArrayOf(0, 70, 60, 70), intArrayOf(0, 180, 0, 180))
    }

    override fun sendPostureAlertHaptic(): Boolean {
        // Fallback local: 3 pulsos suaves espaçados.
        return triggerDeviceVibration(longArrayOf(0, 100, 100, 100, 100, 100), intArrayOf(0, 150, 0, 150, 0, 150))
    }

    override fun onWritingActivityDetected(durationActiveMs: Long) {
        if (!isPostureReminderEnabled || durationActiveMs <= 0L) return

        accumulatedActiveWritingMs += durationActiveMs
        val thresholdMs = postureAlertThresholdMinutes * 60_000L
        val nowElapsed = SystemClock.elapsedRealtime()

        if (accumulatedActiveWritingMs >= thresholdMs &&
            (lastPostureAlertElapsedMs == 0L || nowElapsed - lastPostureAlertElapsedMs >= MIN_ALERT_GAP_MS)
        ) {
            sendPostureAlertHaptic()
            lastPostureAlertElapsedMs = nowElapsed
            accumulatedActiveWritingMs %= thresholdMs
        }
    }

    override fun resetPostureTimer() {
        accumulatedActiveWritingMs = 0L
        lastPostureAlertElapsedMs = 0L
    }

    private fun triggerDeviceVibration(timings: LongArray, amplitudes: IntArray): Boolean {
        val vibrationEnabled = runCatching { preferencesStore.load().vibrationEnabled }.getOrDefault(true)
        if (!vibrationEnabled) return false

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
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        private const val MIN_ALERT_GAP_MS = 5 * 60_000L
    }
}
