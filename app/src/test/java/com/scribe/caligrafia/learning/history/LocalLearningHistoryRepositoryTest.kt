package com.scribe.caligrafia.learning.history

import com.scribe.caligrafia.learning.review.SpacedRepetitionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Testes unitários do repositório local de histórico de aprendizado (M4 — SCR-404).
 */
class LocalLearningHistoryRepositoryTest {

    @Test
    fun recordSession_persistsAndCalculatesSummaryAccurately() {
        val tempDir = Files.createTempDirectory("scribe_learning_test").toFile()
        try {
            val repository = LocalLearningHistoryRepository(tempDir)

            val session1 = CompletedSessionRecord(
                sessionId = "s1",
                lessonId = "lesson_1",
                lessonTitle = "Traço Reto",
                timestampMs = System.currentTimeMillis(),
                durationMinutes = 10,
                actualDurationSeconds = 600,
                attemptsCount = 5,
                averageScorePercent = 82
            )

            val repetition1 = SpacedRepetitionItem(
                targetId = "lesson_1",
                repetitionCount = 1,
                lastScorePercent = 82,
                lastPracticedTimestampMs = session1.timestampMs,
                intervalDays = 3,
                nextReviewTimestampMs = session1.timestampMs + (3 * 24 * 3600 * 1000L)
            )

            repository.recordSession(session1, repetition1)

            val summary = repository.getProgressSummary()
            assertEquals(10, summary.totalMinutesPracticed)
            assertEquals(1, summary.totalSessionsCompleted)
            assertEquals(1, summary.uniqueLessonsPracticed)
            assertEquals(1, summary.daysActiveLast30Days)
            assertEquals(1, summary.spacedRepetitionItems.size)
            assertEquals(82, summary.spacedRepetitionItems["lesson_1"]?.lastScorePercent)

            // Testa persistência recriando a instância a partir do mesmo diretório
            val reloadedRepo = LocalLearningHistoryRepository(tempDir)
            val reloadedSummary = reloadedRepo.getProgressSummary()
            assertEquals(10, reloadedSummary.totalMinutesPracticed)
            assertEquals(1, reloadedSummary.totalSessionsCompleted)
            assertEquals(1, reloadedSummary.uniqueLessonsPracticed)
            assertEquals(1, reloadedSummary.spacedRepetitionItems.size)
            assertEquals("Traço Reto", reloadedSummary.recentSessions.first().lessonTitle)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun recordMultipleSessions_accumulatesMinutesAndRespectsNonPunitiveLogic() {
        val tempDir = Files.createTempDirectory("scribe_learning_multi_test").toFile()
        try {
            val repository = LocalLearningHistoryRepository(tempDir)

            // Sessão 1: 5 minutos
            repository.recordSession(
                CompletedSessionRecord(
                    sessionId = "s1",
                    lessonId = "lesson_1",
                    lessonTitle = "Lição 1",
                    timestampMs = System.currentTimeMillis() - 86400000L,
                    durationMinutes = 5,
                    actualDurationSeconds = 300,
                    attemptsCount = 3,
                    averageScorePercent = 70
                )
            )

            // Sessão 2: 15 minutos (outra lição)
            repository.recordSession(
                CompletedSessionRecord(
                    sessionId = "s2",
                    lessonId = "lesson_2",
                    lessonTitle = "Lição 2",
                    timestampMs = System.currentTimeMillis(),
                    durationMinutes = 15,
                    actualDurationSeconds = 900,
                    attemptsCount = 8,
                    averageScorePercent = 90
                )
            )

            val summary = repository.getProgressSummary()
            // 300 + 900 = 1200 segundos = 20 minutos
            assertEquals(20, summary.totalMinutesPracticed)
            assertEquals(2, summary.totalSessionsCompleted)
            assertEquals(2, summary.uniqueLessonsPracticed)
            assertEquals(2, summary.recentSessions.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
