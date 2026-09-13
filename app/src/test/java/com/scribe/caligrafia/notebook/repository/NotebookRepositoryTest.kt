package com.scribe.caligrafia.notebook.repository

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.notebook.serialization.NotebookManifestSerializer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class NotebookRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: LocalNotebookRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scribe_nb_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        repository = LocalNotebookRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun createNotebook_creates_notebook_with_first_page_and_empty_strokes() = runBlocking {
        val nb = repository.createNotebook("Caderno de Cursiva")
        assertNotNull(nb.id)
        assertEquals("Caderno de Cursiva", nb.title)
        assertEquals(1, nb.pageCount)

        val pages = repository.getPages(nb.id)
        assertEquals(1, pages.size)
        val firstPage = pages[0]
        assertEquals(0, firstPage.pageIndex)
        assertEquals(nb.id, firstPage.notebookId)

        val strokes = repository.loadPageStrokes(firstPage)
        assertTrue("Primeira página deve iniciar com 0 traços", strokes.isEmpty())
    }

    @Test
    fun addPage_and_getPages_maintains_order_and_index() = runBlocking {
        val nb = repository.createNotebook("Meu Diário")
        val page2 = repository.addPage(nb.id, GuidelineConfig.school())

        assertEquals(1, page2.pageIndex)
        assertEquals(GuidelineRatio.Ratio111, page2.guidelineConfig.ratio)

        val pages = repository.getPages(nb.id)
        assertEquals(2, pages.size)
        assertEquals(0, pages[0].pageIndex)
        assertEquals(1, pages[1].pageIndex)
    }

    @Test
    fun savePageStrokes_and_loadPageStrokes_preserves_stroke_data() = runBlocking {
        val nb = repository.createNotebook("Caderno de Teste")
        val pages = repository.getPages(nb.id)
        val page = pages[0]

        val p1 = StrokePoint(10f, 20f, 1000L, 0.5f, 0.25f, 0.8f)
        val p2 = StrokePoint(30f, 40f, 1016L, 0.7f, 0.30f, 0.82f)
        val stroke = Stroke(
            id = "stroke-1",
            tool = ToolType.STYLUS,
            points = listOf(p1, p2),
            startedAtMs = 1000L,
            endedAtMs = 1016L
        )

        val saved = repository.savePageStrokes(page, listOf(stroke))
        assertTrue("Gravação de traços deve retornar true", saved)

        val loaded = repository.loadPageStrokes(page)
        assertEquals(1, loaded.size)
        val loadedStroke = loaded[0]
        assertEquals("stroke-1", loadedStroke.id)
        assertEquals(2, loadedStroke.points.size)
        assertEquals(10f, loadedStroke.points[0].x, 0.001f)
        assertEquals(0.5f, loadedStroke.points[0].pressure!!, 0.001f)
        assertEquals(0.25f, loadedStroke.points[0].tiltRad!!, 0.001f)
    }

    @Test
    fun deletePage_removes_page_from_notebook_and_disk() = runBlocking {
        val nb = repository.createNotebook("Caderno Rascunho")
        val page2 = repository.addPage(nb.id)
        assertEquals(2, repository.getPages(nb.id).size)

        val deleted = repository.deletePage(nb.id, page2.id)
        assertTrue("Exclusão deve retornar true", deleted)

        val pagesAfter = repository.getPages(nb.id)
        assertEquals(1, pagesAfter.size)
        assertFalse(pagesAfter.any { it.id == page2.id })
    }

    @Test
    fun updatePageGuidelines_updates_persistently() = runBlocking {
        val nb = repository.createNotebook("Caderno Spencerian")
        val page = repository.getPages(nb.id)[0]

        val spencerian = GuidelineConfig.spencerian(xHeightPx = 42f)
        repository.updatePageGuidelines(page.id, spencerian)

        val updatedPage = repository.getPage(page.id)
        assertNotNull(updatedPage)
        assertEquals(GuidelineRatio.Ratio323, updatedPage!!.guidelineConfig.ratio)
        assertEquals(42f, updatedPage.guidelineConfig.xHeightPx, 0.001f)
        assertEquals(68f, updatedPage.guidelineConfig.slant?.angleDegrees ?: 0f, 0.001f)
    }

    @Test
    fun deleteNotebook_removes_all_pages_and_files() = runBlocking {
        val nb = repository.createNotebook("Caderno Efêmero")
        val nbId = nb.id
        assertNotNull(repository.getNotebook(nbId))

        val deleted = repository.deleteNotebook(nbId)
        assertTrue(deleted)
        assertNull(repository.getNotebook(nbId))
        assertTrue(repository.getPages(nbId).isEmpty())
    }

    @Test
    fun serializer_roundtrip_preserves_notebook_and_page_metadata() {
        val nb = com.scribe.caligrafia.core.model.Notebook(
            id = "nb-123",
            title = "Caligrafia Clássica\nCom Quebra",
            createdAt = 1000L,
            updatedAt = 2000L,
            pageIds = listOf("p1", "p2", "p3")
        )

        val serializedNb = NotebookManifestSerializer.serializeNotebook(nb)
        val deserializedNb = NotebookManifestSerializer.deserializeNotebook(serializedNb)

        assertEquals(nb.id, deserializedNb.id)
        assertEquals(nb.title, deserializedNb.title)
        assertEquals(nb.createdAt, deserializedNb.createdAt)
        assertEquals(nb.updatedAt, deserializedNb.updatedAt)
        assertEquals(nb.pageIds, deserializedNb.pageIds)
        assertEquals(nb.coverStyle, deserializedNb.coverStyle)

        val page = com.scribe.caligrafia.core.model.NotebookPage(
            id = "p-1",
            notebookId = "nb-123",
            pageIndex = 2,
            guidelineConfig = GuidelineConfig.copperplate(xHeightPx = 55f)
        )

        val serializedPage = NotebookManifestSerializer.serializePage(page)
        val deserializedPage = NotebookManifestSerializer.deserializePage(serializedPage)

        assertEquals(page.id, deserializedPage.id)
        assertEquals(page.notebookId, deserializedPage.notebookId)
        assertEquals(page.pageIndex, deserializedPage.pageIndex)
        assertEquals(55f, deserializedPage.guidelineConfig.xHeightPx, 0.001f)
        assertEquals(GuidelineRatio.Ratio212, deserializedPage.guidelineConfig.ratio)
        assertEquals(52f, deserializedPage.guidelineConfig.slant?.angleDegrees ?: 0f, 0.001f)
    }

    @Test
    fun renameNotebook_valid_title_updates_title_and_updatedAt() = runBlocking {
        val nb = repository.createNotebook("Título Antigo")
        val oldUpdated = nb.updatedAt

        Thread.sleep(10)
        val success = repository.renameNotebook(nb.id, "  Novo Título Valioso  ")
        assertTrue("Renomear deve retornar true", success)

        val updated = repository.getNotebook(nb.id)
        assertNotNull(updated)
        assertEquals("Novo Título Valioso", updated!!.title)
        assertTrue("updatedAt deve ter sido atualizado", updated.updatedAt >= oldUpdated)
    }

    @Test
    fun renameNotebook_invalid_blank_or_too_long_returns_false() = runBlocking {
        val nb = repository.createNotebook("Título Original")

        val emptyResult = repository.renameNotebook(nb.id, "   ")
        assertFalse("Não deve aceitar título em branco", emptyResult)

        val tooLongResult = repository.renameNotebook(nb.id, "A".repeat(41))
        assertFalse("Não deve aceitar título com mais de 40 caracteres", tooLongResult)

        val unchanged = repository.getNotebook(nb.id)
        assertEquals("Título Original", unchanged!!.title)
    }

    @Test
    fun duplicatePage_clones_page_with_new_ids_and_preserves_strokes() = runBlocking {
        val nb = repository.createNotebook("Caderno Duplicação")
        val originalPage = repository.getPages(nb.id)[0]

        val stroke = Stroke(
            id = "stroke-orig-1",
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(15f, 25f, 500L, 0.6f, 0.1f, 0.2f)),
            startedAtMs = 500L,
            endedAtMs = 520L
        )
        repository.savePageStrokes(originalPage, listOf(stroke))

        val duplicatedPage = repository.duplicatePage(nb.id, originalPage.id)
        assertNotNull("Página duplicada não pode ser nula", duplicatedPage)
        assertTrue("Nova página deve ter ID diferente", duplicatedPage!!.id != originalPage.id)
        assertEquals("Deve estar no final da lista de páginas", 1, duplicatedPage.pageIndex)

        // Verificar strokes duplicados
        val dupStrokes = repository.loadPageStrokes(duplicatedPage)
        assertEquals(1, dupStrokes.size)
        assertTrue("Stroke clonado deve ter ID único", dupStrokes[0].id != stroke.id)
        assertEquals(stroke.points.size, dupStrokes[0].points.size)
        assertEquals(15f, dupStrokes[0].points[0].x, 0.001f)
        assertEquals(0.6f, dupStrokes[0].points[0].pressure!!, 0.001f)

        // Modificar a cópia não deve alterar o original
        val newStroke = Stroke(
            id = "stroke-copy-extra",
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(100f, 200f, 600L)),
            startedAtMs = 600L,
            endedAtMs = 610L
        )
        repository.savePageStrokes(duplicatedPage, listOf(dupStrokes[0], newStroke))

        val origStrokesAfter = repository.loadPageStrokes(originalPage)
        assertEquals("Original deve continuar com apenas 1 stroke", 1, origStrokesAfter.size)
        assertEquals(stroke.id, origStrokesAfter[0].id)
    }
}
