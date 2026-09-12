package com.scribe.caligrafia.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GuidelineConfigTest {

    @Test
    fun computeBands_with_111_ratio_calculates_equal_heights() {
        val config = GuidelineConfig(
            xHeightPx = 50f,
            ratio = GuidelineRatio.Ratio111,
            slant = null,
            topMarginPx = 50f,
            interlineGapPx = 50f
        )

        val bands = config.computeBands(pageHeight = 500f)
        assertTrue("Deve gerar ao menos uma faixa", bands.isNotEmpty())

        val band0 = bands[0]
        assertEquals("ascenderY deve iniciar na margem superior", 50f, band0.ascenderY, 0.001f)
        assertEquals("xHeightY deve somar a altura do ascender (50px)", 100f, band0.xHeightY, 0.001f)
        assertEquals("baselineY deve somar a altura-X (50px)", 150f, band0.baselineY, 0.001f)
        assertEquals("descenderY deve somar o descender (50px)", 200f, band0.descenderY, 0.001f)

        assertEquals(50f, band0.ascenderHeight, 0.001f)
        assertEquals(50f, band0.xHeight, 0.001f)
        assertEquals(50f, band0.descenderHeight, 0.001f)
        assertEquals(150f, band0.totalHeight, 0.001f)
    }

    @Test
    fun computeBands_with_copperplate_212_ratio_calculates_double_heights() {
        val config = GuidelineConfig.copperplate(xHeightPx = 40f).copy(
            topMarginPx = 60f,
            interlineGapPx = 30f
        )

        val bands = config.computeBands(pageHeight = 800f)
        assertTrue(bands.isNotEmpty())

        val b0 = bands[0]
        // Ratio 2:1:2 -> ascender = 80px, xHeight = 40px, descender = 80px. Total = 200px
        assertEquals(60f, b0.ascenderY, 0.001f)
        assertEquals(140f, b0.xHeightY, 0.001f)
        assertEquals(180f, b0.baselineY, 0.001f)
        assertEquals(260f, b0.descenderY, 0.001f)

        assertEquals(80f, b0.ascenderHeight, 0.001f)
        assertEquals(40f, b0.xHeight, 0.001f)
        assertEquals(80f, b0.descenderHeight, 0.001f)
        assertEquals(200f, b0.totalHeight, 0.001f)
    }

    @Test
    fun computeBands_separates_consecutive_bands_by_interlineGap() {
        val config = GuidelineConfig(
            xHeightPx = 50f,
            ratio = GuidelineRatio.Ratio111,
            topMarginPx = 50f,
            interlineGapPx = 40f
        )

        val bands = config.computeBands(pageHeight = 500f)
        assertTrue("Deve caber ao menos 2 faixas", bands.size >= 2)

        val b0 = bands[0] // 50 .. 200
        val b1 = bands[1] // 200 + 40 = 240 .. 390

        assertEquals(200f, b0.descenderY, 0.001f)
        assertEquals(240f, b1.ascenderY, 0.001f)
        assertEquals(40f, b1.ascenderY - b0.descenderY, 0.001f)
    }

    @Test
    fun computeBands_does_not_overflow_pageHeight() {
        val config = GuidelineConfig(
            xHeightPx = 50f,
            ratio = GuidelineRatio.Ratio111, // 150px por faixa
            topMarginPx = 50f,
            interlineGapPx = 50f
        )

        // b0: 50..200 (gap 50)
        // b1: 250..400
        // b2: precisaria de 450..600, mas pageHeight é 450 -> não cabe b2
        val bands = config.computeBands(pageHeight = 450f)
        assertEquals(2, bands.size)
        assertTrue(bands.last().descenderY <= 450f)
    }

    @Test
    fun computeSlantSegments_calculates_trigonometric_inclination_correctly() {
        val config = GuidelineConfig.copperplate(xHeightPx = 50f)
        val band = GuidelineBand(
            bandIndex = 0,
            ascenderY = 100f,
            xHeightY = 200f,
            baselineY = 250f,
            descenderY = 350f
        ) // totalHeight = 250f

        val segments = config.computeSlantSegments(pageWidth = 1080f, band = band)
        assertTrue("Deve gerar segmentos diagonais de inclinação", segments.isNotEmpty())

        val seg = segments[0]
        assertEquals(100f, seg.startY, 0.001f)
        assertEquals(350f, seg.endY, 0.001f)

        // Delta Y = 250f
        // Ângulo Copperplate = 52°
        // tan(52°) ≈ 1.27994f
        // dx esperado = 250 / 1.27994 ≈ 195.32f
        val deltaX = seg.startX - seg.endX
        val expectedDx = 250f / kotlin.math.tan(Math.toRadians(52.0)).toFloat()
        assertEquals(expectedDx, deltaX, 0.5f)
    }

    @Test
    fun computeSlantSegments_returns_empty_when_slant_is_null() {
        val config = GuidelineConfig.school().copy(slant = null)
        val band = GuidelineBand(0, 50f, 100f, 150f, 200f)

        val segments = config.computeSlantSegments(1080f, band)
        assertTrue("Pauta sem slant não deve gerar segmentos", segments.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_xHeight_throws_exception() {
        GuidelineConfig(xHeightPx = 3f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_slant_angle_throws_exception() {
        SlantConfig(angleDegrees = 5f)
    }

    @Test
    fun notebook_and_notebookPage_model_integrity() {
        val notebook = Notebook(
            title = "Caligrafia Cursiva",
            pageIds = listOf("page-1", "page-2")
        )
        assertEquals(2, notebook.pageCount)
        assertEquals("Caligrafia Cursiva", notebook.title)
        assertNotNull(notebook.id)

        val page = NotebookPage(
            notebookId = notebook.id,
            pageIndex = 0
        )
        assertEquals(notebook.id, page.notebookId)
        assertEquals(0, page.pageIndex)
        assertTrue(page.documentRelativePath.contains(page.id))
    }
}
