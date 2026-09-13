package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Testes unitários do agregador do calendário de consistência (SCR-504).
 */
class CalendarConsistencyHelperTest {

    @Test
    fun buildMonthDays_aggregatesMinutesCorrectly() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val session1 = CompletedSessionRecord(
            sessionId = "s1",
            lessonId = "l1",
            lessonTitle = "Lição 1",
            timestampMs = cal.timeInMillis, // Dia 15
            durationMinutes = 10,
            actualDurationSeconds = 600,
            attemptsCount = 5,
            averageScorePercent = 80
        )

        val session2 = CompletedSessionRecord(
            sessionId = "s2",
            lessonId = "l2",
            lessonTitle = "Lição 2",
            timestampMs = cal.timeInMillis + 3600000L, // Mesmo dia 15
            durationMinutes = 15,
            actualDurationSeconds = 900,
            attemptsCount = 6,
            averageScorePercent = 85
        )

        val days = CalendarConsistencyHelper.buildMonthDays(listOf(session1, session2), cal)

        assertEquals(30, days.size) // Setembro tem 30 dias

        val day15 = days.first { it.dayOfMonth == 15 }
        // 600s + 900s = 1500s = 25 min
        assertEquals(25, day15.totalMinutesPracticed)
        assertEquals(2, day15.sessionCount)
        assertTrue(day15.hasActivity)

        val day1 = days.first { it.dayOfMonth == 1 }
        assertEquals(0, day1.totalMinutesPracticed)
        assertEquals(0, day1.sessionCount)
    }

    @Test
    fun computeSummary_calculatesTotalsWithoutPunitiveStreaks() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0)
        val day1Ms = cal.timeInMillis

        cal.set(2026, Calendar.SEPTEMBER, 10, 10, 0, 0)
        val day10Ms = cal.timeInMillis

        val sessions = listOf(
            CompletedSessionRecord("s1", "l1", "Lição 1", day1Ms, 10, 600, 4, 80),
            CompletedSessionRecord("s2", "l1", "Lição 1", day1Ms + 1000L, 10, 600, 4, 85),
            CompletedSessionRecord("s3", "l2", "Lição 2", day10Ms, 20, 1200, 8, 90)
        )

        val summary = CalendarConsistencyHelper.computeSummary(
            sessions = sessions,
            comparisonsCount = 2,
            averageGain = 18
        )

        // 600 + 600 + 1200 = 2400s = 40 min
        assertEquals(40, summary.totalMinutesPracticed)
        assertEquals(3, summary.totalSessionsCompleted)
        assertEquals(2, summary.activeDaysCount) // 2 dias distintos
        assertEquals(18, summary.averageAccuracyGainPercent)
        assertEquals(2, summary.comparisonsCount)
    }

    @Test
    fun buildMonthDays_zeroSecondSession_doesNotTurnIntoOneMinute() {
        // R07: Sessão com 0 segundos não deve virar 1 minuto inventado
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)

        val sessionZero = CompletedSessionRecord(
            sessionId = "s_zero",
            lessonId = "l1",
            lessonTitle = "Lição Zero",
            timestampMs = cal.timeInMillis,
            durationMinutes = 0,
            actualDurationSeconds = 0,
            attemptsCount = 0,
            averageScorePercent = 0
        )

        val days = CalendarConsistencyHelper.buildMonthDays(listOf(sessionZero), cal)
        val day15 = days.first { it.dayOfMonth == 15 }
        assertEquals(0, day15.totalMinutesPracticed)
    }
}
