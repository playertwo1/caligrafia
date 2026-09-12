package com.scribe.caligrafia.learning.history

import com.scribe.caligrafia.learning.review.SpacedRepetitionItem

/**
 * Registro individual de uma sessão de treino concluída (M4 — SCR-404).
 */
data class CompletedSessionRecord(
    val sessionId: String,
    val lessonId: String,
    val lessonTitle: String,
    val timestampMs: Long,
    val durationMinutes: Int,
    val actualDurationSeconds: Int,
    val attemptsCount: Int,
    val averageScorePercent: Int?
)

/**
 * Resumo consolidado de progresso caligráfico não-punitivo.
 *
 * Exibe tempo acumulado e consistência, valorizando cada minuto dedicado à escrita.
 */
data class LearningProgressSummary(
    val totalMinutesPracticed: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val uniqueLessonsPracticed: Int = 0,
    val daysActiveLast30Days: Int = 0,
    val spacedRepetitionItems: Map<String, SpacedRepetitionItem> = emptyMap(),
    val recentSessions: List<CompletedSessionRecord> = emptyList()
)
