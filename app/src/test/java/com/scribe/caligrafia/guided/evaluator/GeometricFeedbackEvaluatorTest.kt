package com.scribe.caligrafia.guided.evaluator

import com.scribe.caligrafia.core.model.GuidelineBand
import com.scribe.caligrafia.core.model.SlantConfig
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometricFeedbackEvaluatorTest {

    private val band = GuidelineBand(
        bandIndex = 0,
        ascenderY = 100f,
        xHeightY = 200f,
        baselineY = 300f,
        descenderY = 400f
    )
    private val originX = 100f
    private val glyphWidthPx = 100f
    private val slantConfig = SlantConfig(angleDegrees = 52.0f)

    @Test
    fun testEmptyStrokesReturnsZeroScore() {
        val result = GeometricFeedbackEvaluator.evaluate(
            userStrokes = emptyList(),
            reference = ReferenceGlyphCatalog.BASIC_SLANT,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = slantConfig
        )
        assertEquals(0, result.scorePercent)
        assertFalse(result.isPassed)
    }

    @Test
    fun testPerfectStrokeEvaluationScoresHigh() {
        val ref = ReferenceGlyphCatalog.BASIC_SLANT
        val refPoints = ref.strokes.first().points

        // Cria traço simulado idêntico à referência mapeada na tela
        val userPoints = refPoints.mapIndexed { idx, pt ->
            val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, originX, glyphWidthPx)
            StrokePoint(
                x = screenPt.x,
                y = screenPt.y,
                tMs = idx * 16L,
                pressure = 0.5f
            )
        }

        val stroke = Stroke(
            id = "perfect_slant",
            tool = ToolType.STYLUS,
            points = userPoints,
            startedAtMs = 0L,
            endedAtMs = userPoints.size * 16L
        )

        val result = GeometricFeedbackEvaluator.evaluate(
            userStrokes = listOf(stroke),
            reference = ref,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = slantConfig
        )

        assertTrue("Pontuação deve ser alta (>= 80%): ${result.scorePercent}", result.scorePercent >= 80)
        assertTrue("Direção deve ser considerada correta", result.direction.isDirectionCorrect)
        assertTrue("Deve respeitar as linhas-guia", result.guideline.isWithinBounds)
        assertTrue("Deve passar no teste", result.isPassed)
    }

    @Test
    fun testOvershootPenalizesGuidelines() {
        val ref = ReferenceGlyphCatalog.BASIC_SLANT

        // Traço que ultrapassa muito a pauta (de y=50f até y=450f, onde xHeight vai de 200f a 300f)
        val badPoints = listOf(
            StrokePoint(x = 160f, y = 50f, tMs = 0L),    // Ultrapassa topo em 150px
            StrokePoint(x = 135f, y = 450f, tMs = 100L)  // Ultrapassa base em 150px
        )

        val stroke = Stroke(
            id = "bad_overshoot",
            tool = ToolType.STYLUS,
            points = badPoints,
            startedAtMs = 0L,
            endedAtMs = 100L
        )

        val result = GeometricFeedbackEvaluator.evaluate(
            userStrokes = listOf(stroke),
            reference = ref,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = slantConfig
        )

        assertFalse("Não deve estar dentro dos limites", result.guideline.isWithinBounds)
        assertTrue("Deve registrar vazamento no topo", result.guideline.overshootTopPx > 0f)
        assertTrue("Deve registrar vazamento na base", result.guideline.overshootBottomPx > 0f)
        assertTrue("Nota de diretriz deve ser baixa", result.guideline.scorePercent < 60)
    }

    @Test
    fun testWrongDirectionDetected() {
        val ref = ReferenceGlyphCatalog.BASIC_SLANT
        val refPoints = ref.strokes.first().points

        // Desenha de trás para frente (de baixo para cima)
        val userPointsReversed = refPoints.reversed().mapIndexed { idx, pt ->
            val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, originX, glyphWidthPx)
            StrokePoint(
                x = screenPt.x,
                y = screenPt.y,
                tMs = idx * 16L,
                pressure = 0.5f
            )
        }

        val stroke = Stroke(
            id = "reversed_stroke",
            tool = ToolType.STYLUS,
            points = userPointsReversed,
            startedAtMs = 0L,
            endedAtMs = userPointsReversed.size * 16L
        )

        val result = GeometricFeedbackEvaluator.evaluate(
            userStrokes = listOf(stroke),
            reference = ref,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = slantConfig
        )

        assertFalse("Direção deve ser reprovada quando feito ao contrário", result.direction.isDirectionCorrect)
    }
}
