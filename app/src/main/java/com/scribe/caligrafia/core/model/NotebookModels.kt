package com.scribe.caligrafia.core.model

import java.util.UUID

/**
 * Representa um Caderno de Caligrafia composto por uma ou mais páginas.
 */
data class Notebook(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pageIds: List<String> = emptyList()
) {
    init {
        require(title.isNotBlank()) { "O título do caderno não pode estar vazio" }
    }

    val pageCount: Int
        get() = pageIds.size
}

/**
 * Representa uma Página individual de um Caderno de Caligrafia.
 *
 * Cada página possui suas pautas caligráficas associadas ([guidelineConfig])
 * e armazena seus traços vetoriais em um arquivo binário `.scribe` dedicado ([documentRelativePath]).
 */
data class NotebookPage(
    val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val pageIndex: Int = 0,
    val guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate(),
    val documentRelativePath: String = "notebooks/$notebookId/pages/$id.scribe",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(pageIndex >= 0) { "O índice da página deve ser >= 0" }
        require(notebookId.isNotBlank()) { "O ID do caderno pai não pode estar vazio" }
    }
}
