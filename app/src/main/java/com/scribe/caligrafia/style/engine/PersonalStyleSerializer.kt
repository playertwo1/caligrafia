package com.scribe.caligrafia.style.engine

import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.style.model.StyleCategory

/**
 * Serializador e parser puro em Kotlin para persistência de [ScribeStyle] (R05).
 *
 * Não depende de stubs do framework Android (org.json), garantindo execução determinística na JVM.
 */
object PersonalStyleSerializer {

    fun serialize(styles: List<ScribeStyle>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        styles.forEachIndexed { index, s ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${escape(s.id)}\",\n")
            sb.append("    \"name\": \"${escape(s.name)}\",\n")
            sb.append("    \"description\": \"${escape(s.description)}\",\n")
            sb.append("    \"category\": \"${s.category.name}\",\n")
            sb.append("    \"recommendedRatio\": \"${ratioToString(s.recommendedRatio)}\",\n")
            sb.append("    \"defaultSlantAngle\": ${s.defaultSlantAngle},\n")
            sb.append("    \"recommendedStrokeWidthPx\": ${s.recommendedStrokeWidthPx},\n")
            sb.append("    \"contrastRatio\": ${s.contrastRatio},\n")
            sb.append("    \"sampleAlphabet\": \"${escape(s.sampleAlphabet)}\",\n")
            sb.append("    \"isCustom\": ${s.isCustom}\n")
            sb.append("  }${if (index < styles.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserialize(content: String): List<ScribeStyle> {
        val list = mutableListOf<ScribeStyle>()
        if (content.isBlank()) return list

        val blocks = content.split("}")
        for (block in blocks) {
            val id = extractString(block, "id") ?: continue
            val name = extractString(block, "name") ?: "Meu Estilo Pessoal"
            val description = extractString(block, "description") ?: ""
            val categoryStr = extractString(block, "category")
            val category = categoryStr?.let { runCatching { StyleCategory.valueOf(it) }.getOrNull() } ?: StyleCategory.PERSONAL
            val ratioStr = extractString(block, "recommendedRatio")
            val ratio = stringToRatio(ratioStr)
            val slant = extractFloat(block, "defaultSlantAngle") ?: 52.0f
            val width = extractFloat(block, "recommendedStrokeWidthPx") ?: 5.0f
            val contrast = extractFloat(block, "contrastRatio") ?: 1.0f
            val sample = extractString(block, "sampleAlphabet") ?: ""
            val isCustom = extractBoolean(block, "isCustom") ?: true

            list.add(
                ScribeStyle(
                    id = id,
                    name = name,
                    description = description,
                    category = category,
                    recommendedRatio = ratio,
                    defaultSlantAngle = slant,
                    recommendedStrokeWidthPx = width,
                    contrastRatio = contrast,
                    sampleAlphabet = sample,
                    isCustom = isCustom
                )
            )
        }
        return list
    }

    private fun escape(text: String): String = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun extractString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
    }

    private fun extractFloat(json: String, key: String): Float? {
        val pattern = Regex("\"$key\"\\s*:\\s*([0-9.-]+)")
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = Regex("\"$key\"\\s*:\\s*(true|false)")
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun ratioToString(ratio: GuidelineRatio): String = when (ratio) {
        is GuidelineRatio.Ratio111 -> "Ratio111"
        is GuidelineRatio.Ratio212 -> "Ratio212"
        is GuidelineRatio.Ratio323 -> "Ratio323"
        is GuidelineRatio.Custom -> "Custom:${ratio.ascenderRatio}:${ratio.descenderRatio}"
    }

    private fun stringToRatio(str: String?): GuidelineRatio {
        if (str == null) return GuidelineRatio.Ratio212
        return when {
            str.startsWith("Ratio111") -> GuidelineRatio.Ratio111
            str.startsWith("Ratio212") -> GuidelineRatio.Ratio212
            str.startsWith("Ratio323") -> GuidelineRatio.Ratio323
            str.startsWith("Custom:") -> {
                val parts = str.split(":")
                val asc = parts.getOrNull(1)?.toFloatOrNull() ?: 2.0f
                val desc = parts.getOrNull(2)?.toFloatOrNull() ?: 2.0f
                GuidelineRatio.Custom(asc, desc)
            }
            else -> GuidelineRatio.Ratio212
        }
    }
}
