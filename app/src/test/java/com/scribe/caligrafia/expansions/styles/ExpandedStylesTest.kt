package com.scribe.caligrafia.expansions.styles

import com.scribe.caligrafia.style.engine.StyleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedStylesTest {

    @Test
    fun expandedStyles_allHaveValidProperties() {
        val styles = ExpandedStyles.allExpandedStyles
        assertEquals("Devem existir exatamente 3 estilos expandidos no M8", 3, styles.size)

        for (style in styles) {
            assertTrue("ID não pode ser vazio", style.id.isNotBlank())
            assertTrue("Nome não pode ser vazio", style.name.isNotBlank())
            assertTrue("Descrição deve ser informativa", style.description.length > 25)
            assertTrue("Deve possuir regras de ductus", style.ductusRules.isNotEmpty())
            assertTrue("Espessura recomendada deve ser positiva", style.recommendedStrokeWidthPx > 0f)
            assertTrue("Slant angle em intervalo válido", style.defaultSlantAngle in 45f..90f)
        }
    }

    @Test
    fun styleEngine_resolvesExpandedStyles() {
        val engine = StyleEngine()
        val allAvailable = engine.getAvailableStyles()

        assertTrue("StyleEngine deve conter ao menos 6 estilos (3 built-in + 3 expanded)", allAvailable.size >= 6)

        val gotica = engine.getStyle("gotica_textura")
        assertNotNull(gotica)
        assertEquals("Gótica Textura Quadrata", gotica.name)
        assertEquals(90.0f, gotica.defaultSlantAngle, 0.01f)

        val italica = engine.getStyle("italica_chanceleresca")
        assertNotNull(italica)
        assertEquals("Itálica Chanceleresca", italica.name)
        assertEquals(85.0f, italica.defaultSlantAngle, 0.01f)

        val uncial = engine.getStyle("uncial_classica")
        assertNotNull(uncial)
        assertEquals("Uncial Clássica", uncial.name)
    }
}
