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
        val map = parseJsonObject(json) ?: return null
        val formatVersion = map["formatVersion"] as? String ?: return null
        if (formatVersion != "1.0") {
            return null // Versão não suportada
        }
        val appVersion = map["appVersion"] as? String ?: "0.8.0"
        val appVersionCode = (map["appVersionCode"] as? Number)?.toInt() ?: 10
        val createdAtMs = (map["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val deviceInfo = map["deviceInfo"] as? String ?: "Android"
        val notebookCount = (map["notebookCount"] as? Number)?.toInt() ?: 0
        val pageCount = (map["pageCount"] as? Number)?.toInt() ?: 0
        val personalGlyphCount = (map["personalGlyphCount"] as? Number)?.toInt() ?: 0
        val lessonHistoryCount = (map["lessonHistoryCount"] as? Number)?.toInt() ?: 0
        val practiceAttemptCount = (map["practiceAttemptCount"] as? Number)?.toInt() ?: 0
        val hasTeacherDiagnostic = map["hasTeacherDiagnostic"] as? Boolean ?: false

        return BackupManifest(
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
    }

    fun parseJsonObject(json: String): Map<String, Any?>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        return JsonParser(trimmed).parseTopLevelObject()
    }

    private class JsonParser(private val trimmed: String) {
        private var index = 0

        fun parseTopLevelObject(): Map<String, Any?>? {
            val map = parseObject() ?: return null
            skipWhitespace()
            return if (index == trimmed.length) map else null
        }

        fun parseObject(): Map<String, Any?>? {
            skipWhitespace()
            if (index >= trimmed.length || trimmed[index] != '{') return null
            index++ // skip {
            val map = mutableMapOf<String, Any?>()
            skipWhitespace()
            if (index < trimmed.length && trimmed[index] == '}') {
                index++
                return map
            }
            while (index < trimmed.length) {
                skipWhitespace()
                val key = parseString() ?: return null
                skipWhitespace()
                if (index >= trimmed.length || trimmed[index] != ':') return null
                index++ // skip :
                skipWhitespace()
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (index >= trimmed.length) return null
                if (trimmed[index] == ',') {
                    index++
                    skipWhitespace()
                    if (index < trimmed.length && trimmed[index] == '}') return null
                } else if (trimmed[index] == '}') {
                    index++
                    return map
                } else {
                    return null
                }
            }
            return null
        }

        private fun skipWhitespace() {
            while (index < trimmed.length && trimmed[index].isWhitespace()) {
                index++
            }
        }

        private fun parseString(): String? {
            if (index >= trimmed.length || trimmed[index] != '"') return null
            index++ // skip opening quote
            val sb = StringBuilder()
            while (index < trimmed.length) {
                val c = trimmed[index++]
                if (c == '\\') {
                    if (index >= trimmed.length) return null
                    when (trimmed[index++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 > trimmed.length) return null
                            val hex = trimmed.substring(index, index + 4)
                            val code = hex.toIntOrNull(16) ?: return null
                            sb.append(code.toChar())
                            index += 4
                        }
                        else -> return null
                    }
                } else if (c == '"') {
                    return sb.toString()
                } else if (c < ' ' && c != '\t' && c != '\r' && c != '\n') {
                    return null
                } else {
                    sb.append(c)
                }
            }
            return null
        }

        private fun parseNumber(): Number? {
            val start = index
            if (index < trimmed.length && (trimmed[index] == '-' || trimmed[index] == '+')) {
                index++
            }
            var hasDigits = false
            while (index < trimmed.length && trimmed[index].isDigit()) {
                index++
                hasDigits = true
            }
            if (!hasDigits) return null
            var isDouble = false
            if (index < trimmed.length && trimmed[index] == '.') {
                isDouble = true
                index++
                while (index < trimmed.length && trimmed[index].isDigit()) {
                    index++
                }
            }
            if (index < trimmed.length && (trimmed[index] == 'e' || trimmed[index] == 'E')) {
                isDouble = true
                index++
                if (index < trimmed.length && (trimmed[index] == '-' || trimmed[index] == '+')) index++
                while (index < trimmed.length && trimmed[index].isDigit()) index++
            }
            val numStr = trimmed.substring(start, index)
            return if (isDouble) numStr.toDoubleOrNull() else numStr.toLongOrNull()
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (index >= trimmed.length) return null
            return when (trimmed[index]) {
                '"' -> parseString()
                '{' -> parseObject()
                '[' -> parseArray()
                't' -> {
                    if (trimmed.startsWith("true", index)) {
                        index += 4
                        true
                    } else null
                }
                'f' -> {
                    if (trimmed.startsWith("false", index)) {
                        index += 5
                        false
                    } else null
                }
                'n' -> {
                    if (trimmed.startsWith("null", index)) {
                        index += 4
                        null
                    } else null
                }
                '-', in '0'..'9' -> parseNumber()
                else -> null
            }
        }

        fun parseArray(): List<Any?>? {
            if (index >= trimmed.length || trimmed[index] != '[') return null
            index++ // skip [
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (index < trimmed.length && trimmed[index] == ']') {
                index++
                return list
            }
            while (index < trimmed.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (index >= trimmed.length) return null
                if (trimmed[index] == ',') {
                    index++
                    skipWhitespace()
                } else if (trimmed[index] == ']') {
                    index++
                    return list
                } else {
                    return null
                }
            }
            return null
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
