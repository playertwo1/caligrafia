package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SignatureExporterTest {

    @Test
    fun exportToSvg_generatesValidSvgContainerAndPaths() {
        val points = listOf(
            StrokePoint(10f, 20f, 1000L, 0.5f, null, null),
            StrokePoint(30f, 40f, 1050L, 0.6f, null, null),
            StrokePoint(50f, 20f, 1100L, 0.5f, null, null)
        )
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = points,
            startedAtMs = 1000L,
            endedAtMs = 1100L,
            isCancelled = false,
            color = 0xFF000000.toInt(),
            baseWidthPx = 3.5f
        )

        val svg = SignatureExporter.exportToSvg(listOf(stroke), width = 500, height = 200)

        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("viewBox=\"0 0 500 200\""))
        assertTrue(svg.contains("<path d=\"M 10.00 20.00 L 30.00 40.00 L 50.00 20.00\""))
        assertTrue(svg.contains("stroke-width=\"3.50\""))
        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun exportToSvg_emptyStrokes_producesValidContainer() {
        val svg = SignatureExporter.exportToSvg(emptyList(), width = 400, height = 200)
        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("</svg>"))
    }
}
