package com.scribe.caligrafia.notebook.repository

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.Stroke

/**
 * Contrato de repositório para gerenciamento de Cadernos, Páginas e persistência vetorial (.scribe).
 */
interface NotebookRepository {
    suspend fun getNotebooks(): List<Notebook>
    suspend fun getNotebook(id: String): Notebook?
    suspend fun createNotebook(
        title: String,
        initialGuideline: GuidelineConfig = GuidelineConfig.copperplate(),
        coverStyle: String = "PAPEL_ARTESANAL"
    ): Notebook
    suspend fun deleteNotebook(id: String): Boolean

    suspend fun getPages(notebookId: String): List<NotebookPage>
    suspend fun getPage(pageId: String): NotebookPage?
    suspend fun addPage(notebookId: String, guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate()): NotebookPage
    suspend fun deletePage(notebookId: String, pageId: String): Boolean
    suspend fun updatePageGuidelines(pageId: String, guidelineConfig: GuidelineConfig): Boolean

    suspend fun loadPageStrokes(page: NotebookPage): List<Stroke>
    suspend fun savePageStrokes(page: NotebookPage, strokes: List<Stroke>): Boolean
}
