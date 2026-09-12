package com.scribe.caligrafia.guided.catalog

import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceGlyphCatalogTest {

    @Test
    fun testAllGlyphsHaveValidStrokesAndPoints() {
        val glyphs = ReferenceGlyphCatalog.ALL_GLYPHS
        assertTrue("Catálogo deve possuir ao menos 10 exercícios", glyphs.size >= 10)

        for (glyph in glyphs) {
            assertTrue("ID não pode estar vazio: ${glyph.name}", glyph.id.isNotBlank())
            assertTrue("Símbolo não pode estar vazio: ${glyph.name}", glyph.symbol.isNotBlank())
            assertTrue("Instruções devem existir: ${glyph.name}", glyph.instructions.isNotBlank())
            assertTrue("Glifo deve ter ao menos 1 traço: ${glyph.name}", glyph.strokes.isNotEmpty())

            var orderCheck = 1
            for (stroke in glyph.strokes) {
                assertEquals("Ordem do traço deve ser sequencial", orderCheck++, stroke.orderIndex)
                assertTrue("Traço deve ter pontos interpolados", stroke.points.size >= 2)

                for (pt in stroke.points) {
                    // X deve ser relativo (normalmente entre -0.5 e 1.5)
                    assertTrue("Ponto X em range razoável", pt.xRatio in -0.5f..2.0f)
                    // Y deve ser relativo à baseline (normalmente entre -2.0 e 3.5)
                    assertTrue("Ponto Y em range razoável", pt.yRatio in -2.0f..3.5f)
                }
            }
        }
    }

    @Test
    fun testFindByIdReturnsCorrectGlyph() {
        val slant = ReferenceGlyphCatalog.findById("basic_slant")
        assertNotNull(slant)
        assertEquals("Traço Inclinado Descendente", slant?.name)

        val letterA = ReferenceGlyphCatalog.findById("letter_a")
        assertNotNull(letterA)
        assertEquals("Letra 'a' Cursiva", letterA?.name)
        assertEquals(2, letterA?.strokeCount)
    }

    @Test
    fun testGhostModeTransitions() {
        assertEquals(GhostModeLevel.CLEAR, GhostModeLevel.FULL.nextLowerLevel())
        assertEquals(GhostModeLevel.FAINT, GhostModeLevel.CLEAR.nextLowerLevel())
        assertEquals(GhostModeLevel.WATERMARK, GhostModeLevel.FAINT.nextLowerLevel())
        assertEquals(GhostModeLevel.OFF, GhostModeLevel.WATERMARK.nextLowerLevel())
        assertEquals(GhostModeLevel.OFF, GhostModeLevel.OFF.nextLowerLevel())

        assertTrue(GhostModeLevel.FULL.isVisible)
        assertTrue(GhostModeLevel.WATERMARK.isVisible)
        assertFalse(GhostModeLevel.OFF.isVisible)
    }

    @Test
    fun testPracticeStageTransitions() {
        assertEquals(PracticeStage.COPY, PracticeStage.TRACE.nextStage())
        assertEquals(PracticeStage.SOLO, PracticeStage.COPY.nextStage())
        assertEquals(PracticeStage.SOLO, PracticeStage.SOLO.nextStage())

        assertEquals(PracticeStage.TRACE, PracticeStage.TRACE.previousStage())
        assertEquals(PracticeStage.TRACE, PracticeStage.COPY.previousStage())
        assertEquals(PracticeStage.COPY, PracticeStage.SOLO.previousStage())
    }
}
