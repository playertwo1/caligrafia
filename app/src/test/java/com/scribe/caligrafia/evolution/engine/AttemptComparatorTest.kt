package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do comparador determinístico de tentativas caligráficas (SCR-501).
 */
class AttemptComparatorTest {

    @Test
    fun compare_calculatesPositiveGainsAndSlantImprovement() {
        val before = PracticeAttemptRecord(
            attemptId = "att_before",
            targetId = "basic_slant",
            targetTitle = "Traço Inclinado",
            timestampMs = 1000000L,
            strokes = emptyList(),
            scorePercent = 60,
            averageSlantDegrees = 62.0f,
            durationMs = 4000L,
            isBaseline = true
        )

        val after = PracticeAttemptRecord(
            attemptId = "att_after",
            targetId = "basic_slant",
            targetTitle = "Traço Inclinado",
            timestampMs = 2000000L,
            strokes = emptyList(),
            scorePercent = 88,
            averageSlantDegrees = 53.0f,
            durationMs = 2200L,
            isBaseline = false
        )

        val comparison = AttemptComparator.compare(before, after, targetSlantDegrees = 52.0f)

        // 88 - 60 = +28
        assertEquals(28, comparison.scoreGainPercent)

        // Before diff from 52 = |62 - 52| = 10. After diff from 52 = |53 - 52| = 1. Improvement = 10 - 1 = 9.0
        assertEquals(9.0f, comparison.slantImprovementDegrees, 0.01f)

        // 2200 - 4000 = -1800ms
        assertEquals(-1800L, comparison.durationDeltaMs)

        assertTrue(comparison.summaryInsight.contains("+28%"))
        assertTrue(comparison.summaryInsight.contains("52°"))
    }

    @Test
    fun compare_handlesEqualScoresWithSlantGain() {
        val before = PracticeAttemptRecord(
            attemptId = "att_1",
            targetId = "underturn",
            targetTitle = "Curva Inferior",
            timestampMs = 1000L,
            strokes = emptyList(),
            scorePercent = 75,
            averageSlantDegrees = 59.0f,
            durationMs = 3000L
        )

        val after = PracticeAttemptRecord(
            attemptId = "att_2",
            targetId = "underturn",
            targetTitle = "Curva Inferior",
            timestampMs = 2000L,
            strokes = emptyList(),
            scorePercent = 75,
            averageSlantDegrees = 54.0f,
            durationMs = 2800L
        )

        val comparison = AttemptComparator.compare(before, after, targetSlantDegrees = 52.0f)
        assertEquals(0, comparison.scoreGainPercent)
        // |59 - 52| = 7. |54 - 52| = 2. 7 - 2 = 5.0
        assertEquals(5.0f, comparison.slantImprovementDegrees, 0.01f)
        assertTrue(comparison.summaryInsight.contains("disciplina angular"))
    }
}
