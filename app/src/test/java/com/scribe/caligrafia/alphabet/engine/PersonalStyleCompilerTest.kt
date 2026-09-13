package com.scribe.caligrafia.alphabet.engine

import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.style.model.StyleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PersonalStyleCompilerTest {

    private val compiler = PersonalStyleCompiler()

    @Test
    fun calculateAverageSlant_withRecordedAngles_calculatesAccurateAverage() {
        val variants = listOf(
            createVariant("v1", 52.0f),
            createVariant("v2", 56.0f),
            createVariant("v3", 60.0f)
        )

        val average = compiler.calculateAverageSlant(variants)
        assertEquals(56.0f, average, 0.1f)
    }

    @Test
    fun calculateAverageSlant_emptyList_returnsDefaultSixtyDegrees() {
        val average = compiler.calculateAverageSlant(emptyList())
        assertEquals(60.0f, average, 0.1f)
    }

    @Test
    fun estimateGuidelineRatio_withTallAscenders_estimatesSpencerianOrCopperplate() {
        val normalGlyph = PersonalGlyph(
            id = "glyph_lower_a",
            symbol = "a",
            name = "Letra a",
            category = AlphabetCategory.LOWERCASE,
            variants = listOf(
                createVariantWithStrokes("v1", listOf(
                    createStroke(listOf(Pair(10f, 50f), Pair(20f, 90f))) // height = 40
                ))
            )
        )

        val ascenderGlyph = PersonalGlyph(
            id = "glyph_lower_l",
            symbol = "l",
            name = "Letra l",
            category = AlphabetCategory.LOWERCASE,
            variants = listOf(
                createVariantWithStrokes("v2", listOf(
                    createStroke(listOf(Pair(10f, 0f), Pair(20f, 100f))) // height = 100 -> ratio = 2.5 >= 2.4 -> Ratio212
                ))
            )
        )

        val alphabet = PersonalAlphabet(
            glyphs = mapOf(
                "glyph_lower_a" to normalGlyph,
                "glyph_lower_l" to ascenderGlyph
            )
        )

        val ratio = compiler.estimateGuidelineRatio(alphabet)
        assertEquals(GuidelineRatio.Ratio212, ratio)
    }

    @Test
    fun compile_generatesValidPersonalScribeStyle() {
        val glyphA = PersonalGlyph(
            id = "glyph_lower_a",
            symbol = "a",
            name = "Letra a",
            category = AlphabetCategory.LOWERCASE,
            variants = listOf(createVariant("v1", 54.0f, 90f))
        )

        val alphabet = PersonalAlphabet(
            glyphs = mapOf("glyph_lower_a" to glyphA)
        )

        val style = compiler.compile(alphabet, styleName = "Meu Estilo Caligráfico", styleId = "test_style")

        assertNotNull(style)
        assertEquals("test_style", style.id)
        assertEquals("Meu Estilo Caligráfico", style.name)
        assertEquals(StyleCategory.PERSONAL, style.category)
        assertEquals(54.0f, style.defaultSlantAngle, 0.1f)
        assertTrue(style.isCustom)
        assertTrue(style.sampleAlphabet.contains("a"))
    }

    private fun createVariant(id: String, slant: Float, score: Float = 85f): GlyphVariant {
        return GlyphVariant(
            id = id,
            glyphId = "g1",
            version = 1,
            label = "v1",
            score = score,
            slantAngleDegrees = slant,
            isFavorite = true
        )
    }

    private fun createVariantWithStrokes(id: String, strokes: List<Stroke>): GlyphVariant {
        return GlyphVariant(
            id = id,
            glyphId = "g1",
            version = 1,
            label = "v1",
            score = 90f,
            slantAngleDegrees = 55f,
            isFavorite = true,
            strokes = strokes
        )
    }

    private fun createStroke(points: List<Pair<Float, Float>>): Stroke {
        return Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = points.mapIndexed { i, (x, y) ->
                StrokePoint(x, y, 1000L + (i * 50), 0.5f, 0f, 0f)
            },
            startedAtMs = 1000L,
            endedAtMs = 1000L + (points.size * 50),
            baseWidthPx = 5.0f
        )
    }
}
