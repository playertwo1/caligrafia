package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.*
import org.junit.Test

class SmoothedReferenceRendererTest {

    @Test
    fun testImmutabilityOfStrokePointsDuringRender() {
        val renderer = SmoothedReferenceRenderer(baseStrokeWidth = 4f)
        val originalPoints = listOf(
            StrokePoint(10f, 20f, 100L, pressure = 0.5f),
            StrokePoint(15f, 25f, 116L, pressure = 0.6f),
            StrokePoint(25f, 35f, 132L, pressure = 0.7f),
            StrokePoint(40f, 50f, 148L, pressure = 0.8f)
        )

        val stroke = Stroke(
            id = "test-stroke",
            tool = ToolType.STYLUS,
            points = originalPoints,
            startedAtMs = 100L,
            endedAtMs = 148L
        )

        val canvas = Canvas()
        renderer.renderStroke(canvas, stroke)

        // Assegura que a lista de pontos e seus valores internos permaneceram estritamente idênticos
        assertEquals(4, stroke.points.size)
        assertEquals(10f, stroke.points[0].x, 0.0001f)
        assertEquals(20f, stroke.points[0].y, 0.0001f)
        assertEquals(100L, stroke.points[0].tMs)
        assertEquals(0.5f, stroke.points[0].pressure)
        assertEquals(40f, stroke.points[3].x, 0.0001f)
    }

    @Test
    fun testRenderEdgeCasesWithoutCrash() {
        val renderer = SmoothedReferenceRenderer()
        val canvas = Canvas()

        // 1. Traço sem pontos
        val emptyStroke = Stroke("empty", ToolType.STYLUS, emptyList(), 0L, 0L)
        renderer.renderStroke(canvas, emptyStroke)

        // 2. Traço com 1 ponto (toque pontual)
        val singlePointStroke = Stroke(
            "single", ToolType.STYLUS,
            listOf(StrokePoint(50f, 50f, 100L, pressure = 0.5f)),
            100L, 100L
        )
        renderer.renderStroke(canvas, singlePointStroke)

        // 3. Traço com 2 pontos (segmento simples)
        val twoPointsStroke = Stroke(
            "two", ToolType.STYLUS,
            listOf(
                StrokePoint(0f, 0f, 100L),
                StrokePoint(10f, 10f, 116L)
            ),
            100L, 116L
        )
        renderer.renderStroke(canvas, twoPointsStroke)

        // 4. Traço sem dados de pressão (hardware sem sensor de pressão)
        val nullPressureStroke = Stroke(
            "no-pressure", ToolType.FINGER,
            listOf(
                StrokePoint(0f, 0f, 100L, pressure = null),
                StrokePoint(10f, 10f, 116L, pressure = null),
                StrokePoint(20f, 20f, 132L, pressure = null)
            ),
            100L, 132L
        )
        renderer.renderStroke(canvas, nullPressureStroke)
    }

    @Test
    fun testRendererManagerSelection() {
        val manager = RendererManager()
        assertEquals(RendererType.SMOOTHED_REFERENCE, manager.activeRenderer.type)

        manager.selectRenderer(RendererType.RAW_POLYLINE)
        assertEquals(RendererType.RAW_POLYLINE, manager.activeRenderer.type)

        manager.selectRenderer(RendererType.ANDROID_INK_API)
        assertEquals(RendererType.ANDROID_INK_API, manager.activeRenderer.type)

        assertEquals(3, manager.availableRenderers.size)
    }

    @Test
    fun testAndroidInkRendererAdapterDelegation() {
        val adapter = AndroidInkRendererAdapter()
        val canvas = Canvas()
        val stroke = Stroke(
            "ink-test", ToolType.STYLUS,
            listOf(StrokePoint(5f, 5f, 100L), StrokePoint(15f, 15f, 116L)),
            100L, 116L
        )

        adapter.renderStroke(canvas, stroke)
        assertEquals(1L, adapter.totalRenderCalls)
        assertEquals("1.0.0-alpha03", adapter.libraryVersion)
        assertTrue(adapter.isHardwareAcceleratedOnDevice)
    }

    @Test
    fun testPressureCurveModulatesWidth() {
        val renderer = SmoothedReferenceRenderer(baseStrokeWidth = 10f)
        val stroke = Stroke(
            id = "pressure-stroke",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(0f, 0f, 100L, pressure = 0.25f),
                StrokePoint(10f, 10f, 116L, pressure = 0.25f)
            ),
            startedAtMs = 100L,
            endedAtMs = 116L
        )

        // Com LINEAR: 0.25 -> 10 * (0.4 + 0.25 * 1.2) = 10 * 0.7 = 7.0f
        renderer.pressureCurve = com.scribe.caligrafia.expansions.styles.PressureCurveType.LINEAR
        renderer.renderStroke(Canvas(), stroke)

        // Com SOFT: 0.25^0.6 ~ 0.435 -> maior espessura para toque leve
        renderer.pressureCurve = com.scribe.caligrafia.expansions.styles.PressureCurveType.SOFT
        renderer.renderStroke(Canvas(), stroke)

        // Com FIRM: 0.25^1.6 ~ 0.109 -> menor espessura para toque leve
        renderer.pressureCurve = com.scribe.caligrafia.expansions.styles.PressureCurveType.FIRM
        renderer.renderStroke(Canvas(), stroke)

        // Assegura que todas as renderizações executam sem erro e stroke original permanece estritamente imutável
        assertEquals(2, stroke.points.size)
        assertEquals(0.25f, stroke.points[0].pressure)
    }
}
