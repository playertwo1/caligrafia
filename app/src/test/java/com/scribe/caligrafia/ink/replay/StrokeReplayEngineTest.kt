package com.scribe.caligrafia.ink.replay

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StrokeReplayEngineTest {

    private lateinit var strokes: List<Stroke>
    private lateinit var engine: StrokeReplayEngine

    @Before
    fun setUp() {
        // Traço 1: de t = 1000ms a 1200ms (3 pontos: 1000, 1100, 1200)
        val stroke1 = Stroke(
            id = "stroke_1",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(10f, 10f, 1000L, pressure = 0.3f),
                StrokePoint(20f, 20f, 1100L, pressure = 0.5f),
                StrokePoint(30f, 30f, 1200L, pressure = 0.7f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1200L
        )

        // Traço 2: de t = 1400ms a 1600ms (pausa de 200ms entre traços, 3 pontos: 1400, 1500, 1600)
        val stroke2 = Stroke(
            id = "stroke_2",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(50f, 50f, 1400L, pressure = 0.4f),
                StrokePoint(60f, 60f, 1500L, pressure = 0.6f),
                StrokePoint(70f, 70f, 1600L, pressure = 0.8f)
            ),
            startedAtMs = 1400L,
            endedAtMs = 1600L
        )

        strokes = listOf(stroke1, stroke2)
        engine = StrokeReplayEngine(strokes)
    }

    @Test
    fun testTotalDurationComputation() {
        val initialFrame = engine.frameFlow.value
        // Duração total: 1600 - 1000 = 600ms
        assertEquals(600L, initialFrame.totalDurationMs)
        assertEquals(0L, initialFrame.currentPositionMs)
        assertEquals(0f, initialFrame.progressFraction, 0.001f)
    }

    @Test
    fun testFrameAtStart() {
        val frame = engine.computeFrameAt(0L)
        assertEquals(0, frame.completedStrokes.size)
        assertNotNull(frame.activeStroke)
        assertEquals(1, frame.activeStroke!!.points.size)
        assertEquals(10f, frame.activeStroke!!.points[0].x, 0.001f)
    }

    @Test
    fun testFrameMidwayFirstStroke() {
        // t = 100ms (tempo absoluto 1100ms) -> deve conter os 2 primeiros pontos do traço 1
        val frame = engine.computeFrameAt(100L)
        assertEquals(0, frame.completedStrokes.size)
        assertNotNull(frame.activeStroke)
        assertEquals(2, frame.activeStroke!!.points.size)
        assertEquals("stroke_1", frame.activeStroke!!.id)
    }

    @Test
    fun testFrameDuringPauseBetweenStrokes() {
        // t = 300ms (tempo absoluto 1300ms) -> Traço 1 terminou (1200ms), Traço 2 ainda não começou (1400ms)
        val frame = engine.computeFrameAt(300L)
        assertEquals(1, frame.completedStrokes.size)
        assertEquals("stroke_1", frame.completedStrokes[0].id)
        assertNull("Em pausa entre traços, nenhum traço deve estar ativo", frame.activeStroke)
    }

    @Test
    fun testFrameMidwaySecondStroke() {
        // t = 500ms (tempo absoluto 1500ms) -> Traço 1 completo, Traço 2 com 2 pontos
        val frame = engine.computeFrameAt(500L)
        assertEquals(1, frame.completedStrokes.size)
        assertEquals("stroke_1", frame.completedStrokes[0].id)
        assertNotNull(frame.activeStroke)
        assertEquals("stroke_2", frame.activeStroke!!.id)
        assertEquals(2, frame.activeStroke!!.points.size)
    }

    @Test
    fun testFrameAtEnd() {
        // t = 600ms -> Ambos os traços completos
        val frame = engine.computeFrameAt(600L)
        assertEquals(2, frame.completedStrokes.size)
        assertNull(frame.activeStroke)
        assertEquals(1.0f, frame.progressFraction, 0.001f)
    }

    @Test
    fun testSeekToFraction() {
        engine.seekTo(0.5f) // 50% de 600ms = 300ms
        val frame = engine.frameFlow.value
        assertEquals(300L, frame.currentPositionMs)
        assertEquals(0.5f, frame.progressFraction, 0.001f)
        assertEquals(1, frame.completedStrokes.size)
    }

    @Test
    fun testSpeedConfiguration() {
        engine.setSpeed(ReplaySpeed.HALF)
        assertEquals(ReplaySpeed.HALF, engine.frameFlow.value.speed)
        assertEquals(0.5f, engine.frameFlow.value.speed.multiplier, 0.001f)

        engine.setSpeed(ReplaySpeed.DOUBLE)
        assertEquals(ReplaySpeed.DOUBLE, engine.frameFlow.value.speed)
        assertEquals(2.0f, engine.frameFlow.value.speed.multiplier, 0.001f)
    }

    @Test
    fun testEmptyStrokesHandling() {
        val emptyEngine = StrokeReplayEngine(emptyList())
        val frame = emptyEngine.frameFlow.value
        assertEquals(0L, frame.totalDurationMs)
        assertEquals(0L, frame.currentPositionMs)
        assertEquals(0, frame.completedStrokes.size)
        assertNull(frame.activeStroke)
    }
}
