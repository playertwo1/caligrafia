package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class MotorDiagnosticEngineTest {

    private val engine = MotorDiagnosticEngine(defaultTargetSlantDegrees = 52.0f)

    private fun createStroke(id: String, points: List<StrokePoint>): Stroke {
        val start = points.firstOrNull()?.tMs ?: 1000L
        val end = points.lastOrNull()?.tMs ?: (start + 100L)
        return Stroke(
            id = id,
            tool = ToolType.STYLUS,
            points = points,
            startedAtMs = start,
            endedAtMs = end,
            baseWidthPx = 5.0f
        )
    }

    @Test
    fun `diagnoseAttempts with empty list returns valid fallback diagnostic`() {
        val diag = engine.diagnoseAttempts(emptyList())

        assertNotNull(diag)
        assertEquals(70.0f, diag.overallScore, 0.1f)
        assertEquals(4, diag.dimensions.size)
        assertTrue(diag.dimensions.containsKey(BiomechanicalDimension.SLANT_STABILITY))
        assertTrue(diag.dimensions.containsKey(BiomechanicalDimension.GUIDELINE_CONTAINMENT))
        assertTrue(diag.dimensions.containsKey(BiomechanicalDimension.RHYTHM_AND_CADENCE))
        assertTrue(diag.dimensions.containsKey(BiomechanicalDimension.PRESSURE_CONTROL))
    }

    @Test
    fun `evaluateSlantStability gives high score for consistent 52 degree strokes`() {
        // Gera traços descendentes estritamente a 52 graus
        val rad52 = Math.toRadians(52.0)
        val strokes = (0 until 5).map { sIdx ->
            val pts = (0 until 10).map { pIdx ->
                val dist = pIdx * 10.0
                StrokePoint(
                    x = (100.0 + sIdx * 50.0 + dist * cos(rad52)).toFloat(),
                    y = (100.0 + dist * sin(rad52)).toFloat(),
                    tMs = pIdx * 16L,
                    pressure = 0.5f,
                    tiltRad = null,
                    orientationRad = null
                )
            }
            createStroke("s_$sIdx", pts)
        }

        val diag = engine.diagnoseStrokes(strokes, targetSlantDegrees = 52.0f)
        val slantEval = diag.dimensions[BiomechanicalDimension.SLANT_STABILITY]

        assertNotNull(slantEval)
        assertTrue("Score deve ser alto para inclinação perfeita: ${slantEval?.score}", (slantEval?.score ?: 0f) >= 85f)
        assertEquals(EvaluationStatus.EXCELLENT, slantEval?.status)
    }

    @Test
    fun `evaluateRhythmAndCadence penalizes hesitations and stalls`() {
        // Traço com várias pausas longas e movimento quase nulo (hesitação real da caneta parada)
        val pts = mutableListOf<StrokePoint>()
        var time = 0L
        for (i in 0 until 10) {
            pts.add(StrokePoint(x = 10f + (i % 2) * 1.0f, y = 10f + (i % 2) * 1.0f, tMs = time, pressure = 0.5f, tiltRad = null, orientationRad = null))
            time += 250L // hesitação deliberada
        }
        val strokes = listOf(createStroke("hesitant_stroke", pts))

        val diag = engine.diagnoseStrokes(strokes)
        val rhythmEval = diag.dimensions[BiomechanicalDimension.RHYTHM_AND_CADENCE]

        assertNotNull(rhythmEval)
        assertTrue("Score de ritmo deve sofrer penalidade por hesitação: ${rhythmEval?.score}", (rhythmEval?.score ?: 100f) < 80f)
    }

    @Test
    fun `evaluatePressureControl rewards clear downstroke contrast`() {
        // Traço com descida pesada e subida leve
        val downPts = (0 until 5).map { i ->
            StrokePoint(x = 100f + i * 5f, y = 100f + i * 10f, tMs = i * 20L, pressure = 0.8f, tiltRad = null, orientationRad = null)
        }
        val upPts = (0 until 5).map { i ->
            StrokePoint(x = 125f + i * 5f, y = 150f - i * 10f, tMs = 100L + i * 20L, pressure = 0.25f, tiltRad = null, orientationRad = null)
        }
        val stroke = createStroke("modulated_stroke", downPts + upPts)

        val diag = engine.diagnoseStrokes(listOf(stroke))
        val pressureEval = diag.dimensions[BiomechanicalDimension.PRESSURE_CONTROL]

        assertNotNull(pressureEval)
        assertTrue("Score de pressão deve reconhecer bom contraste: ${pressureEval?.score}", (pressureEval?.score ?: 0f) >= 80f)
    }

    @Test
    fun `diagnoseAttempts identifies primary weakness and strength correctly`() {
        val attemptStroke = createStroke(
            "stroke_att",
            listOf(
                StrokePoint(100f, 100f, 1000L, 0.5f, null, null),
                StrokePoint(120f, 150f, 1050L, 0.5f, null, null),
                StrokePoint(140f, 200f, 1100L, 0.5f, null, null)
            )
        )
        val attempt = PracticeAttemptRecord(
            attemptId = "att_1",
            targetId = "t",
            targetTitle = "Letra t",
            timestampMs = System.currentTimeMillis(),
            strokes = listOf(attemptStroke),
            scorePercent = 95,
            averageSlantDegrees = 52.0f,
            durationMs = 2500L
        )

        val diag = engine.diagnoseAttempts(listOf(attempt))
        assertNotNull(diag.primaryStrength)
        assertNotNull(diag.primaryWeakness)
        assertTrue(diag.overallScore in 0f..100f)
    }
}
