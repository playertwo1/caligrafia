package com.scribe.caligrafia.ink.capture

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StrokeCapturePipelineTest {

    private lateinit var pipeline: StrokeCapturePipeline
    private lateinit var repository: InMemoryStrokeRepository

    @Before
    fun setup() {
        repository = InMemoryStrokeRepository()
        pipeline = StrokeCapturePipeline(
            onStrokeCompleted = { stroke -> repository.addStroke(stroke) }
        )
    }

    @Test
    fun testStrokeCaptureLifecycle() {
        val baseTime = 1000L
        val pointerId = 1

        // 1. DOWN
        val p1 = StrokePoint(100f, 200f, baseTime, pressure = 0.5f)
        val downConsumed = pipeline.onPointerDown(pointerId, ToolType.STYLUS, p1, baseTime)
        assertTrue(downConsumed)
        assertTrue(pipeline.isCapturing)
        assertNotNull(pipeline.getActiveStrokePreview())
        assertEquals(1, pipeline.getActiveStrokePreview()?.pointCount)

        // 2. MOVE (com 2 historical samples + 1 ponto atual)
        val h1 = StrokePoint(105f, 202f, baseTime + 5, pressure = 0.55f)
        val h2 = StrokePoint(110f, 205f, baseTime + 10, pressure = 0.60f)
        val cur = StrokePoint(120f, 210f, baseTime + 16, pressure = 0.65f)

        val moveConsumed = pipeline.onPointerMove(pointerId, listOf(h1, h2, cur), historicalCount = 2)
        assertTrue(moveConsumed)
        assertEquals(4, pipeline.getActiveStrokePreview()?.pointCount)
        assertEquals(2, pipeline.totalHistoricalSamplesAbsorbed)
        assertEquals(4, pipeline.totalPointsCaptured)

        // 3. UP
        val finalTime = baseTime + 32
        val pUp = StrokePoint(150f, 230f, finalTime, pressure = 0.4f)
        val upConsumed = pipeline.onPointerUp(pointerId, listOf(pUp), finalTime)
        assertTrue(upConsumed)
        assertFalse(pipeline.isCapturing)

        // Repository deve conter exatamente 1 stroke com 5 pontos
        assertEquals(1, repository.count)
        val savedStroke = repository.allStrokes.first()
        assertNotNull(savedStroke.id)
        assertEquals(ToolType.STYLUS, savedStroke.tool)
        assertEquals(5, savedStroke.pointCount)
        assertEquals(baseTime, savedStroke.startedAtMs)
        assertEquals(finalTime, savedStroke.endedAtMs)
        assertEquals(32L, savedStroke.durationMs)
        assertFalse(savedStroke.isCancelled)

        // Validação da ordem cronológica estrita dos pontos
        for (i in 0 until savedStroke.points.size - 1) {
            val current = savedStroke.points[i]
            val next = savedStroke.points[i + 1]
            assertTrue("Pontos devem ser cronológicos: ${current.tMs} <= ${next.tMs}", current.tMs <= next.tMs)
        }
    }

    @Test
    fun testActionCancelProducesCancelledStroke() {
        var cancelledStroke: Stroke? = null
        pipeline.onStrokeCancelled = { stroke -> cancelledStroke = stroke }

        val startTime = 2000L
        val p1 = StrokePoint(50f, 50f, startTime)
        pipeline.onPointerDown(1, ToolType.STYLUS, p1, startTime)
        assertTrue(pipeline.isCapturing)

        val cancelTime = startTime + 20
        pipeline.onPointerCancel(1, cancelTime)

        assertFalse(pipeline.isCapturing)
        assertNotNull(cancelledStroke)
        assertTrue(cancelledStroke!!.isCancelled)
        assertEquals(1, cancelledStroke!!.pointCount)
        assertEquals(startTime, cancelledStroke!!.startedAtMs)
        assertEquals(cancelTime, cancelledStroke!!.endedAtMs)
    }

    @Test
    fun testSensorValuesNotInvented() {
        val p = StrokePoint(
            x = 10f,
            y = 10f,
            tMs = 500L,
            pressure = null,
            tiltRad = null,
            orientationRad = null
        )
        pipeline.onPointerDown(0, ToolType.STYLUS, p, 500L)

        val stroke = pipeline.getActiveStrokePreview()
        assertNotNull(stroke)
        val firstPoint = stroke!!.points.first()

        assertNull("Pressão não fornecida deve ser null", firstPoint.pressure)
        assertNull("Tilt não fornecido deve ser null", firstPoint.tiltRad)
        assertNull("Orientação não fornecida deve ser null", firstPoint.orientationRad)
    }

    @Test
    fun testIgnoreSecondaryPointerWhileActive() {
        val p1 = StrokePoint(10f, 10f, 100L)
        pipeline.onPointerDown(0, ToolType.STYLUS, p1, 100L)

        // Outro ponteiro (id = 1) tenta enviar evento MOVE
        val p2 = StrokePoint(20f, 20f, 105L)
        val result = pipeline.onPointerMove(1, listOf(p2), historicalCount = 0)
        assertFalse("Deve ignorar movimento de ponteiro não ativo", result)

        assertEquals(1, pipeline.getActiveStrokePreview()?.pointCount)
    }

    @Test
    fun testInMemoryRepositoryOperations() {
        val repo = InMemoryStrokeRepository()
        assertEquals(0, repo.count)
        assertEquals(0, repo.totalPointsCount)

        val s1 = Stroke(
            id = "s1",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(0f, 0f, 100L),
                StrokePoint(10f, 10f, 110L)
            ),
            startedAtMs = 100L,
            endedAtMs = 110L
        )

        val s2 = Stroke(
            id = "s2",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(20f, 20f, 200L)
            ),
            startedAtMs = 200L,
            endedAtMs = 200L
        )

        repo.addStroke(s1)
        repo.addStroke(s2)

        assertEquals(2, repo.count)
        assertEquals(3, repo.totalPointsCount)

        val removed = repo.removeLastStroke()
        assertEquals(s2.id, removed?.id)
        assertEquals(1, repo.count)

        repo.clear()
        assertEquals(0, repo.count)
        assertEquals(0, repo.totalPointsCount)
    }

    @Test
    fun testFingerRejectedInStylusOnlyMode() {
        var rejectedTool: ToolType? = null
        var rejectedReason: String? = null
        pipeline.onPalmTouchRejected = { tool, reason ->
            rejectedTool = tool
            rejectedReason = reason
        }

        val p = StrokePoint(10f, 10f, 1000L)
        val accepted = pipeline.onPointerDown(1, ToolType.FINGER, p, 1000L)

        assertFalse("Dedo deve ser rejeitado no modo padrão Stylus-Only", accepted)
        assertFalse(pipeline.isCapturing)
        assertEquals(ToolType.FINGER, rejectedTool)
        assertNotNull(rejectedReason)
        assertEquals(1L, pipeline.palmPolicy.rejectedPalmTouchesCount)
    }

    @Test
    fun testStylusPreemptsActiveFingerStrokeInStylusAndTouchMode() {
        pipeline.palmPolicy.inputMode = com.scribe.caligrafia.core.model.InputMode.STYLUS_AND_FINGER
        var cancelledStroke: Stroke? = null
        pipeline.onStrokeCancelled = { stroke -> cancelledStroke = stroke }

        val pFinger = StrokePoint(10f, 10f, 1000L)
        val fingerAccepted = pipeline.onPointerDown(1, ToolType.FINGER, pFinger, 1000L)
        assertTrue(fingerAccepted)
        assertTrue(pipeline.isCapturing)
        assertEquals(ToolType.FINGER, pipeline.currentActiveTool)

        // Stylus toca a tela logo em seguida
        val pStylus = StrokePoint(20f, 20f, 1020L)
        val stylusAccepted = pipeline.onPointerDown(2, ToolType.STYLUS, pStylus, 1020L)
        assertTrue(stylusAccepted)
        assertTrue(pipeline.isCapturing)
        assertEquals(ToolType.STYLUS, pipeline.currentActiveTool)

        // O traço de dedo anterior foi devidamente cancelado
        assertNotNull(cancelledStroke)
        assertEquals(ToolType.FINGER, cancelledStroke!!.tool)
        assertTrue(cancelledStroke!!.isCancelled)
    }

    @Test
    fun testToolTypeFromMotionEventResolvesStylusButtonAsEraser() {
        val resolved = ToolType.fromMotionEvent(
            toolType = android.view.MotionEvent.TOOL_TYPE_STYLUS,
            buttonState = android.view.MotionEvent.BUTTON_STYLUS_PRIMARY
        )
        assertEquals(ToolType.ERASER, resolved)
    }
}

