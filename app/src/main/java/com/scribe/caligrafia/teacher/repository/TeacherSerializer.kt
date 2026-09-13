package com.scribe.caligrafia.teacher.repository

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.MaturityLevel
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight

/**
 * Serializador JSON puro em Kotlin para o módulo do Professor IA.
 * Não depende de android.jar para assegurar execução 100% verde em JVM unit tests.
 */
object TeacherSerializer {

    fun serializeDiagnostic(diag: BiomechanicalDiagnostic): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"id\": \"${diag.id}\",\n")
        sb.append("  \"timestampMs\": ${diag.timestampMs},\n")
        sb.append("  \"overallScore\": ${diag.overallScore},\n")
        sb.append("  \"maturityLevel\": \"${diag.maturityLevel.name}\",\n")
        sb.append("  \"primaryWeakness\": ${diag.primaryWeakness?.let { "\"${it.name}\"" } ?: "null"},\n")
        sb.append("  \"primaryStrength\": ${diag.primaryStrength?.let { "\"${it.name}\"" } ?: "null"},\n")
        sb.append("  \"totalStrokesAnalyzed\": ${diag.totalStrokesAnalyzed},\n")
        sb.append("  \"totalAttemptsAnalyzed\": ${diag.totalAttemptsAnalyzed},\n")
        sb.append("  \"dimensions\": [\n")

        val dimsList = diag.dimensions.values.toList()
        for (i in dimsList.indices) {
            val d = dimsList[i]
            sb.append("    {\n")
            sb.append("      \"dimension\": \"${d.dimension.name}\",\n")
            sb.append("      \"score\": ${d.score},\n")
            sb.append("      \"observedValue\": ${d.observedValue},\n")
            sb.append("      \"targetValue\": ${d.targetValue},\n")
            sb.append("      \"status\": \"${d.status.name}\",\n")
            sb.append("      \"shortDiagnosis\": \"${escapeJson(d.shortDiagnosis)}\"\n")
            sb.append("    }${if (i < dimsList.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun deserializeDiagnostic(json: String): BiomechanicalDiagnostic? {
        return try {
            val id = extractString(json, "id") ?: return null
            val timestampMs = extractLong(json, "timestampMs") ?: System.currentTimeMillis()
            val overallScore = extractFloat(json, "overallScore") ?: 70f
            val maturityStr = extractString(json, "maturityLevel")
            val maturityLevel = maturityStr?.let { runCatching { MaturityLevel.valueOf(it) }.getOrNull() } ?: MaturityLevel.PRACTITIONER
            val primaryWeaknessStr = extractString(json, "primaryWeakness")
            val primaryWeakness = primaryWeaknessStr?.let { runCatching { BiomechanicalDimension.valueOf(it) }.getOrNull() }
            val primaryStrengthStr = extractString(json, "primaryStrength")
            val primaryStrength = primaryStrengthStr?.let { runCatching { BiomechanicalDimension.valueOf(it) }.getOrNull() }
            val totalStrokes = extractInt(json, "totalStrokesAnalyzed") ?: 0
            val totalAttempts = extractInt(json, "totalAttemptsAnalyzed") ?: 0

            val dimensionsMap = mutableMapOf<BiomechanicalDimension, DimensionEvaluation>()
            val dimsBlock = extractArrayBlock(json, "dimensions")
            if (dimsBlock != null) {
                val itemRegex = "\\{[^}]+\\}".toRegex()
                val matches = itemRegex.findAll(dimsBlock)
                for (m in matches) {
                    val itemStr = m.value
                    val dimName = extractString(itemStr, "dimension") ?: continue
                    val dim = runCatching { BiomechanicalDimension.valueOf(dimName) }.getOrNull() ?: continue
                    val score = extractFloat(itemStr, "score") ?: 70f
                    val observed = extractFloat(itemStr, "observedValue")
                    val target = extractFloat(itemStr, "targetValue") ?: 100f
                    val statusStr = extractString(itemStr, "status") ?: "GOOD"
                    val status = runCatching { EvaluationStatus.valueOf(statusStr) }.getOrDefault(EvaluationStatus.GOOD)
                    val diag = extractString(itemStr, "shortDiagnosis") ?: ""

                    dimensionsMap[dim] = DimensionEvaluation(
                        dimension = dim,
                        score = score,
                        observedValue = observed,
                        targetValue = target,
                        status = status,
                        shortDiagnosis = diag
                    )
                }
            }

            BiomechanicalDiagnostic(
                id = id,
                timestampMs = timestampMs,
                overallScore = overallScore,
                maturityLevel = maturityLevel,
                dimensions = dimensionsMap,
                primaryWeakness = primaryWeakness,
                primaryStrength = primaryStrength,
                totalStrokesAnalyzed = totalStrokes,
                totalAttemptsAnalyzed = totalAttempts
            )
        } catch (_: Exception) {
            null
        }
    }

    fun serializePrescription(presc: PrescribedPracticeSession): String {
        val stagesJson = presc.stages.joinToString(separator = ", ") { "\"${escapeJson(it)}\"" }
        return """
        {
          "id": "${presc.id}",
          "timestampMs": ${presc.timestampMs},
          "title": "${escapeJson(presc.title)}",
          "rationale": "${escapeJson(presc.rationale)}",
          "targetDimension": "${presc.targetDimension.name}",
          "recommendedMinutes": ${presc.recommendedMinutes},
          "warmupExerciseId": "${presc.warmupExerciseId}",
          "focusExerciseId": "${presc.focusExerciseId}",
          "recommendedGhostLevel": ${presc.recommendedGhostLevel},
          "targetGoalDescription": "${escapeJson(presc.targetGoalDescription)}",
          "seriesCount": ${presc.seriesCount},
          "stages": [$stagesJson],
          "isCompleted": ${presc.isCompleted}
        }
        """.trimIndent()
    }

    fun deserializePrescription(json: String): PrescribedPracticeSession? {
        return try {
            val id = extractString(json, "id") ?: return null
            val timestampMs = extractLong(json, "timestampMs") ?: System.currentTimeMillis()
            val title = extractString(json, "title") ?: ""
            val rationale = extractString(json, "rationale") ?: ""
            val dimStr = extractString(json, "targetDimension") ?: "SLANT_STABILITY"
            val targetDimension = runCatching { BiomechanicalDimension.valueOf(dimStr) }.getOrDefault(BiomechanicalDimension.SLANT_STABILITY)
            val minutes = extractInt(json, "recommendedMinutes") ?: 10
            val warmup = extractString(json, "warmupExerciseId") ?: "basic_slant"
            val focus = extractString(json, "focusExerciseId") ?: "basic_slant"
            val ghost = extractFloat(json, "recommendedGhostLevel") ?: 0.7f
            val goal = extractString(json, "targetGoalDescription") ?: ""
            val series = extractInt(json, "seriesCount") ?: 3
            val stages = mutableListOf<String>()
            val stagesBlock = extractArrayBlock(json, "stages")
            if (stagesBlock != null) {
                val strRegex = "\"([^\"]*)\"".toRegex()
                for (match in strRegex.findAll(stagesBlock)) {
                    stages.add(match.groupValues[1])
                }
            }
            val finalStages = if (stages.isNotEmpty()) stages else listOf("Aquecimento", "Condução com Ghost", "Prática Autônoma")
            val isCompleted = extractBoolean(json, "isCompleted") ?: false

            PrescribedPracticeSession(
                id = id,
                timestampMs = timestampMs,
                title = title,
                rationale = rationale,
                targetDimension = targetDimension,
                recommendedMinutes = minutes,
                warmupExerciseId = warmup,
                focusExerciseId = focus,
                recommendedGhostLevel = ghost,
                targetGoalDescription = goal,
                seriesCount = series,
                stages = finalStages,
                isCompleted = isCompleted
            )
        } catch (_: Exception) {
            null
        }
    }

    fun serializeInsights(insights: List<TeacherInsight>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        for (i in insights.indices) {
            val ins = insights[i]
            sb.append("  {\n")
            sb.append("    \"id\": \"${ins.id}\",\n")
            sb.append("    \"type\": \"${ins.type.name}\",\n")
            sb.append("    \"title\": \"${escapeJson(ins.title)}\",\n")
            sb.append("    \"message\": \"${escapeJson(ins.message)}\",\n")
            sb.append("    \"relatedDimension\": ${ins.relatedDimension?.let { "\"${it.name}\"" } ?: "null"},\n")
            sb.append("    \"metricDelta\": ${ins.metricDelta?.let { "\"${escapeJson(it)}\"" } ?: "null"},\n")
            sb.append("    \"relatedAttemptId\": ${ins.relatedAttemptId?.let { "\"${it}\"" } ?: "null"},\n")
            sb.append("    \"relatedTargetTitle\": ${ins.relatedTargetTitle?.let { "\"${escapeJson(it)}\"" } ?: "null"},\n")
            sb.append("    \"relatedScore\": ${ins.relatedScore ?: "null"}\n")
            sb.append("  }${if (i < insights.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserializeInsights(json: String): List<TeacherInsight> {
        val list = mutableListOf<TeacherInsight>()
        try {
            val itemRegex = "\\{[^}]+\\}".toRegex()
            val matches = itemRegex.findAll(json)
            for (m in matches) {
                val itemStr = m.value
                val id = extractString(itemStr, "id") ?: continue
                val typeStr = extractString(itemStr, "type") ?: "PRAISE"
                val type = runCatching { InsightType.valueOf(typeStr) }.getOrDefault(InsightType.PRAISE)
                val title = extractString(itemStr, "title") ?: ""
                val msg = extractString(itemStr, "message") ?: ""
                val dimStr = extractString(itemStr, "relatedDimension")
                val dim = dimStr?.let { runCatching { BiomechanicalDimension.valueOf(it) }.getOrNull() }
                val delta = extractString(itemStr, "metricDelta")
                val attemptId = extractString(itemStr, "relatedAttemptId")
                val targetTitle = extractString(itemStr, "relatedTargetTitle")
                val score = extractInt(itemStr, "relatedScore")

                list.add(
                    TeacherInsight(
                        id = id,
                        type = type,
                        title = title,
                        message = msg,
                        relatedDimension = dim,
                        metricDelta = delta,
                        relatedAttemptId = attemptId,
                        relatedTargetTitle = targetTitle,
                        relatedScore = score
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractFloat(json: String, key: String): Float? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractArrayBlock(json: String, key: String): String? {
        val startIndex = json.indexOf("\"$key\"")
        if (startIndex == -1) return null
        val arrayStart = json.indexOf('[', startIndex)
        if (arrayStart == -1) return null
        var depth = 0
        for (i in arrayStart until json.length) {
            if (json[i] == '[') depth++
            else if (json[i] == ']') {
                depth--
                if (depth == 0) {
                    return json.substring(arrayStart, i + 1)
                }
            }
        }
        return null
    }
}
