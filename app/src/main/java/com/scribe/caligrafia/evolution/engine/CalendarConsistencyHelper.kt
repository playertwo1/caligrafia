package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.evolution.model.CalendarDayRecord
import com.scribe.caligrafia.evolution.model.EvolutionSummary
import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import java.util.Calendar
import java.util.TimeZone

/**
 * Agregador determinístico para o calendário de consistência não-punitivo (SCR-504).
 */
object CalendarConsistencyHelper {

    /**
     * Constrói os registros diários para o mês atual ou especificado.
     */
    fun buildMonthDays(
        sessions: List<CompletedSessionRecord>,
        calendar: Calendar = Calendar.getInstance(TimeZone.getDefault())
    ): List<CalendarDayRecord> {
        val targetYear = calendar.get(Calendar.YEAR)
        val targetMonth = calendar.get(Calendar.MONTH) // 0-based
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Mapeia segundos e contagem por dia do mês (R07: consistência matemática de durações)
        val secondsPerDay = mutableMapOf<Int, Int>()
        val countPerDay = mutableMapOf<Int, Int>()

        val sessionCal = Calendar.getInstance(TimeZone.getDefault())
        for (session in sessions) {
            sessionCal.timeInMillis = session.timestampMs
            if (sessionCal.get(Calendar.YEAR) == targetYear && sessionCal.get(Calendar.MONTH) == targetMonth) {
                val day = sessionCal.get(Calendar.DAY_OF_MONTH)
                secondsPerDay[day] = (secondsPerDay[day] ?: 0) + session.actualDurationSeconds.coerceAtLeast(0)
                countPerDay[day] = (countPerDay[day] ?: 0) + 1
            }
        }

        val result = mutableListOf<CalendarDayRecord>()
        for (day in 1..daysInMonth) {
            val daySeconds = secondsPerDay[day] ?: 0
            result.add(
                CalendarDayRecord(
                    epochDay = 0L, // simplificado
                    dayOfMonth = day,
                    month = targetMonth + 1,
                    year = targetYear,
                    totalMinutesPracticed = daySeconds / 60,
                    sessionCount = countPerDay[day] ?: 0
                )
            )
        }
        return result
    }

    /**
     * Calcula o resumo de consistência não-punitivo.
     */
    fun computeSummary(
        sessions: List<CompletedSessionRecord>,
        comparisonsCount: Int,
        averageGain: Int
    ): EvolutionSummary {
        val totalSeconds = sessions.sumOf { it.actualDurationSeconds }
        val totalMinutes = totalSeconds / 60
        val totalSessions = sessions.size

        val cal = Calendar.getInstance(TimeZone.getDefault())
        val uniqueDays = sessions.map {
            cal.timeInMillis = it.timestampMs
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size

        return EvolutionSummary(
            totalMinutesPracticed = totalMinutes,
            totalSessionsCompleted = totalSessions,
            activeDaysCount = uniqueDays,
            averageAccuracyGainPercent = averageGain,
            comparisonsCount = comparisonsCount
        )
    }
}
