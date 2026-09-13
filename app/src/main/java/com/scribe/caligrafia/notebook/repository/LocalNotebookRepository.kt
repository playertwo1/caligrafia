package com.scribe.caligrafia.notebook.repository

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.notebook.serialization.NotebookManifestSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Implementação local do repositório de cadernos e páginas.
 *
 * Estrutura no disco:
 *   [baseDir]/notebooks/[notebookId]/manifest.txt
 *   [baseDir]/notebooks/[notebookId]/pages/[pageId].meta
 *   [baseDir]/notebooks/[notebookId]/pages/[pageId].scribe
 *
 * Combina metadados leves em texto/manifest com armazenamento binário dedicado (.scribe)
 * de alta performance para os traços vetoriais brutos, conforme aprovado no spike SCR-006.
 */
class LocalNotebookRepository(
    private val baseDir: File,
    private val fileStrategy: DedicatedFileStrategy = DedicatedFileStrategy(baseDir)
) : NotebookRepository {

    private val notebooksRoot = File(baseDir, "notebooks")

    init {
        if (!notebooksRoot.exists()) {
            notebooksRoot.mkdirs()
        }
    }

    override suspend fun getNotebooks(): List<Notebook> = withContext(Dispatchers.IO) {
        val dirs = notebooksRoot.listFiles { file -> file.isDirectory } ?: return@withContext emptyList()
        val notebooks = mutableListOf<Notebook>()

        for (dir in dirs) {
            val manifestFile = File(dir, "manifest.txt")
            if (manifestFile.exists()) {
                try {
                    val content = manifestFile.readText()
                    val nb = NotebookManifestSerializer.deserializeNotebook(content)
                    notebooks.add(nb)
                } catch (e: Exception) {
                    // Ignora cadernos corrompidos sem derrubar o repositório
                }
            }
        }

        notebooks.sortedByDescending { it.updatedAt }
    }

    override suspend fun getNotebook(id: String): Notebook? = withContext(Dispatchers.IO) {
        val dir = File(notebooksRoot, id)
        val manifestFile = File(dir, "manifest.txt")
        if (!manifestFile.exists()) return@withContext null
        try {
            NotebookManifestSerializer.deserializeNotebook(manifestFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createNotebook(
        title: String,
        initialGuideline: GuidelineConfig,
        coverStyle: String
    ): Notebook = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val nbDir = File(notebooksRoot, id)
        nbDir.mkdirs()

        // Cria a primeira página com o guideline escolhido
        val firstPageId = UUID.randomUUID().toString()
        val pagesDir = File(nbDir, "pages")
        pagesDir.mkdirs()

        val firstPage = NotebookPage(
            id = firstPageId,
            notebookId = id,
            pageIndex = 0,
            guidelineConfig = initialGuideline,
            documentRelativePath = "notebooks/$id/pages/$firstPageId.scribe"
        )
        val pageMetaFile = File(pagesDir, "$firstPageId.meta")
        atomicWriteText(pageMetaFile, NotebookManifestSerializer.serializePage(firstPage))

        // Inicializa o arquivo binário .scribe vazio da primeira página
        val scribeFile = File(baseDir, firstPage.documentRelativePath)
        fileStrategy.save(scribeFile, emptyList())

        val notebook = Notebook(
            id = id,
            title = title,
            coverStyle = coverStyle,
            pageIds = listOf(firstPageId)
        )
        val manifestFile = File(nbDir, "manifest.txt")
        atomicWriteText(manifestFile, NotebookManifestSerializer.serializeNotebook(notebook))

        notebook
    }

    override suspend fun renameNotebook(id: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 40) return@withContext false
        val notebook = getNotebook(id) ?: return@withContext false
        val updated = notebook.copy(
            title = cleanTitle,
            updatedAt = System.currentTimeMillis()
        )
        val nbDir = File(notebooksRoot, id)
        val manifestFile = File(nbDir, "manifest.txt")
        atomicWriteText(manifestFile, NotebookManifestSerializer.serializeNotebook(updated))
        true
    }

    override suspend fun deleteNotebook(id: String): Boolean = withContext(Dispatchers.IO) {
        val nbDir = File(notebooksRoot, id)
        if (!nbDir.exists()) return@withContext false
        nbDir.deleteRecursively()
    }

    override suspend fun getPages(notebookId: String): List<NotebookPage> = withContext(Dispatchers.IO) {
        val notebook = getNotebook(notebookId) ?: return@withContext emptyList()
        val pagesDir = File(File(notebooksRoot, notebookId), "pages")
        if (!pagesDir.exists()) return@withContext emptyList()

        val pages = mutableListOf<NotebookPage>()
        for (pageId in notebook.pageIds) {
            val metaFile = File(pagesDir, "$pageId.meta")
            if (metaFile.exists()) {
                try {
                    val page = NotebookManifestSerializer.deserializePage(metaFile.readText())
                    pages.add(page)
                } catch (e: Exception) {
                    // Ignora páginas corrompidas
                }
            }
        }

        pages.sortedBy { it.pageIndex }
    }

    override suspend fun getPage(pageId: String): NotebookPage? = withContext(Dispatchers.IO) {
        // Procura a página inspecionando os cadernos
        val dirs = notebooksRoot.listFiles { file -> file.isDirectory } ?: return@withContext null
        for (nbDir in dirs) {
            val metaFile = File(File(nbDir, "pages"), "$pageId.meta")
            if (metaFile.exists()) {
                return@withContext try {
                    NotebookManifestSerializer.deserializePage(metaFile.readText())
                } catch (e: Exception) {
                    null
                }
            }
        }
        null
    }

    override suspend fun addPage(
        notebookId: String,
        guidelineConfig: GuidelineConfig
    ): NotebookPage = withContext(Dispatchers.IO) {
        val notebook = getNotebook(notebookId) ?: throw IllegalArgumentException("Caderno não encontrado: $notebookId")
        val nbDir = File(notebooksRoot, notebookId)
        val pagesDir = File(nbDir, "pages")
        pagesDir.mkdirs()

        val pageId = UUID.randomUUID().toString()
        val newIndex = notebook.pageIds.size
        val newPage = NotebookPage(
            id = pageId,
            notebookId = notebookId,
            pageIndex = newIndex,
            guidelineConfig = guidelineConfig,
            documentRelativePath = "notebooks/$notebookId/pages/$pageId.scribe"
        )

        // Salva metadados da página
        val pageMetaFile = File(pagesDir, "$pageId.meta")
        atomicWriteText(pageMetaFile, NotebookManifestSerializer.serializePage(newPage))

        // Inicializa arquivo de traços .scribe
        val scribeFile = File(baseDir, newPage.documentRelativePath)
        fileStrategy.save(scribeFile, emptyList())

        // Atualiza o manifesto do caderno
        val updatedNb = notebook.copy(
            pageIds = notebook.pageIds + pageId,
            updatedAt = System.currentTimeMillis()
        )
        val manifestFile = File(nbDir, "manifest.txt")
        atomicWriteText(manifestFile, NotebookManifestSerializer.serializeNotebook(updatedNb))

        newPage
    }

    override suspend fun duplicatePage(notebookId: String, sourcePageId: String): NotebookPage? = withContext(Dispatchers.IO) {
        val notebook = getNotebook(notebookId) ?: return@withContext null
        val sourcePage = getPage(sourcePageId) ?: return@withContext null
        val sourceStrokes = loadPageStrokes(sourcePage)

        val nbDir = File(notebooksRoot, notebookId)
        val pagesDir = File(nbDir, "pages")
        if (!pagesDir.exists()) pagesDir.mkdirs()

        val newPageId = UUID.randomUUID().toString()
        val newPage = NotebookPage(
            id = newPageId,
            notebookId = notebookId,
            pageIndex = notebook.pageIds.size,
            guidelineConfig = sourcePage.guidelineConfig,
            documentRelativePath = "notebooks/$notebookId/pages/$newPageId.scribe"
        )

        // Salva metadados da nova página
        val pageMetaFile = File(pagesDir, "$newPageId.meta")
        atomicWriteText(pageMetaFile, NotebookManifestSerializer.serializePage(newPage))

        // Salva traços duplicados com novos IDs únicos
        val duplicatedStrokes = sourceStrokes.map { original ->
            original.copy(id = UUID.randomUUID().toString())
        }
        val scribeFile = File(baseDir, newPage.documentRelativePath)
        fileStrategy.save(scribeFile, duplicatedStrokes)

        // Atualiza o manifesto do caderno
        val updatedNb = notebook.copy(
            pageIds = notebook.pageIds + newPageId,
            updatedAt = System.currentTimeMillis()
        )
        val manifestFile = File(nbDir, "manifest.txt")
        atomicWriteText(manifestFile, NotebookManifestSerializer.serializeNotebook(updatedNb))

        newPage
    }

    override suspend fun deletePage(notebookId: String, pageId: String): Boolean = withContext(Dispatchers.IO) {
        val notebook = getNotebook(notebookId) ?: return@withContext false
        if (!notebook.pageIds.contains(pageId)) return@withContext false

        val nbDir = File(notebooksRoot, notebookId)
        val pagesDir = File(nbDir, "pages")
        val metaFile = File(pagesDir, "$pageId.meta")
        val scribeFile = File(pagesDir, "$pageId.scribe")

        if (metaFile.exists()) metaFile.delete()
        if (scribeFile.exists()) scribeFile.delete()

        val newPageIds = notebook.pageIds.filter { it != pageId }
        val updatedNb = notebook.copy(
            pageIds = newPageIds,
            updatedAt = System.currentTimeMillis()
        )
        val manifestFile = File(nbDir, "manifest.txt")
        atomicWriteText(manifestFile, NotebookManifestSerializer.serializeNotebook(updatedNb))
        true
    }

    override suspend fun updatePageGuidelines(pageId: String, guidelineConfig: GuidelineConfig): Boolean = withContext(Dispatchers.IO) {
        val page = getPage(pageId) ?: return@withContext false
        val pagesDir = File(File(notebooksRoot, page.notebookId), "pages")
        val metaFile = File(pagesDir, "$pageId.meta")

        val updatedPage = page.copy(
            guidelineConfig = guidelineConfig,
            updatedAt = System.currentTimeMillis()
        )
        atomicWriteText(metaFile, NotebookManifestSerializer.serializePage(updatedPage))
        true
    }

    override suspend fun loadPageStrokes(page: NotebookPage): List<Stroke> = withContext(Dispatchers.IO) {
        val scribeFile = File(baseDir, page.documentRelativePath)
        if (!scribeFile.exists()) return@withContext emptyList()
        fileStrategy.load(scribeFile)
    }

    override suspend fun savePageStrokes(page: NotebookPage, strokes: List<Stroke>): Boolean = withContext(Dispatchers.IO) {
        val scribeFile = File(baseDir, page.documentRelativePath)
        scribeFile.parentFile?.mkdirs()
        fileStrategy.save(scribeFile, strokes)
        true
    }

    private fun atomicWriteText(file: File, content: String) {
        val parent = file.parentFile ?: notebooksRoot
        if (!parent.exists()) parent.mkdirs()
        val tempFile = File.createTempFile("scribe_meta_", ".tmp", parent)
        try {
            tempFile.writeText(content)
            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }
}
