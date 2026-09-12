package com.scribe.caligrafia.style.model

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.core.model.SlantConfig

/**
 * Categoria pedagógica ou taxonômica do estilo.
 */
enum class StyleCategory {
    /** Caligrafia fundamental escolar / legibilidade estrutural. */
    FOUNDATIONAL,

    /** Caligrafia tradicional com contraste clássico de pena (ex: Copperplate). */
    CALLIGRAPHIC,

    /** Caligrafia rápida e ornamental (ex: Spencerian, American Cursive). */
    ORNAMENTAL,

    /** Estilo visual importado de arquivo de fonte TTF / OTF local. */
    CUSTOM_FONT,

    /** Estilo pessoal derivado da curadoria das melhores variantes do usuário (M6 — Meu Alfabeto). */
    PERSONAL
}

/**
 * Formato Canônico ScribeStyle v1 (M3 — Style Engine).
 *
 * Encapsula a identidade pedagógica e visual de um método ou estilo caligráfico:
 * 1. Proporção recomendada de pauta (ascender, altura-x, descender).
 * 2. Inclinação padrão (slant angle).
 * 3. Modulação de espessura e regras de pressão.
 * 4. Catálogo de regras de ductus para orientação pedagógica.
 * 5. Referência visual para renderização sem alteração dos traços vetoriais brutos.
 */
data class ScribeStyle(
    val id: String,
    val name: String,
    val description: String,
    val category: StyleCategory,
    val recommendedRatio: GuidelineRatio,
    val defaultSlantAngle: Float,
    val recommendedStrokeWidthPx: Float,
    val contrastRatio: Float = 1.0f,
    val sampleAlphabet: String,
    val ductusRules: List<DuctusRule> = emptyList(),
    val customFontPath: String? = null,
    val isCustom: Boolean = false
) {
    /**
     * Cria uma configuração de pauta caligráfica [GuidelineConfig] alinhada às dimensões
     * e à inclinação recomendadas por este estilo.
     *
     * @param xHeightPx Altura em pixels da altura-x desejada (padrão 80px).
     */
    fun toGuidelineConfig(xHeightPx: Float = 80f): GuidelineConfig {
        return GuidelineConfig(
            ratio = recommendedRatio,
            xHeightPx = xHeightPx,
            slant = if (defaultSlantAngle != 90f) {
                SlantConfig(angleDegrees = defaultSlantAngle, spacingPx = 80f)
            } else {
                null
            }
        )
    }
}
