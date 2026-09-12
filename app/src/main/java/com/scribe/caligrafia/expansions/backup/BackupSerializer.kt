package com.scribe.caligrafia.expansions.backup

/**
 * Serializador JSON puro em Kotlin para o manifesto de backup (.scribepack).
 * Não depende de android.jar para assegurar execução 100% verde em testes de JVM.
 */
object BackupSerializer {

    fun serializeManifest(manifest: BackupManifest): String {
        return """
{
  "formatVersion": "${escapeJson(manifest.formatVersion)}",
  "appVersion": "${escapeJson(manifest.appVersion)}",
  "appVersionCode": ${manifest.appVersionCode},
  "createdAtMs": ${manifest.createdAtMs},
  "deviceInfo": "${escapeJson(manifest.deviceInfo)}",
  "notebookCount": ${manifest.notebookCount},
  "pageCount": ${manifest.pageCount},
  "personalGlyphCount": ${manifest.personalGlyphCount},
  "lessonHistoryCount": ${manifest.lessonHistoryCount},
  "practiceAttemptCount": ${manifest.practiceAttemptCount},
  "hasTeacherDiagnostic": ${manifest.hasTeacherDiagnostic}
}
""".trimIndent()
    }

    fun deserializeManifest(json: String): BackupManifest? {
        return try {
            val formatVersion = extractString(json, "formatVersion") ?: "1.0"
            val appVersion = extractString(json, "appVersion") ?: "0.8.0"
            val appVersionCode = extractInt(json, "appVersionCode") ?: 10
            val createdAtMs = extractLong(json, "createdAtMs") ?: System.currentTimeMillis()
            val deviceInfo = extractString(json, "deviceInfo") ?: "Android"
            val notebookCount = extractInt(json, "notebookCount") ?: 0
            val pageCount = extractInt(json, "pageCount") ?: 0
            val personalGlyphCount = extractInt(json, "personalGlyphCount") ?: 0
            val lessonHistoryCount = extractInt(json, "lessonHistoryCount") ?: 0
            val practiceAttemptCount = extractInt(json, "practiceAttemptCount") ?: 0
            val hasTeacherDiagnostic = extractBoolean(json, "hasTeacherDiagnostic") ?: false

            BackupManifest(
                formatVersion = formatVersion,
                appVersion = appVersion,
                appVersionCode = appVersionCode,
                createdAtMs = createdAtMs,
                deviceInfo = deviceInfo,
                notebookCount = notebookCount,
                pageCount = pageCount,
                personalGlyphCount = personalGlyphCount,
                lessonHistoryCount = lessonHistoryCount,
                practiceAttemptCount = practiceAttemptCount,
                hasTeacherDiagnostic = hasTeacherDiagnostic
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }
}
