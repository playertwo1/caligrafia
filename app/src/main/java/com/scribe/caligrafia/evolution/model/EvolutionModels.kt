package com.scribe.caligrafia.evolution.model

import com.scribe.caligrafia.core.model.Stroke

/**
 * Registro de uma tentativa de escrita caligráfica para histórico e evolução (M5 — SCR-501 a SCR-504).
 */
data class PracticeAttemptRecord(
    val attemptId: String,
    val targetId: String,
    val targetTitle: String,
    val timestampMs: Long,
    val strokes: List<Stroke>,
    val scorePercent: Int,
    val averageSlantDegrees: Float,
    val durationMs: Long,
    val isBaseline: Boolean = false,
    val targetSlantDegrees: Float? = null,
    val styleId: String? = null
)

/**
 * Comparativo estruturado entre uma tentativa inicial (Before) e uma recente (After) de um exercício (SCR-501).
 */
data class BeforeAfterComparison(
    val targetId: String,
    val targetTitle: String,
    val beforeAttempt: PracticeAttemptRecord,
    val afterAttempt: PracticeAttemptRecord,
    val scoreGainPercent: Int,
    val slantImprovementDegrees: Float,
    val durationDeltaMs: Long,
    val summaryInsight: String,
    val speedBeforePxPerMs: Float = 0f,
    val speedAfterPxPerMs: Float = 0f,
    val speedGainPercent: Float = 0f
)

/**
 * Registro diário para o calendário de consistência não-punitivo (SCR-504).
 */
data class CalendarDayRecord(
    val epochDay: Long,
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val totalMinutesPracticed: Int,
    val sessionCount: Int
) {
    val hasActivity: Boolean get() = totalMinutesPracticed > 0 || sessionCount > 0
}

/**
 * Resumo global consolidado de evolução e consistência (SCR-504).
 */
data class EvolutionSummary(
    val totalMinutesPracticed: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val activeDaysCount: Int = 0,
    val averageAccuracyGainPercent: Int = 0,
    val comparisonsCount: Int = 0
)
