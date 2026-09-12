package com.scribe.caligrafia.style.model

import com.scribe.caligrafia.core.model.GuidelineRatio
import org.junit.Assert.*
import org.junit.Test

class ScribeStyleTest {

    @Test
    fun builtInStyles_allHaveValidPropertiesAndDuctusRules() {
        val styles = BuiltInStyles.ALL
        assertEquals("Devem existir exatamente 3 estilos pré-instalados", 3, styles.size)

        for (style in styles) {
            assertTrue("ID não pode ser vazio", style.id.isNotBlank())
            assertTrue("Nome não pode ser vazio", style.name.isNotBlank())
            assertTrue("Descrição deve ser informativa", style.description.length > 20)
            assertTrue("Alfabeto de exemplo deve conter letras", style.sampleAlphabet.length >= 26)
            assertTrue("Deve ter ao menos uma regra de ductus", style.ductusRules.isNotEmpty())
            assertTrue("Espessura recomendada deve ser positiva", style.recommendedStrokeWidthPx > 0f)
            assertTrue("Ângulo padrão de inclinação deve estar em intervalo válido", style.defaultSlantAngle in 45f..90f)
            assertFalse("Estilos embutidos não são customizados", style.isCustom)
            assertNull("Estilos embutidos não possuem caminho externo de fonte", style.customFontPath)
        }
    }

    @Test
    fun cursivaEscolar_hasProperProportionsAndSlant() {
        val style = BuiltInStyles.CURSIVA_ESCOLAR
        assertEquals("cursiva_escolar", style.id)
        assertEquals(GuidelineRatio.Ratio111, style.recommendedRatio)
        assertEquals(68.0f, style.defaultSlantAngle, 0.001f)
        assertEquals(1.0f, style.contrastRatio, 0.001f) // Monoline uniforme
        assertEquals(StyleCategory.FOUNDATIONAL, style.category)

        val config = style.toGuidelineConfig(xHeightPx = 60f)
        assertEquals(60f, config.xHeightPx, 0.001f)
        assertNotNull(config.slant)
        assertEquals(68.0f, config.slant?.angleDegrees ?: 0f, 0.001f)
    }

    @Test
    fun copperplate_hasClassicalProportionsAndSteepSlant() {
        val style = BuiltInStyles.COPPERPLATE
        assertEquals("copperplate", style.id)
        assertEquals(GuidelineRatio.Ratio323, style.recommendedRatio)
        assertEquals(52.0f, style.defaultSlantAngle, 0.001f)
        assertEquals(2.8f, style.contrastRatio, 0.001f) // Alto contraste
        assertEquals(StyleCategory.CALLIGRAPHIC, style.category)

        val config = style.toGuidelineConfig(xHeightPx = 80f)
        assertEquals(80f, config.xHeightPx, 0.001f)
        assertNotNull(config.slant)
        assertEquals(52.0f, config.slant?.angleDegrees ?: 0f, 0.001f)
    }

    @Test
    fun spencerian_hasSlenderProportionsAndShadedDuctus() {
        val style = BuiltInStyles.SPENCERIAN
        assertEquals("spencerian", style.id)
        assertEquals(GuidelineRatio.Ratio212, style.recommendedRatio)
        assertEquals(52.0f, style.defaultSlantAngle, 0.001f)
        assertEquals(StyleCategory.ORNAMENTAL, style.category)
        assertTrue("Deve conter regras de sombreamento pontual", style.ductusRules.any { it.pressureBehavior == PressureBehavior.SHADED_ACCENT })
    }
}
