package com.scribe.caligrafia.learning.history

import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.review.SpacedRepetitionItem
import com.scribe.caligrafia.learning.session.ActiveSessionState
import com.scribe.caligrafia.learning.session.SessionDuration
import com.scribe.caligrafia.learning.session.SessionPhase
import java.util.UUID

/**
 * Serializador e parser JSON puro em Kotlin para histórico de sessões e repetição espaçada (SCR-404).
 *
 * Não depende de stubs do android.jar, garantindo testabilidade e determinismo tanto na JVM quanto no Android.
 */
object LearningHistorySerializer {

    fun serialize(
        sessions: List<CompletedSessionRecord>,
        repetitions: List<SpacedRepetitionItem>
    ): String {
        val sb = StringBuilder()
        sb.append("{\n  \"sessions\": [\n")
        sessions.forEachIndexed { index, s ->
            sb.append("    {\n")
            sb.append("      \"sessionId\": \"${escape(s.sessionId)}\",\n")
            sb.append("      \"lessonId\": \"${escape(s.lessonId)}\",\n")
            sb.append("      \"lessonTitle\": \"${escape(s.lessonTitle)}\",\n")
            sb.append("      \"timestampMs\": ${s.timestampMs},\n")
            sb.append("      \"durationMinutes\": ${s.durationMinutes},\n")
            sb.append("      \"actualDurationSeconds\": ${s.actualDurationSeconds},\n")
            sb.append("      \"attemptsCount\": ${s.attemptsCount},\n")
            sb.append("      \"averageScorePercent\": ${s.averageScorePercent ?: "null"}\n")
            sb.append("    }${if (index < sessions.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n  \"spacedRepetition\": [\n")
        repetitions.forEachIndexed { index, r ->
            sb.append("    {\n")
            sb.append("      \"targetId\": \"${escape(r.targetId)}\",\n")
            sb.append("      \"repetitionCount\": ${r.repetitionCount},\n")
            sb.append("      \"lastScorePercent\": ${r.lastScorePercent},\n")
            sb.append("      \"lastPracticedTimestampMs\": ${r.lastPracticedTimestampMs},\n")
            sb.append("      \"intervalDays\": ${r.intervalDays},\n")
            sb.append("      \"nextReviewTimestampMs\": ${r.nextReviewTimestampMs}\n")
            sb.append("    }${if (index < repetitions.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n}")
        return sb.toString()
    }

    fun deserialize(jsonText: String): Pair<List<CompletedSessionRecord>, List<SpacedRepetitionItem>> {
        val sessions = mutableListOf<CompletedSessionRecord>()
        val repetitions = mutableListOf<SpacedRepetitionItem>()

        val sessionsSection = jsonText.substringAfter("\"sessions\"", "").substringBefore("\"spacedRepetition\"", "")
        val repetitionSection = jsonText.substringAfter("\"spacedRepetition\"", "")

        val sessionObjects = extractJsonObjects(sessionsSection)
        for (obj in sessionObjects) {
            val lessonId = extractString(obj, "lessonId") ?: continue
            val sessionId = extractString(obj, "sessionId") ?: UUID.randomUUID().toString()
            val lessonTitle = extractString(obj, "lessonTitle") ?: "Lição"
            val timestampMs = extractLong(obj, "timestampMs") ?: 0L
            val durationMinutes = extractInt(obj, "durationMinutes") ?: 0
            val actualDurationSeconds = extractInt(obj, "actualDurationSeconds") ?: 0
            val attemptsCount = extractInt(obj, "attemptsCount") ?: 0
            val averageScorePercent = extractInt(obj, "averageScorePercent")

            sessions.add(
                CompletedSessionRecord(
                    sessionId = sessionId,
                    lessonId = lessonId,
                    lessonTitle = lessonTitle,
                    timestampMs = timestampMs,
                    durationMinutes = durationMinutes,
                    actualDurationSeconds = actualDurationSeconds,
                    attemptsCount = attemptsCount,
                    averageScorePercent = averageScorePercent
                )
            )
        }

        val repetitionObjects = extractJsonObjects(repetitionSection)
        for (obj in repetitionObjects) {
            val targetId = extractString(obj, "targetId") ?: continue
            val repetitionCount = extractInt(obj, "repetitionCount") ?: 0
            val lastScorePercent = extractInt(obj, "lastScorePercent") ?: 0
            val lastPracticedTimestampMs = extractLong(obj, "lastPracticedTimestampMs") ?: 0L
            val intervalDays = extractInt(obj, "intervalDays") ?: 1
            val nextReviewTimestampMs = extractLong(obj, "nextReviewTimestampMs") ?: 0L

            repetitions.add(
                SpacedRepetitionItem(
                    targetId = targetId,
                    repetitionCount = repetitionCount,
                    lastScorePercent = lastScorePercent,
                    lastPracticedTimestampMs = lastPracticedTimestampMs,
                    intervalDays = intervalDays,
                    nextReviewTimestampMs = nextReviewTimestampMs
                )
            )
        }

        return Pair(sessions, repetitions)
    }

    fun serializeActiveSession(session: ActiveSessionState): String {
        return "{\n" +
            "  \"lessonId\": \"${escape(session.lesson.id)}\",\n" +
            "  \"duration\": \"${session.duration.name}\",\n" +
            "  \"currentPhase\": \"${session.currentPhase.name}\",\n" +
            "  \"phaseElapsedSeconds\": ${session.phaseElapsedSeconds},\n" +
            "  \"phaseTotalSeconds\": ${session.phaseTotalSeconds},\n" +
            "  \"totalElapsedSeconds\": ${session.totalElapsedSeconds},\n" +
            "  \"isPaused\": true,\n" +
            "  \"isFinished\": ${session.isFinished},\n" +
            "  \"attemptsCount\": ${session.attemptsCount},\n" +
            "  \"averageScore\": ${session.averageScore ?: "null"}\n" +
            "}"
    }

    fun deserializeActiveSession(jsonText: String): ActiveSessionState? {
        val lessonId = extractString(jsonText, "lessonId") ?: return null
        val lesson = CurriculumCatalog.getLessonById(lessonId) ?: return null
        val durationName = extractString(jsonText, "duration") ?: SessionDuration.MIN_10.name
        val duration = try { SessionDuration.valueOf(durationName) } catch (_: Throwable) { SessionDuration.MIN_10 }
        val phaseName = extractString(jsonText, "currentPhase") ?: SessionPhase.WARM_UP.name
        val phase = try { SessionPhase.valueOf(phaseName) } catch (_: Throwable) { SessionPhase.WARM_UP }
        val phaseElapsed = extractInt(jsonText, "phaseElapsedSeconds") ?: 0
        val phaseTotal = extractInt(jsonText, "phaseTotalSeconds") ?: 90
        val totalElapsed = extractInt(jsonText, "totalElapsedSeconds") ?: 0
        val isFinished = jsonText.contains("\"isFinished\"\\s*:\\s*true".toRegex())
        val attemptsCount = extractInt(jsonText, "attemptsCount") ?: 0
        val avgScoreRegex = Regex("\"averageScore\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
        val avgScore = avgScoreRegex.find(jsonText)?.groupValues?.get(1)?.toFloatOrNull()

        return ActiveSessionState(
            lesson = lesson,
            duration = duration,
            currentPhase = phase,
            phaseElapsedSeconds = phaseElapsed,
            phaseTotalSeconds = phaseTotal,
            totalElapsedSeconds = totalElapsed,
            isPaused = true, // F2.11: Sempre reabre pausada ao restaurar
            isFinished = isFinished,
            attemptsCount = attemptsCount,
            averageScore = avgScore
        )
    }

    private fun extractJsonObjects(section: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in section.indices) {
            when (section[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        objects.add(section.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractString(block: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        return regex.find(block)?.groupValues?.get(1)?.let { unescape(it) }
    }

    private fun extractInt(block: String, key: String): Int? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return regex.find(block)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(block: String, key: String): Long? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return regex.find(block)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescape(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
