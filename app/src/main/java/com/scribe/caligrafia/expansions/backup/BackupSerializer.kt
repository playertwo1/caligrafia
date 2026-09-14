package com.scribe.caligrafia.expansions.backup

/**
 * Serializador JSON estrito do manifesto de backup (.scribepack).
 * Não depende de android.jar e rejeita gramática incompleta, lixo após o objeto e tipos inválidos.
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
  "spacedRepetitionCount": ${manifest.spacedRepetitionCount},
  "practiceAttemptCount": ${manifest.practiceAttemptCount},
  "passageCopyCount": ${manifest.passageCopyCount},
  "personalStyleCount": ${manifest.personalStyleCount},
  "importedFontCount": ${manifest.importedFontCount},
  "signatureReferenceCount": ${manifest.signatureReferenceCount},
  "hasTeacherDiagnostic": ${manifest.hasTeacherDiagnostic},
  "hasPreferencesSnapshot": ${manifest.hasPreferencesSnapshot}
}
""".trimIndent()
    }

    fun deserializeManifest(json: String): BackupManifest? {
        val map = parseJsonObject(json) ?: return null
        val formatVersion = map["formatVersion"] as? String ?: return null
        if (formatVersion != "1.0") return null

        fun intField(name: String, default: Int = 0): Int? {
            val value = map[name] ?: return default
            return (value as? Number)?.toInt()
        }

        fun longField(name: String, default: Long): Long? {
            val value = map[name] ?: return default
            return (value as? Number)?.toLong()
        }

        fun boolField(name: String, default: Boolean = false): Boolean? {
            val value = map[name] ?: return default
            return value as? Boolean
        }

        val appVersion = map["appVersion"] as? String ?: "0.8.0"
        val deviceInfo = map["deviceInfo"] as? String ?: "Android"

        return BackupManifest(
            formatVersion = formatVersion,
            appVersion = appVersion,
            appVersionCode = intField("appVersionCode", 10) ?: return null,
            createdAtMs = longField("createdAtMs", 0L) ?: return null,
            deviceInfo = deviceInfo,
            notebookCount = intField("notebookCount") ?: return null,
            pageCount = intField("pageCount") ?: return null,
            personalGlyphCount = intField("personalGlyphCount") ?: return null,
            lessonHistoryCount = intField("lessonHistoryCount") ?: return null,
            spacedRepetitionCount = intField("spacedRepetitionCount") ?: return null,
            practiceAttemptCount = intField("practiceAttemptCount") ?: return null,
            passageCopyCount = intField("passageCopyCount") ?: return null,
            personalStyleCount = intField("personalStyleCount") ?: return null,
            importedFontCount = intField("importedFontCount") ?: return null,
            signatureReferenceCount = intField("signatureReferenceCount") ?: return null,
            hasTeacherDiagnostic = boolField("hasTeacherDiagnostic") ?: return null,
            hasPreferencesSnapshot = boolField("hasPreferencesSnapshot") ?: return null
        )
    }

    fun parseJsonObject(json: String): Map<String, Any?>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        return JsonParser(trimmed).parseTopLevelObject()
    }

    private class JsonParser(private val trimmed: String) {
        private var index = 0

        private class ParsedValue(val value: Any?)

        fun parseTopLevelObject(): Map<String, Any?>? {
            val map = parseObject() ?: return null
            skipWhitespace()
            return if (index == trimmed.length) map else null
        }

        private fun parseObject(): Map<String, Any?>? {
            skipWhitespace()
            if (index >= trimmed.length || trimmed[index] != '{') return null
            index++
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
                index++
                skipWhitespace()
                val parsed = parseValue() ?: return null
                map[key] = parsed.value
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
            while (index < trimmed.length && trimmed[index].isWhitespace()) index++
        }

        private fun parseString(): String? {
            if (index >= trimmed.length || trimmed[index] != '"') return null
            index++
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
                } else if (c < ' ') {
                    return null
                } else {
                    sb.append(c)
                }
            }
            return null
        }

        private fun parseNumber(): Number? {
            val start = index
            if (index < trimmed.length && trimmed[index] == '-') index++
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
                var hasFractionDigits = false
                while (index < trimmed.length && trimmed[index].isDigit()) {
                    index++
                    hasFractionDigits = true
                }
                if (!hasFractionDigits) return null
            }
            if (index < trimmed.length && (trimmed[index] == 'e' || trimmed[index] == 'E')) {
                isDouble = true
                index++
                if (index < trimmed.length && (trimmed[index] == '-' || trimmed[index] == '+')) index++
                var hasExpDigits = false
                while (index < trimmed.length && trimmed[index].isDigit()) {
                    index++
                    hasExpDigits = true
                }
                if (!hasExpDigits) return null
            }
            val numStr = trimmed.substring(start, index)
            return if (isDouble) numStr.toDoubleOrNull() else numStr.toLongOrNull()
        }

        private fun parseValue(): ParsedValue? {
            skipWhitespace()
            if (index >= trimmed.length) return null
            return when (trimmed[index]) {
                '"' -> ParsedValue(parseString() ?: return null)
                '{' -> ParsedValue(parseObject() ?: return null)
                '[' -> ParsedValue(parseArray() ?: return null)
                't' -> if (trimmed.startsWith("true", index)) {
                    index += 4
                    ParsedValue(true)
                } else null
                'f' -> if (trimmed.startsWith("false", index)) {
                    index += 5
                    ParsedValue(false)
                } else null
                'n' -> if (trimmed.startsWith("null", index)) {
                    index += 4
                    ParsedValue(null)
                } else null
                '-', in '0'..'9' -> ParsedValue(parseNumber() ?: return null)
                else -> null
            }
        }

        private fun parseArray(): List<Any?>? {
            if (index >= trimmed.length || trimmed[index] != '[') return null
            index++
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (index < trimmed.length && trimmed[index] == ']') {
                index++
                return list
            }
            while (index < trimmed.length) {
                val parsed = parseValue() ?: return null
                list.add(parsed.value)
                skipWhitespace()
                if (index >= trimmed.length) return null
                if (trimmed[index] == ',') {
                    index++
                    skipWhitespace()
                    if (index < trimmed.length && trimmed[index] == ']') return null
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
