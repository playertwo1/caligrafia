package com.scribe.caligrafia.learning.review

import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.model.CurriculumStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do algoritmo determinístico de repetição espaçada caligráfica (M4 — SCR-403).
 */
class ReviewSchedulerTest {

    private val oneDayMs = 24 * 60 * 60 * 1000L

    @Test
    fun updateRepetition_scoreBelow60_resetsIntervalToOneDay() {
        val baseTime = 1000000000L
        val prevItem = SpacedRepetitionItem(
            targetId = "lesson_1",
            repetitionCount = 3,
            intervalDays = 5,
            nextReviewTimestampMs = baseTime
        )

        val updated = ReviewScheduler.updateRepetition(
            currentItem = prevItem,
            targetId = "lesson_1",
            scorePercent = 55,
            nowMs = baseTime
        )

        assertEquals(1, updated.intervalDays)
        assertEquals(4, updated.repetitionCount)
        assertEquals(55, updated.lastScorePercent)
        assertEquals(baseTime + oneDayMs, updated.nextReviewTimestampMs)
    }

    @Test
    fun updateRepetition_scoreBetween60And79_expandsIntervalByOneAndHalf() {
        val baseTime = 1000000000L
        val prevItem = SpacedRepetitionItem(
            targetId = "lesson_1",
            repetitionCount = 1,
            intervalDays = 2,
            nextReviewTimestampMs = baseTime
        )

        val updated = ReviewScheduler.updateRepetition(
            currentItem = prevItem,
            targetId = "lesson_1",
            scorePercent = 75,
            nowMs = baseTime
        )

        // 2 * 1.5 = 3
        assertEquals(3, updated.intervalDays)
        assertEquals(baseTime + (3 * oneDayMs), updated.nextReviewTimestampMs)
    }

    @Test
    fun updateRepetition_score80AndAbove_expandsIntervalByTwoPointTwo() {
        val baseTime = 1000000000L
        val prevItem = SpacedRepetitionItem(
            targetId = "lesson_1",
            repetitionCount = 2,
            intervalDays = 3,
            nextReviewTimestampMs = baseTime
        )

        val updated = ReviewScheduler.updateRepetition(
            currentItem = prevItem,
            targetId = "lesson_1",
            scorePercent = 90,
            nowMs = baseTime
        )

        // 3 * 2.2 = 6.6 -> round to 7
        assertEquals(7, updated.intervalDays)
        assertEquals(baseTime + (7 * oneDayMs), updated.nextReviewTimestampMs)
    }

    @Test
    fun getDailyRecommendation_withNoHistory_returnsFirstLesson() {
        val recommendation = ReviewScheduler.getDailyRecommendation(
            allLessons = CurriculumCatalog.ALL_LESSONS,
            records = emptyList()
        )

        assertNotNull(recommendation)
        assertEquals(CurriculumStage.STAGE_1_STROKES, recommendation.warmUpLesson.stage)
        assertEquals("lesson_s1_01_slant", recommendation.focusLesson.id)
    }

    @Test
    fun getDailyRecommendation_prioritizesDueReviewItemWithLowestScore() {
        val now = 2000000000L
        val dueItem1 = SpacedRepetitionItem(
            targetId = "lesson_s1_02_underturn",
            lastScorePercent = 85,
            nextReviewTimestampMs = now - 1000L // Vencido
        )
        val dueItem2 = SpacedRepetitionItem(
            targetId = "lesson_s1_03_overturn",
            lastScorePercent = 50, // Menor nota: prioridade máxima!
            nextReviewTimestampMs = now - 1000L // Vencido
        )

        val recommendation = ReviewScheduler.getDailyRecommendation(
            allLessons = CurriculumCatalog.ALL_LESSONS,
            records = listOf(dueItem1, dueItem2),
            nowMs = now
        )

        assertEquals("lesson_s1_03_overturn", recommendation.focusLesson.id)
        assertTrue(recommendation.reason.contains("Revisão sugerida"))
    }
}
