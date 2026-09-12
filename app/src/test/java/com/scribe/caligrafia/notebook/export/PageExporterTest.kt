package com.scribe.caligrafia.notebook.export

import android.graphics.Color
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PageExporterTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scribe_export_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun exportToPng_throwsIOException_in_non_graphic_environment_and_does_not_create_invalid_file() {
        val page = NotebookPage(
            id = "page-export-1",
            notebookId = "nb-1",
            guidelineConfig = GuidelineConfig.copperplate()
        )

        val originalPoints = listOf(
            StrokePoint(10f, 20f, 1000L, 0.5f),
            StrokePoint(30f, 40f, 1016L, 0.7f)
        )
        val stroke = Stroke(
            id = "stroke-export-1",
            tool = ToolType.STYLUS,
            points = originalPoints,
            startedAtMs = 1000L,
            endedAtMs = 1016L,
            color = Color.BLACK,
            baseWidthPx = 4f
        )
        val strokes = listOf(stroke)
        val targetFile = File(tempDir, "exported_page.png")

        // No ambiente JVM stubbed (sem Skia/Android OS gráfico real), deve falhar com IOException explícita
        // sem jamais criar um arquivo PNG falso de 8 bytes (A09).
        val result = runCatching {
            PageExporter.exportToPng(
                page = page,
                strokes = strokes,
                targetFile = targetFile,
                options = PageExportOptions(widthPx = 1080, heightPx = 1920)
            )
        }

        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertTrue("Deve lançar IOException explicativa", exception is java.io.IOException)
            assertTrue("Arquivo não deve ter sido gerado como stub de 8 bytes", !targetFile.exists() || targetFile.length() != 8L)
        } else {
            // Se executado com runtime gráfico real (ex: Robolectric ou device)
            val file = result.getOrThrow()
            assertTrue(file.exists())
            assertTrue(file.length() > 8L)
        }

        // Princípio central: os traços vetoriais brutos não sofreram qualquer mutação
        assertEquals(1, strokes.size)
        assertEquals("stroke-export-1", strokes[0].id)
        assertEquals(2, strokes[0].points.size)
        assertEquals(10f, strokes[0].points[0].x, 0.0001f)
        assertEquals(0.5f, strokes[0].points[0].pressure)
    }

    @Test
    fun exportOptions_defaultsAreValid() {
        val options = PageExportOptions()
        assertEquals(1440, options.widthPx)
        assertEquals(2560, options.heightPx)
        assertTrue(options.includeGuidelines)
        assertEquals(Color.WHITE, options.backgroundColor)
        assertEquals(100, options.compressQuality)
    }
}
