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
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }
        return try {
            val formatVersion = extractString(trimmed, "formatVersion") ?: return null
            if (formatVersion != "1.0") {
                return null // Versão não suportada
            }
            val appVersion = extractString(trimmed, "appVersion") ?: "0.8.0"
            val appVersionCode = extractInt(trimmed, "appVersionCode") ?: 10
            val createdAtMs = extractLong(trimmed, "createdAtMs") ?: System.currentTimeMillis()
            val deviceInfo = extractString(trimmed, "deviceInfo") ?: "Android"
            val notebookCount = extractInt(trimmed, "notebookCount") ?: 0
            val pageCount = extractInt(trimmed, "pageCount") ?: 0
            val personalGlyphCount = extractInt(trimmed, "personalGlyphCount") ?: 0
            val lessonHistoryCount = extractInt(trimmed, "lessonHistoryCount") ?: 0
            val practiceAttemptCount = extractInt(trimmed, "practiceAttemptCount") ?: 0
            val hasTeacherDiagnostic = extractBoolean(trimmed, "hasTeacherDiagnostic") ?: false

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
        val keyIdx = json.indexOf("\"$key\"")
        if (keyIdx < 0) return null
        val colonIdx = json.indexOf(':', keyIdx + key.length + 2)
        if (colonIdx < 0) return null
        val startQuote = json.indexOf('"', colonIdx + 1)
        if (startQuote < 0) return null

        val sb = StringBuilder()
        var i = startQuote + 1
        var escaped = false
        while (i < json.length) {
            val c = json[i]
            if (escaped) {
                when (c) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 < json.length) {
                            val hex = json.substring(i + 1, i + 5)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            } else {
                                sb.append('u')
                            }
                        } else {
                            sb.append('u')
                        }
                    }
                    else -> sb.append(c)
                }
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                return sb.toString()
            } else {
                sb.append(c)
            }
            i++
        }
        return null
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
