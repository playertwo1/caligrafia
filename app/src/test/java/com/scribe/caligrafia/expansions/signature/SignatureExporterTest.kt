package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureExporterTest {

    @Test
    fun exportToSvg_negativeAndWideBounds_areUniformlyFittedInsideViewBox() {
        val stroke = Stroke(
            id = "wide-negative",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(-200f, -30f, 1000L, 0f, 0f, 0f),
                StrokePoint(100f, 20f, 1050L, 0.6f, 0.1f, 0.2f),
                StrokePoint(900f, 160f, 1100L, 1f, 0f, 0f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1100L,
            isCancelled = false,
            color = 0xFF000000.toInt(),
            baseWidthPx = 12f
        )

        val svg = SignatureExporter.exportToSvg(listOf(stroke), width = 500, height = 200)

        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("viewBox=\"0 0 500 200\""))

        val pathData = Regex("<path d=\"([^\"]+)\"").find(svg)?.groupValues?.get(1)
            ?: error("path ausente")
        val coords = Regex("-?\\d+(?:\\.\\d+)?").findAll(pathData).map { it.value.toFloat() }.toList()
        assertTrue(coords.size >= 6)
        coords.chunked(2).forEach { pair ->
            assertTrue("x fora do viewBox: ${pair[0]}", pair[0] in 0f..500f)
            assertTrue("y fora do viewBox: ${pair[1]}", pair[1] in 0f..200f)
        }

        val exportedWidth = Regex("stroke-width=\"([0-9.]+)\"")
            .find(svg)?.groupValues?.get(1)?.toFloat() ?: 0f
        assertTrue(exportedWidth > 0f)
    }

    @Test
    fun exportToSvg_singlePoint_includesStrokeRadiusWithoutClipping() {
        val stroke = Stroke(
            id = "point",
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(-100f, -100f, 1L, 0f, 0f, 0f)),
            startedAtMs = 1L,
            endedAtMs = 1L,
            isCancelled = false,
            baseWidthPx = 20f
        )

        val svg = SignatureExporter.exportToSvg(listOf(stroke), width = 400, height = 200)
        val circle = Regex("<circle cx=\"([0-9.]+)\" cy=\"([0-9.]+)\" r=\"([0-9.]+)\"")
            .find(svg) ?: error("circle ausente")
        val cx = circle.groupValues[1].toFloat()
        val cy = circle.groupValues[2].toFloat()
        val radius = circle.groupValues[3].toFloat()

        assertTrue(radius > 0f)
        assertTrue(cx - radius >= 0f)
        assertTrue(cy - radius >= 0f)
        assertTrue(cx + radius <= 400f)
        assertTrue(cy + radius <= 200f)
    }

    @Test
    fun exportToSvg_emptyStrokes_producesValidContainer() {
        val svg = SignatureExporter.exportToSvg(emptyList(), width = 400, height = 200)
        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("</svg>"))
    }
}
