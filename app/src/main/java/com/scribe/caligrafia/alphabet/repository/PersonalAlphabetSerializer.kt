package com.scribe.caligrafia.alphabet.repository

import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph

/**
 * Serializador e parser JSON puro em Kotlin para o manifesto do Alfabeto Pessoal (SCR-603 — M6).
 *
 * Não depende de stubs do framework Android (org.json), garantindo portabilidade e execução determinística na JVM.
 */
object PersonalAlphabetSerializer {

    fun serialize(alphabet: PersonalAlphabet): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"id\": \"${escape(alphabet.id)}\",\n")
        sb.append("  \"name\": \"${escape(alphabet.name)}\",\n")
        sb.append("  \"lastCompiledStyleId\": ${if (alphabet.lastCompiledStyleId != null) "\"${escape(alphabet.lastCompiledStyleId)}\"" else "null"},\n")
        sb.append("  \"lastCompiledTimestamp\": ${alphabet.lastCompiledTimestamp ?: "null"},\n")
        sb.append("  \"glyphs\": [\n")

        val glyphList = alphabet.glyphs.values.toList()
        glyphList.forEachIndexed { gIdx, glyph ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escape(glyph.id)}\",\n")
            sb.append("      \"symbol\": \"${escape(glyph.symbol)}\",\n")
            sb.append("      \"name\": \"${escape(glyph.name)}\",\n")
            sb.append("      \"category\": \"${glyph.category.name}\",\n")
            sb.append("      \"selectedVariantId\": ${if (glyph.selectedVariantId != null) "\"${escape(glyph.selectedVariantId)}\"" else "null"},\n")
            sb.append("      \"updatedAtTimestamp\": ${glyph.updatedAtTimestamp},\n")
            sb.append("      \"variants\": [\n")

            glyph.variants.forEachIndexed { vIdx, v ->
                sb.append("        {\n")
                sb.append("          \"id\": \"${escape(v.id)}\",\n")
                sb.append("          \"glyphId\": \"${escape(v.glyphId)}\",\n")
                sb.append("          \"version\": ${v.version},\n")
                sb.append("          \"label\": \"${escape(v.label)}\",\n")
                sb.append("          \"score\": ${v.score},\n")
                sb.append("          \"slantAngleDegrees\": ${v.slantAngleDegrees},\n")
                sb.append("          \"isFavorite\": ${v.isFavorite},\n")
                sb.append("          \"strokeCount\": ${v.strokeCount},\n")
                sb.append("          \"createdAtTimestamp\": ${v.createdAtTimestamp},\n")
                sb.append("          \"sourceAttemptId\": ${if (v.sourceAttemptId != null) "\"${escape(v.sourceAttemptId)}\"" else "null"}\n")
                sb.append("        }${if (vIdx < glyph.variants.size - 1) "," else ""}\n")
            }

            sb.append("      ]\n")
            sb.append("    }${if (gIdx < glyphList.size - 1) "," else ""}\n")
        }

        sb.append("  ]\n}")
        return sb.toString()
    }

    fun deserialize(jsonText: String): PersonalAlphabet {
        val rootId = extractString(jsonText, "id") ?: "my_personal_alphabet"
        val rootName = extractString(jsonText, "name") ?: "Meu Alfabeto"
        val lastStyleId = extractString(jsonText, "lastCompiledStyleId")
        val lastTimestamp = extractLong(jsonText, "lastCompiledTimestamp")

        val glyphsMap = mutableMapOf<String, PersonalGlyph>()
        val glyphsSection = jsonText.substringAfter("\"glyphs\"", "")

        val glyphObjects = extractJsonObjects(glyphsSection)
        for (gObj in glyphObjects) {
            val id = extractString(gObj, "id") ?: continue
            val symbol = extractString(gObj, "symbol") ?: ""
            val name = extractString(gObj, "name") ?: symbol
            val catStr = extractString(gObj, "category") ?: "LOWERCASE"
            val category = try {
                AlphabetCategory.valueOf(catStr)
            } catch (_: Throwable) {
                AlphabetCategory.LOWERCASE
            }
            val selectedVariantId = extractString(gObj, "selectedVariantId")
            val updatedAt = extractLong(gObj, "updatedAtTimestamp") ?: 0L

            val variantsList = mutableListOf<GlyphVariant>()
            val variantsSection = gObj.substringAfter("\"variants\"", "")
            val variantObjects = extractJsonObjects(variantsSection)

            for (vObj in variantObjects) {
                val vId = extractString(vObj, "id") ?: continue
                val vGlyphId = extractString(vObj, "glyphId") ?: id
                val version = extractInt(vObj, "version") ?: 1
                val label = extractString(vObj, "label") ?: "v$version"
                val score = extractFloat(vObj, "score") ?: 0f
                val slant = extractFloat(vObj, "slantAngleDegrees") ?: 60f
                val isFav = extractBoolean(vObj, "isFavorite") ?: false
                val strokeCount = extractInt(vObj, "strokeCount") ?: 0
                val createdAt = extractLong(vObj, "createdAtTimestamp") ?: 0L
                val sourceAttemptId = extractString(vObj, "sourceAttemptId")

                variantsList.add(
                    GlyphVariant(
                        id = vId,
                        glyphId = vGlyphId,
                        version = version,
                        label = label,
                        score = score,
                        slantAngleDegrees = slant,
                        isFavorite = isFav,
                        strokeCount = strokeCount,
                        createdAtTimestamp = createdAt,
                        sourceAttemptId = sourceAttemptId
                    )
                )
            }

            glyphsMap[id] = PersonalGlyph(
                id = id,
                symbol = symbol,
                name = name,
                category = category,
                selectedVariantId = selectedVariantId,
                variants = variantsList,
                updatedAtTimestamp = updatedAt
            )
        }

        return PersonalAlphabet(
            id = rootId,
            name = rootName,
            glyphs = glyphsMap,
            lastCompiledStyleId = lastStyleId,
            lastCompiledTimestamp = lastTimestamp
        )
    }

    private fun escape(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractJsonObjects(text: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var startIndex = -1
        var inQuotes = false
        var escapeNext = false

        for (i in text.indices) {
            val c = text[i]
            if (escapeNext) {
                escapeNext = false
                continue
            }
            if (c == '\\') {
                escapeNext = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) startIndex = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        list.add(text.substring(startIndex, i + 1))
                        startIndex = -1
                    }
                }
            }
        }
        return list
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.unescape()
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
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+(\\.\\d+)?)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun String.unescape(): String {
        return this.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
