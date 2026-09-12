package com.scribe.caligrafia.ink.capture

import com.scribe.caligrafia.core.model.CalligraphyColor
import com.scribe.caligrafia.core.model.EraserMode
import com.scribe.caligrafia.core.model.PenThickness
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoRedoStackTest {

    private lateinit var repository: InMemoryStrokeRepository

    @Before
    fun setUp() {
        repository = InMemoryStrokeRepository()
    }

    private fun createStroke(id: String, x: Float, y: Float): Stroke {
        return Stroke(
            id = id,
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(x, y, 1000L, 0.5f),
                StrokePoint(x + 10f, y + 10f, 1016L, 0.6f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1016L,
            color = CalligraphyColor.ROYAL_BLUE.argb,
            baseWidthPx = PenThickness.MEDIUM.widthPx
        )
    }

    @Test
    fun initialState_hasNoUndoOrRedo() {
        assertTrue(repository.isEmpty)
        assertFalse(repository.canUndo)
        assertFalse(repository.canRedo)
        assertFalse(repository.undo())
        assertFalse(repository.redo())
    }

    @Test
    fun addStroke_enablesUndo_and_disablesRedo() {
        val s1 = createStroke("s1", 10f, 10f)
        repository.addStroke(s1)

        assertEquals(1, repository.count)
        assertTrue(repository.canUndo)
        assertFalse(repository.canRedo)
    }

    @Test
    fun undo_and_redo_singleStroke() {
        val s1 = createStroke("s1", 10f, 10f)
        repository.addStroke(s1)

        // Undo
        val undoSuccess = repository.undo()
        assertTrue(undoSuccess)
        assertEquals(0, repository.count)
        assertFalse(repository.canUndo)
        assertTrue(repository.canRedo)

        // Redo
        val redoSuccess = repository.redo()
        assertTrue(redoSuccess)
        assertEquals(1, repository.count)
        assertEquals("s1", repository.allStrokes[0].id)
        assertTrue(repository.canUndo)
        assertFalse(repository.canRedo)
    }

    @Test
    fun multiStep_undo_and_redo_preservesOrder() {
        val s1 = createStroke("s1", 10f, 10f)
        val s2 = createStroke("s2", 20f, 20f)
        val s3 = createStroke("s3", 30f, 30f)

        repository.addStroke(s1)
        repository.addStroke(s2)
        repository.addStroke(s3)
        assertEquals(3, repository.count)

        // Undo twice
        repository.undo() // undo s3
        assertEquals(2, repository.count)
        assertEquals(listOf("s1", "s2"), repository.allStrokes.map { it.id })

        repository.undo() // undo s2
        assertEquals(1, repository.count)
        assertEquals(listOf("s1"), repository.allStrokes.map { it.id })

        // Redo once
        repository.redo() // redo s2
        assertEquals(2, repository.count)
        assertEquals(listOf("s1", "s2"), repository.allStrokes.map { it.id })

        // Redo once more
        repository.redo() // redo s3
        assertEquals(3, repository.count)
        assertEquals(listOf("s1", "s2", "s3"), repository.allStrokes.map { it.id })
    }

    @Test
    fun newStrokeAfterUndo_clearsRedoStack() {
        val s1 = createStroke("s1", 10f, 10f)
        val s2 = createStroke("s2", 20f, 20f)
        repository.addStroke(s1)
        repository.addStroke(s2)

        repository.undo() // undo s2
        assertTrue(repository.canRedo)

        // Add brand new stroke
        val s3 = createStroke("s3", 30f, 30f)
        repository.addStroke(s3)

        assertFalse("Novo traço deve descartar pilha de refazer (redo)", repository.canRedo)
        assertEquals(2, repository.count)
        assertEquals(listOf("s1", "s3"), repository.allStrokes.map { it.id })
    }

    @Test
    fun eraserAction_canBeUndone_andRedone() {
        val s1 = createStroke("s1", 10f, 10f)
        val s2 = createStroke("s2", 50f, 50f)
        repository.addStroke(s1)
        repository.addStroke(s2)

        // Erase s1
        val eraserPoints = listOf(StrokePoint(12f, 12f, 2000L))
        val erased = repository.eraseStrokesIntersecting(eraserPoints, eraserRadius = 10f)
        assertEquals(1, erased.size)
        assertEquals("s1", erased[0].id)
        assertEquals(1, repository.count)
        assertEquals("s2", repository.allStrokes[0].id)

        // Undo erase -> restores s1
        assertTrue(repository.canUndo)
        repository.undo()
        assertEquals(2, repository.count)
        assertEquals("s1", repository.allStrokes[0].id)
        assertEquals("s2", repository.allStrokes[1].id)

        // Redo erase -> re-erases s1
        assertTrue(repository.canRedo)
        repository.redo()
        assertEquals(1, repository.count)
        assertEquals("s2", repository.allStrokes[0].id)
    }

    @Test
    fun loadStrokes_resetsUndoAndRedo() {
        val s1 = createStroke("s1", 10f, 10f)
        repository.addStroke(s1)
        repository.undo()
        assertTrue(repository.canRedo)

        val pageStrokes = listOf(createStroke("page1", 100f, 100f))
        repository.loadStrokes(pageStrokes)

        assertEquals(1, repository.count)
        assertEquals("page1", repository.allStrokes[0].id)
        assertFalse("loadStrokes deve zerar redo", repository.canRedo)
    }

    @Test
    fun toolConfig_defaultsAndPresets() {
        val config = ToolConfig()
        assertEquals(ToolMode.PEN, config.mode)
        assertEquals(PenThickness.MEDIUM, config.penThickness)
        assertEquals(5.0f, config.activeStrokeWidthPx, 0.001f)
        assertEquals(CalligraphyColor.NANQUIM, config.color)
        assertEquals(EraserMode.STROKE, config.eraserMode)

        val custom = config.copy(
            mode = ToolMode.ERASER,
            penThickness = PenThickness.FINE,
            color = CalligraphyColor.SEPIA
        )
        assertEquals(ToolMode.ERASER, custom.mode)
        assertEquals(2.5f, custom.activeStrokeWidthPx, 0.001f)
        assertEquals(CalligraphyColor.SEPIA.argb, custom.activeColorArgb)
    }
}
