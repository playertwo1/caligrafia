package com.scribe.caligrafia.alphabet.engine

import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.style.model.DuctusRule
import com.scribe.caligrafia.style.model.PressureBehavior
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.style.model.StyleCategory
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Motor de Compilação de Estilo Pessoal (SCR-602 — M6).
 *
 * Responsável por analisar as características biométricas e geométricas dos traços
 * das variantes favoritas do usuário e derivar um [ScribeStyle] canônico personalizado.
 *
 * Princípios:
 * 1. 100% determinístico e local (zero IA / nuvem).
 * 2. Análise puramente vetorial sobre os pontos brutos imutáveis.
 * 3. Tolerante a dados parciais (gera estilo consistente mesmo com poucas letras cadastradas).
 */
class PersonalStyleCompiler {

    /**
     * Compila um [ScribeStyle] a partir do [PersonalAlphabet] do usuário.
     *
     * @param alphabet O alfabeto contendo os glifos e variantes do usuário.
     * @param styleName Nome opcional para o estilo gerado (padrão: "Meu Estilo Pessoal").
     * @param styleId Identificador fixo ou dinâmico para o estilo.
     */
    fun compile(
        alphabet: PersonalAlphabet,
        styleName: String = "Meu Estilo Pessoal",
        styleId: String = "personal_style_v1"
    ): ScribeStyle {
        val activeVariants = alphabet.glyphs.values
            .mapNotNull { it.activeVariant }

        val slantAngle = calculateAverageSlant(activeVariants)
        val ratio = estimateGuidelineRatio(alphabet)
        val strokeWidth = calculateAverageStrokeWidth(activeVariants)
        val contrast = calculateContrastRatio(activeVariants)

        val sampleAlphabetBuilder = StringBuilder()
        // Adiciona caracteres minúsculos existentes ou fallback
        val lowercaseSymbols = alphabet.glyphs.values
            .filter { it.category == AlphabetCategory.LOWERCASE && it.isCompleted }
            .map { it.symbol }
        if (lowercaseSymbols.isNotEmpty()) {
            sampleAlphabetBuilder.append(lowercaseSymbols.joinToString(""))
        } else {
            sampleAlphabetBuilder.append("abcdefghijklmnopqrstuvwxyz")
        }

        sampleAlphabetBuilder.append(" ")

        // Adiciona maiúsculas
        val uppercaseSymbols = alphabet.glyphs.values
            .filter { it.category == AlphabetCategory.UPPERCASE && it.isCompleted }
            .map { it.symbol }
        if (uppercaseSymbols.isNotEmpty()) {
            sampleAlphabetBuilder.append(uppercaseSymbols.joinToString(""))
        } else {
            sampleAlphabetBuilder.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        }

        val ductus = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Ductus Personalizado",
                instruction = "Inclinação média observada: ${slantAngle.roundToInt()}°",
                pressureBehavior = if (contrast > 1.3f) PressureBehavior.DOWNSTROKE_HEAVY else PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Modulação de Pressão",
                instruction = if (contrast > 1.3f) "Descendentes com modulação e espessura acentuada" else "Traçado uniforme e monolinear",
                pressureBehavior = if (contrast > 1.3f) PressureBehavior.DOWNSTROKE_HEAVY else PressureBehavior.UNIFORM
            )
        )

        return ScribeStyle(
            id = styleId,
            name = styleName,
            description = "Estilo pessoal derivado da curadoria de ${activeVariants.size} glifos do seu alfabeto (inclinação ${slantAngle.roundToInt()}°).",
            category = StyleCategory.PERSONAL,
            recommendedRatio = ratio,
            defaultSlantAngle = slantAngle,
            recommendedStrokeWidthPx = strokeWidth,
            contrastRatio = contrast,
            sampleAlphabet = sampleAlphabetBuilder.toString().trim(),
            ductusRules = ductus,
            isCustom = true
        )
    }

    /**
     * Calcula o ângulo de inclinação médio ($\theta$) a partir das variantes ativas.
     * Analisa tanto os valores pré-computados de slant quanto a direção dos traços descendentes.
     */
    fun calculateAverageSlant(variants: List<GlyphVariant>): Float {
        if (variants.isEmpty()) return 60.0f

        val recordedAngles = variants
            .map { it.slantAngleDegrees }
            .filter { it in 45.0f..90.0f }

        if (recordedAngles.isNotEmpty()) {
            return (recordedAngles.average().toFloat() * 10f).roundToInt() / 10f
        }

        // Se não houver ângulos registrados, estima a partir dos traços vetoriais brutos
        val measuredAngles = mutableListOf<Float>()
        for (variant in variants) {
            for (stroke in variant.strokes) {
                val pts = stroke.points
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val dy = p2.y - p1.y
                        val dx = p2.x - p1.x
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                        // Traços descendentes significativos (dy > 4px, dist > 5px)
                        if (dy > 4f && dist > 5f) {
                            // Convenção canônica caligráfica: atan2(dy, -dx)
                            val angleRad = atan2(dy.toDouble(), (-dx).toDouble())
                            var deg = Math.toDegrees(angleRad).toFloat()
                            if (deg < 0) deg += 180f
                            if (deg in 45f..90f) {
                                measuredAngles.add(deg)
                            }
                        }
                    }
                    // Endpoints diretos
                    val pFirst = pts.first()
                    val pLast = pts.last()
                    val totalDy = pLast.y - pFirst.y
                    val totalDx = pLast.x - pFirst.x
                    if (totalDy > 8f) {
                        val angleRad = atan2(totalDy.toDouble(), (-totalDx).toDouble())
                        var deg = Math.toDegrees(angleRad).toFloat()
                        if (deg < 0) deg += 180f
                        if (deg in 45f..90f) {
                            measuredAngles.add(deg)
                        }
                    }
                }
            }
        }

        return if (measuredAngles.isNotEmpty()) {
            (measuredAngles.average().toFloat() * 10f).roundToInt() / 10f
        } else {
            60.0f
        }
    }

    /**
     * Estima a proporção canônica de pauta recomendada baseada nos glifos com hastes (ascendentes/descendentes).
     */
    fun estimateGuidelineRatio(alphabet: PersonalAlphabet): GuidelineRatio {
        val ascenderGlyphs = listOf("glyph_lower_l", "glyph_lower_t", "glyph_lower_b", "glyph_lower_d", "glyph_lower_h", "glyph_lower_k")
        val normalGlyphs = listOf("glyph_lower_a", "glyph_lower_c", "glyph_lower_o", "glyph_lower_i", "glyph_lower_m", "glyph_lower_n")

        val hasAscenders = ascenderGlyphs.any { alphabet.glyphs[it]?.isCompleted == true }
        val hasNormal = normalGlyphs.any { alphabet.glyphs[it]?.isCompleted == true }

        return if (hasAscenders && hasNormal) {
            // Analisa alturas relativas médias
            val avgAscenderHeight = ascenderGlyphs
                .mapNotNull { alphabet.glyphs[it]?.activeVariant }
                .mapNotNull { computeBoundingHeight(it) }
                .average()

            val avgNormalHeight = normalGlyphs
                .mapNotNull { alphabet.glyphs[it]?.activeVariant }
                .mapNotNull { computeBoundingHeight(it) }
                .average()

            if (avgNormalHeight > 0.0 && avgAscenderHeight > 0.0) {
                val ratio = avgAscenderHeight / avgNormalHeight
                when {
                    ratio >= 2.4 -> GuidelineRatio.Ratio212 // Estilo de alta ascensão (Copperplate 2:1:2)
                    ratio >= 1.7 -> GuidelineRatio.Ratio323 // Estilo moderado (Spencerian/Itálica 3:2:3)
                    else -> GuidelineRatio.Ratio111         // Estilo compacto/escolar (1:1:1)
                }
            } else {
                GuidelineRatio.Ratio212
            }
        } else {
            // Padrão equilibrado
            GuidelineRatio.Ratio212
        }
    }

    /**
     * Calcula a espessura média de traço a partir das variantes ativas.
     */
    fun calculateAverageStrokeWidth(variants: List<GlyphVariant>): Float {
        if (variants.isEmpty()) return 5.0f

        val widths = variants.flatMap { v ->
            v.strokes.mapNotNull { it.baseWidthPx }
        }

        return if (widths.isNotEmpty()) {
            (widths.average().toFloat() * 10f).roundToInt() / 10f
        } else {
            5.0f
        }
    }

    /**
     * Calcula a taxa de contraste (modulação de pressão entre descendentes pesados e ascendentes finos).
     */
    fun calculateContrastRatio(variants: List<GlyphVariant>): Float {
        if (variants.isEmpty()) return 1.0f

        val pressures = variants.flatMap { v ->
            v.strokes.flatMap { s ->
                s.points.mapNotNull { it.pressure }
            }
        }.filter { it > 0.05f }

        if (pressures.size < 10) return 1.0f

        val sorted = pressures.sorted()
        val lowIdx = (sorted.size * 0.1f).toInt()
        val highIdx = (sorted.size * 0.9f).toInt()

        val pLow = sorted.getOrNull(lowIdx) ?: 0.2f
        val pHigh = sorted.getOrNull(highIdx) ?: 0.6f

        val ratio = (pHigh / pLow.coerceAtLeast(0.1f)).coerceIn(1.0f, 3.0f)
        return (ratio * 10f).roundToInt() / 10f
    }

    private fun computeBoundingHeight(variant: GlyphVariant): Double? {
        val yCoords = variant.strokes.flatMap { s -> s.points.map { it.y } }
        if (yCoords.size < 2) return null
        return (yCoords.maxOrNull()!! - yCoords.minOrNull()!!).toDouble().coerceAtLeast(1.0)
    }
}
