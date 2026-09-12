package com.scribe.caligrafia.ink.lifecycle

import android.content.Intent
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class StylusLifecycleTest {

    private lateinit var tempDir: File
    private lateinit var lifecycleManager: SessionLifecycleManager
    private lateinit var pipeline: StrokeCapturePipeline

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scribe_lifecycle_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        lifecycleManager = SessionLifecycleManager(tempDir)
        pipeline = StrokeCapturePipeline()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun pipeline_flushes_and_commits_active_stroke_when_points_are_valid() {
        var completedStroke: Stroke? = null
        var cancelledStroke: Stroke? = null
        pipeline.onStrokeCompleted = { completedStroke = it }
        pipeline.onStrokeCancelled = { cancelledStroke = it }

        // Simula início de traço
        val p1 = StrokePoint(10f, 10f, 1000L, 0.5f, null, null)
        val p2 = StrokePoint(20f, 20f, 1020L, 0.6f, null, null)
        val p3 = StrokePoint(30f, 30f, 1040L, 0.7f, null, null)

        pipeline.onPointerDown(pointerId = 1, toolType = ToolType.STYLUS, point = p1, eventTime = 1000L)
        pipeline.onPointerMove(pointerId = 1, points = listOf(p2, p3))

        assertTrue("Pipeline deve estar capturando", pipeline.isCapturing)

        // Ocorre interrupção de ciclo de vida (ex: perda de foco de janela ou onPause)
        val flushed = pipeline.flushActiveStroke(commitIfValid = true)

        assertTrue("Flush deve reportar sucesso", flushed)
        assertFalse("Pipeline não deve mais estar capturando", pipeline.isCapturing)
        assertNotNull("Traço deve ter sido finalizado e comitado", completedStroke)
        assertNull("Nenhum traço deve ter sido cancelado", cancelledStroke)
        assertEquals(3, completedStroke!!.points.size)
        assertFalse(completedStroke!!.isCancelled)
        assertEquals(1040L, completedStroke!!.endedAtMs)
    }

    @Test
    fun pipeline_cancels_active_stroke_cleanly_when_points_are_fewer_than_two() {
        var completedStroke: Stroke? = null
        var cancelledStroke: Stroke? = null
        pipeline.onStrokeCompleted = { completedStroke = it }
        pipeline.onStrokeCancelled = { cancelledStroke = it }

        // Apenas 1 ponto coletado (ex: toque acidental interrompido imediatamente)
        val p1 = StrokePoint(10f, 10f, 1000L, 0.5f, null, null)
        pipeline.onPointerDown(pointerId = 1, toolType = ToolType.STYLUS, point = p1, eventTime = 1000L)

        val flushed = pipeline.flushActiveStroke(commitIfValid = true)

        assertTrue("Flush deve reportar sucesso", flushed)
        assertFalse("Pipeline não deve mais estar capturando", pipeline.isCapturing)
        assertNull("Traço com 1 ponto não deve ser comitado", completedStroke)
        assertNotNull("Traço deve ser cancelado com segurança", cancelledStroke)
        assertTrue(cancelledStroke!!.isCancelled)
    }

    @Test
    fun session_lifecycle_manager_auto_saves_and_restores_exact_vector_strokes() {
        val originalStrokes = listOf(
            Stroke(
                id = "stroke-alpha",
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(100f, 200f, 1000L, 0.45f, 0.12f, 0.55f),
                    StrokePoint(110f, 210f, 1016L, 0.75f, 0.14f, 0.58f),
                    StrokePoint(125f, 225f, 1032L, 0.85f, 0.15f, 0.60f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1032L
            ),
            Stroke(
                id = "stroke-beta",
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(300f, 400f, 2000L, 0.50f, null, null),
                    StrokePoint(310f, 410f, 2016L, 0.60f, null, null)
                ),
                startedAtMs = 2000L,
                endedAtMs = 2016L
            )
        )

        assertFalse("Não deve existir auto-save antes de gravar", lifecycleManager.hasAutoSave())

        val bytesSaved = lifecycleManager.autoSave(originalStrokes)
        assertTrue("Bytes gravados devem ser > 0", bytesSaved > 0)
        assertTrue("Auto-save deve existir em disco", lifecycleManager.hasAutoSave())

        // Simula restauração após encerramento do processo
        val restoredStrokes = lifecycleManager.restore()
        assertEquals(2, restoredStrokes.size)

        val s1 = restoredStrokes[0]
        assertEquals("stroke-alpha", s1.id)
        assertEquals(ToolType.STYLUS, s1.tool)
        assertEquals(3, s1.points.size)
        assertEquals(100f, s1.points[0].x, 0.001f)
        assertEquals(200f, s1.points[0].y, 0.001f)
        assertEquals(0.45f, s1.points[0].pressure!!, 0.001f)
        assertEquals(0.12f, s1.points[0].tiltRad!!, 0.001f)
        assertEquals(0.55f, s1.points[0].orientationRad!!, 0.001f)

        val s2 = restoredStrokes[1]
        assertEquals("stroke-beta", s2.id)
        assertEquals(2, s2.points.size)
        assertNull(s2.points[0].tiltRad)
    }

    @Test
    fun session_lifecycle_manager_clear_auto_save_removes_file() {
        val strokes = listOf(
            Stroke(
                id = "temp-stroke",
                tool = ToolType.STYLUS,
                points = listOf(StrokePoint(10f, 10f, 1000L, 0.5f, null, null)),
                startedAtMs = 1000L,
                endedAtMs = 1000L
            )
        )

        lifecycleManager.autoSave(strokes)
        assertTrue(lifecycleManager.hasAutoSave())

        val cleared = lifecycleManager.clearAutoSave()
        assertTrue("Clear deve retornar true", cleared)
        assertFalse("Arquivo de auto-save não deve mais existir", lifecycleManager.hasAutoSave())
    }

    @Test
    fun spen_insertion_detector_processes_samsung_broadcast_intents() {
        val detector = SPenInsertionDetector()
        assertEquals(SPenSlotState.UNKNOWN, detector.state.value)

        var insertedFired = false
        var detachedFired = false
        detector.onPenInserted = { insertedFired = true }
        detector.onPenDetached = { detachedFired = true }

        // Simula evento Samsung de remoção da S Pen (penInsert = false)
        val handledDetach = detector.handleRawInsertionEvent(
            action = SPenInsertionDetector.ACTION_PEN_INSERTION,
            isInserted = false
        )
        assertTrue("Evento de remoção deve ser reconhecido", handledDetach)
        assertEquals(SPenSlotState.DETACHED, detector.state.value)
        assertTrue("Callback de remoção deve ter sido disparado", detachedFired)

        // Simula evento Samsung de inserção da S Pen no silo (penInsert = true)
        val handledInsert = detector.handleRawInsertionEvent(
            action = SPenInsertionDetector.ACTION_PEN_INSERTION,
            isInserted = true
        )
        assertTrue("Evento de inserção deve ser reconhecido", handledInsert)
        assertEquals(SPenSlotState.INSERTED, detector.state.value)
        assertTrue("Callback de inserção deve ter sido disparado", insertedFired)
    }

    @Test
    fun spen_insertion_detector_ignores_foreign_or_null_intents() {
        val detector = SPenInsertionDetector()

        assertFalse("Intent nulo deve retornar false", detector.handleIntent(null))
        assertFalse(
            "Action estrangeira deve ser ignorada",
            detector.handleRawInsertionEvent("android.intent.action.BATTERY_CHANGED", false)
        )
        assertEquals(SPenSlotState.UNKNOWN, detector.state.value)
    }

    @Test
    fun spen_insertion_triggers_active_stroke_flush_in_pipeline() {
        val detector = SPenInsertionDetector()
        var completedStroke: Stroke? = null
        pipeline.onStrokeCompleted = { completedStroke = it }

        detector.onPenInserted = {
            pipeline.flushActiveStroke(commitIfValid = true)
        }

        // Usuário está escrevendo
        val p1 = StrokePoint(50f, 50f, 1000L, 0.5f, null, null)
        val p2 = StrokePoint(60f, 60f, 1016L, 0.6f, null, null)
        pipeline.onPointerDown(pointerId = 1, toolType = ToolType.STYLUS, point = p1, eventTime = 1000L)
        pipeline.onPointerMove(pointerId = 1, points = listOf(p2))

        assertTrue(pipeline.isCapturing)

        // S Pen é guardada no silo físico
        detector.handleRawInsertionEvent(
            action = SPenInsertionDetector.ACTION_PEN_INSERTION,
            isInserted = true
        )

        assertFalse("Pipeline deve ter sido descarregado", pipeline.isCapturing)
        assertNotNull("Traço deve ter sido salvo antes da caneta ser guardada", completedStroke)
        assertEquals(2, completedStroke!!.points.size)
    }
}
