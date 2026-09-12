package com.scribe.caligrafia.ink.replay

import com.scribe.caligrafia.core.model.Stroke

/**
 * Estados do ciclo de vida da reprodução temporal (Replay).
 */
enum class ReplayStatus {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED
}

/**
 * Multiplicadores de velocidade para estudo caligráfico.
 */
enum class ReplaySpeed(val multiplier: Float, val displayName: String) {
    HALF(0.5f, "0.5x"),
    NORMAL(1.0f, "1.0x"),
    DOUBLE(2.0f, "2.0x")
}

/**
 * Fotograma vetorial puro no instante temporal corrente.
 *
 * @param completedStrokes Traços que já terminaram antes do instante atual.
 * @param activeStroke Traço que está sendo executado no instante atual (contendo apenas os pontos desenhados até agora).
 * @param currentPositionMs Tempo decorrido na linha do tempo da caligrafia (relativo a 0).
 * @param totalDurationMs Duração total da sessão (tMax - tMin).
 * @param progressFraction Fração normalizada de 0.0 a 1.0.
 */
data class ReplayFrame(
    val completedStrokes: List<Stroke> = emptyList(),
    val activeStroke: Stroke? = null,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val progressFraction: Float = if (totalDurationMs > 0L) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f,
    val status: ReplayStatus = ReplayStatus.IDLE,
    val speed: ReplaySpeed = ReplaySpeed.NORMAL
)
