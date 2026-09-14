package com.scribe.caligrafia.preferences

/**
 * Relógio monotônico de tempo ATIVO de escrita para lembretes de pausa.
 *
 * Não usa relógio civil e não acumula tempo enquanto a sessão está pausada ou em background.
 * O chamador informa deltas ativos; ao atingir o limiar, o contador reinicia somente depois de
 * consumir o alerta. Assim não há rajada de alertas ao retornar do fundo.
 */
class ActiveBreakReminder(
    enabled: Boolean = true,
    intervalMinutes: Int = 15
) {
    var enabled: Boolean = enabled
        set(value) {
            field = value
            if (!value) accumulatedActiveMs = 0L
        }

    var intervalMinutes: Int = intervalMinutes.coerceIn(5, 60)
        set(value) {
            field = value.coerceIn(5, 60)
            accumulatedActiveMs = accumulatedActiveMs.coerceAtMost(thresholdMs())
        }

    var isPaused: Boolean = false
        private set

    var isInBackground: Boolean = false
        private set

    var accumulatedActiveMs: Long = 0L
        private set

    fun onActiveWriting(deltaMs: Long): Boolean {
        if (!enabled || isPaused || isInBackground || deltaMs <= 0L) return false
        accumulatedActiveMs = (accumulatedActiveMs + deltaMs).coerceAtLeast(0L)
        if (accumulatedActiveMs < thresholdMs()) return false
        accumulatedActiveMs %= thresholdMs()
        return true
    }

    fun onPause() {
        isPaused = true
    }

    fun onResume() {
        isPaused = false
    }

    fun onBackground() {
        isInBackground = true
    }

    fun onForeground() {
        isInBackground = false
    }

    fun reset() {
        accumulatedActiveMs = 0L
    }

    private fun thresholdMs(): Long = intervalMinutes * 60_000L
}
