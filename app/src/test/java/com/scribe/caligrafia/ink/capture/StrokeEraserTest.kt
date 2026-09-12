package com.scribe.caligrafia.ink.capture

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeEraserTest {

    private fun makeStroke(id: String, points: List<StrokePoint>): Stroke {
        return Stroke(
            id = id,
            tool = ToolType.STYLUS,
            points = points,
            startedAtMs = points.firstOrNull()?.tMs ?: 0L,
            endedAtMs = points.lastOrNull()?.tMs ?: 0L
        )
    }

    @Test
    fun testStrokeEraserHelperIntersectsLineSegment() {
        val stroke = makeStroke(
            id = "stroke-1",
            points = listOf(
                StrokePoint(10f, 10f, 100L),
                StrokePoint(100f, 100f, 120L)
            )
        )

        // Ponto da borracha bem no meio do segmento (55, 55)
        val eraserPoints = listOf(
            StrokePoint(55f, 55f, 200L)
        )

        val hits = StrokeEraserHelper.intersects(eraserPoints, stroke, eraserRadius = 10f)
        assertTrue("Borracha deveria atingir o segmento", hits)
    }

    @Test
    fun testStrokeEraserHelperIgnoresDistantStroke() {
        val stroke = makeStroke(
            id = "stroke-1",
            points = listOf(
                StrokePoint(10f, 10f, 100L),
                StrokePoint(20f, 20f, 120L)
            )
        )

        val eraserPoints = listOf(
            StrokePoint(500f, 500f, 200L)
        )

        val hits = StrokeEraserHelper.intersects(eraserPoints, stroke, eraserRadius = 24f)
        assertFalse("Borracha distante não deve atingir o traço", hits)
    }

    @Test
    fun testRepositoryEraseAndUndo() {
        val repo = InMemoryStrokeRepository()

        val strokeA = makeStroke(
            id = "stroke-A",
            points = listOf(
                StrokePoint(0f, 0f, 100L),
                StrokePoint(50f, 50f, 110L)
            )
        )
        val strokeB = makeStroke(
            id = "stroke-B",
            points = listOf(
                StrokePoint(300f, 300f, 200L),
                StrokePoint(400f, 400f, 210L)
            )
        )

        repo.addStroke(strokeA)
        repo.addStroke(strokeB)
        assertEquals(2, repo.count)

        // Apaga passando por cima do Stroke A
        val eraserPoints = listOf(StrokePoint(25f, 25f, 300L))
        val erased = repo.eraseStrokesIntersecting(eraserPoints, eraserRadius = 20f)

        assertEquals(1, erased.size)
        assertEquals("stroke-A", erased[0].id)
        assertEquals(1, repo.count)
        assertEquals("stroke-B", repo.allStrokes[0].id)

        // Desfazer (Undo) deve restaurar Stroke A
        val restored = repo.removeLastStroke()
        assertEquals(2, repo.count)
        assertTrue(repo.allStrokes.any { it.id == "stroke-A" })
        assertTrue(repo.allStrokes.any { it.id == "stroke-B" })
    }

    @Test
    fun testMultiStrokeSweepErasesAllIntersected() {
        val repo = InMemoryStrokeRepository()
        val s1 = makeStroke("s1", listOf(StrokePoint(10f, 10f, 100L), StrokePoint(10f, 50f, 110L)))
        val s2 = makeStroke("s2", listOf(StrokePoint(30f, 10f, 100L), StrokePoint(30f, 50f, 110L)))
        val s3 = makeStroke("s3", listOf(StrokePoint(50f, 10f, 100L), StrokePoint(50f, 50f, 110L)))
        repo.addStroke(s1)
        repo.addStroke(s2)
        repo.addStroke(s3)

        // Traço da borracha corta horizontalmente de (0, 30) a (60, 30)
        val sweepPoints = listOf(
            StrokePoint(0f, 30f, 200L),
            StrokePoint(60f, 30f, 220L)
        )
        val erased = repo.eraseStrokesIntersecting(sweepPoints, eraserRadius = 5f)
        assertEquals(3, erased.size)
        assertTrue(repo.isEmpty)
    }

    @Test
    fun testSinglePointDotErasure() {
        val dotStroke = makeStroke("dot", listOf(StrokePoint(100f, 100f, 100L)))
        val nearbyEraser = listOf(StrokePoint(108f, 106f, 200L)) // dist = 10px <= 15px radius
        val hits = StrokeEraserHelper.intersects(nearbyEraser, dotStroke, eraserRadius = 15f)
        assertTrue("Borracha deve atingir ponto caligráfico isolado (ponto/acento)", hits)

        val farEraser = listOf(StrokePoint(130f, 130f, 200L)) // dist > 40px
        val misses = StrokeEraserHelper.intersects(farEraser, dotStroke, eraserRadius = 15f)
        assertFalse("Borracha além do raio não deve atingir o ponto", misses)
    }
}
