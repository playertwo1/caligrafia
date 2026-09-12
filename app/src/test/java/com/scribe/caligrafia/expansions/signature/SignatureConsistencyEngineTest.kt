package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SignatureConsistencyEngineTest {

    private fun createStroke(
        points: List<Pair<Float, Float>>,
        startMs: Long,
        endMs: Long
    ): Stroke {
        val strokePoints = points.mapIndexed { i, (x, y) ->
            val t = startMs + ((endMs - startMs) * i / points.size.coerceAtLeast(1))
            StrokePoint(x = x, y = y, tMs = t, pressure = 0.5f, tiltRad = null, orientationRad = null)
        }
        return Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = strokePoints,
            startedAtMs = startMs,
            endedAtMs = endMs,
            isCancelled = false,
            color = 0xFF000000.toInt(),
            baseWidthPx = 4.0f
        )
    }

    @Test
    fun computeMetrics_emptyStrokes_returnsSafeDefaults() {
        val metrics = SignatureConsistencyEngine.computeMetrics(emptyList())
        assertEquals(0, metrics.strokeCount)
        assertEquals(0L, metrics.durationMs)
        assertEquals(0f, metrics.totalLengthPx, 0.001f)
        assertEquals(1f, metrics.aspectRatio, 0.001f)
    }

    @Test
    fun computeMetrics_validStrokes_computesAccurateMeasurements() {
        val stroke1 = createStroke(
            listOf(0f to 0f, 100f to 0f),
            startMs = 1000L,
            endMs = 1500L
        )
        val stroke2 = createStroke(
            listOf(100f to 0f, 100f to 50f),
            startMs = 1600L,
            endMs = 2000L
        )

        val metrics = SignatureConsistencyEngine.computeMetrics(listOf(stroke1, stroke2))
        assertEquals(2, metrics.strokeCount)
        assertEquals(1000L, metrics.durationMs) // 2000 - 1000
        assertEquals(150f, metrics.totalLengthPx, 0.1f) // 100 + 50
        assertEquals(2.0f, metrics.aspectRatio, 0.1f) // w=100, h=50 -> 2.0
        assertEquals(1, metrics.penUpCount)
    }

    @Test
    fun evaluateConsistency_identicalSignatures_yieldsNearPerfectScore() {
        val metrics = SignatureMetrics(
            strokeCount = 3,
            durationMs = 2500L,
            averageSpeedPxPerMs = 0.45f,
            totalLengthPx = 1125f,
            aspectRatio = 2.4f,
            penUpCount = 2
        )

        val report = SignatureConsistencyEngine.evaluateConsistency(metrics, metrics)
        assertTrue("Assinaturas idênticas devem ser consistentes", report.isConsistent)
        assertEquals(100f, report.repeatabilityScore, 0.01f)
        assertTrue(report.strokeCountMatch)
        assertEquals(0f, report.durationVariationPercent, 0.01f)
    }

    @Test
    fun evaluateConsistency_divergentStrokesAndSpeed_penalizesScore() {
        val baseline = SignatureMetrics(
            strokeCount = 2,
            durationMs = 2000L,
            averageSpeedPxPerMs = 0.5f,
            totalLengthPx = 1000f,
            aspectRatio = 2.5f,
            penUpCount = 1
        )
        val rushedAttempt = SignatureMetrics(
            strokeCount = 5, // 3 traços extras
            durationMs = 800L, // Muito rápida
            averageSpeedPxPerMs = 1.8f,
            totalLengthPx = 1440f,
            aspectRatio = 4.0f,
            penUpCount = 4
        )

        val report = SignatureConsistencyEngine.evaluateConsistency(baseline, rushedAttempt)
        assertFalse(report.isConsistent)
        assertTrue(report.repeatabilityScore < 60f)
        assertFalse(report.strokeCountMatch)
    }
}
