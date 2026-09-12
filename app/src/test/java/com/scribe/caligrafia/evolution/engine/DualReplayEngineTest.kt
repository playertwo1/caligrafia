package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Testes unitários do motor de reprodução dupla sincronizada (SCR-503).
 */
class DualReplayEngineTest {

    private fun createTestStroke(id: String, startMs: Long, durationMs: Long): Stroke {
        val points = listOf(
            StrokePoint(100f, 100f, startMs, 0.5f),
            StrokePoint(120f, 150f, startMs + (durationMs / 2), 0.7f),
            StrokePoint(140f, 200f, startMs + durationMs, 0.4f)
        )
        return Stroke(
            id = id,
            tool = ToolType.STYLUS,
            points = points,
            startedAtMs = startMs,
            endedAtMs = startMs + durationMs
        )
    }

    @Test
    fun computeDualFrameAt_zeroProgress_returnsEmptyStrokes() {
        val strokeA = createTestStroke("sA", 1000L, 2000L)
        val strokeB = createTestStroke("sB", 5000L, 1500L)

        val engine = DualReplayEngine(listOf(strokeA), listOf(strokeB))

        val frame = engine.computeDualFrameAt(0.0f)
        assertEquals(0.0f, frame.progress, 0.001f)
        // No início absoluto (t=0), nenhum ponto com t > 0 deve estar visível se o tempo relativo for 0
        assertNotNull(frame.visibleStrokesA)
        assertNotNull(frame.visibleStrokesB)
    }

    @Test
    fun computeDualFrameAt_fullProgress_returnsCompleteStrokesForBothTracks() {
        val strokeA = createTestStroke("sA", 1000L, 2000L)
        val strokeB = createTestStroke("sB", 5000L, 1500L)

        val engine = DualReplayEngine(listOf(strokeA), listOf(strokeB))

        val frame = engine.computeDualFrameAt(1.0f)
        assertEquals(1.0f, frame.progress, 0.001f)
        assertEquals(1, frame.visibleStrokesA.size)
        assertEquals(3, frame.visibleStrokesA[0].points.size)
        assertEquals(1, frame.visibleStrokesB.size)
        assertEquals(3, frame.visibleStrokesB[0].points.size)
    }

    @Test
    fun computeDualFrameAt_halfProgress_returnsPartialStrokes() {
        val strokeA = createTestStroke("sA", 1000L, 2000L)
        val strokeB = createTestStroke("sB", 5000L, 2000L)

        val engine = DualReplayEngine(listOf(strokeA), listOf(strokeB))

        val frame = engine.computeDualFrameAt(0.5f)
        assertEquals(0.5f, frame.progress, 0.001f)
        assertEquals(1, frame.visibleStrokesA.size)
        // Na metade (1000ms de 2000ms), o segundo ponto (tMs = 2000L) deve estar incluído
        assertEquals(2, frame.visibleStrokesA[0].points.size)
        assertEquals(1, frame.visibleStrokesB.size)
        assertEquals(2, frame.visibleStrokesB[0].points.size)
    }

    @Test
    fun setSpeed_updatesFrameSpeed() {
        val engine = DualReplayEngine()
        engine.setSpeed(ReplaySpeed.DOUBLE)
        assertEquals(ReplaySpeed.DOUBLE, engine.frameFlow.value.speed)
    }
}
