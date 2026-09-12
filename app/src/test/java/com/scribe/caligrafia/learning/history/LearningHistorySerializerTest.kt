package com.scribe.caligrafia.learning.history

import com.scribe.caligrafia.learning.review.SpacedRepetitionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do serializador puro de histórico de aprendizado (SCR-404).
 */
class LearningHistorySerializerTest {

    @Test
    fun serializeAndDeserialize_maintainsExactFidelity() {
        val sessions = listOf(
            CompletedSessionRecord(
                sessionId = "s_001",
                lessonId = "lesson_s1_01",
                lessonTitle = "Traço Inclinado",
                timestampMs = 1700000000000L,
                durationMinutes = 10,
                actualDurationSeconds = 600,
                attemptsCount = 5,
                averageScorePercent = 88
            ),
            CompletedSessionRecord(
                sessionId = "s_002",
                lessonId = "lesson_s2_01",
                lessonTitle = "Letra \"i\" com aspas e quebra\nde linha",
                timestampMs = 1700001000000L,
                durationMinutes = 15,
                actualDurationSeconds = 900,
                attemptsCount = 8,
                averageScorePercent = null
            )
        )

        val repetitions = listOf(
            SpacedRepetitionItem(
                targetId = "lesson_s1_01",
                repetitionCount = 2,
                lastScorePercent = 88,
                lastPracticedTimestampMs = 1700000000000L,
                intervalDays = 3,
                nextReviewTimestampMs = 1700259200000L
            )
        )

        val json = LearningHistorySerializer.serialize(sessions, repetitions)
        assertTrue(json.contains("\"sessions\":"))
        assertTrue(json.contains("\"spacedRepetition\":"))

        val (loadedSessions, loadedRepetitions) = LearningHistorySerializer.deserialize(json)
        assertEquals(2, loadedSessions.size)
        assertEquals(1, loadedRepetitions.size)

        val firstSession = loadedSessions[0]
        assertEquals("s_001", firstSession.sessionId)
        assertEquals("lesson_s1_01", firstSession.lessonId)
        assertEquals("Traço Inclinado", firstSession.lessonTitle)
        assertEquals(1700000000000L, firstSession.timestampMs)
        assertEquals(10, firstSession.durationMinutes)
        assertEquals(600, firstSession.actualDurationSeconds)
        assertEquals(5, firstSession.attemptsCount)
        assertEquals(88, firstSession.averageScorePercent)

        val secondSession = loadedSessions[1]
        assertEquals("s_002", secondSession.sessionId)
        assertEquals("Letra \"i\" com aspas e quebra\nde linha", secondSession.lessonTitle)
        assertNull(secondSession.averageScorePercent)

        val repItem = loadedRepetitions[0]
        assertEquals("lesson_s1_01", repItem.targetId)
        assertEquals(2, repItem.repetitionCount)
        assertEquals(88, repItem.lastScorePercent)
        assertEquals(3, repItem.intervalDays)
    }
}
