package com.scribe.caligrafia.core.model

import java.util.UUID

/**
 * Estilos de capa clássicos para cadernos de caligrafia (Fluxo 01).
 */
enum class NotebookCoverStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val spineColorHex: Long,
    val textColorHex: Long
) {
    PAPEL_ARTESANAL(
        id = "PAPEL_ARTESANAL",
        title = "Papel Artesanal",
        subtitle = "Creme clássico e pergaminho",
        primaryColorHex = 0xFFF7EFE2,
        secondaryColorHex = 0xFFE8DBC5,
        accentColorHex = 0xFF8C7355,
        spineColorHex = 0xFFC2AF94,
        textColorHex = 0xFF3D2E1E
    ),
    AZUL_NOITE(
        id = "AZUL_NOITE",
        title = "Azul Noite",
        subtitle = "Índigo profundo com detalhes prata",
        primaryColorHex = 0xFF1E293B,
        secondaryColorHex = 0xFF0F172A,
        accentColorHex = 0xFF94A3B8,
        spineColorHex = 0xFF334155,
        textColorHex = 0xFFF8FAFC
    ),
    COURO_SEPIA(
        id = "COURO_SEPIA",
        title = "Couro Sépia",
        subtitle = "Couro rústico com carimbo dourado",
        primaryColorHex = 0xFF5C3A21,
        secondaryColorHex = 0xFF3B2211,
        accentColorHex = 0xFFD4A373,
        spineColorHex = 0xFF2B180A,
        textColorHex = 0xFFFAEDCD
    ),
    VERDE_FLORESTA(
        id = "VERDE_FLORESTA",
        title = "Verde Floresta",
        subtitle = "Verde botânico imperial",
        primaryColorHex = 0xFF1B4332,
        secondaryColorHex = 0xFF081C15,
        accentColorHex = 0xFF52B788,
        spineColorHex = 0xFF2D6A4F,
        textColorHex = 0xFFD8F3DC
    );

    companion object {
        fun fromId(id: String?): NotebookCoverStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PAPEL_ARTESANAL
        }
    }
}

/**
 * Representa um Caderno de Caligrafia composto por uma ou mais páginas.
 */
data class Notebook(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val coverStyle: String = "PAPEL_ARTESANAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pageIds: List<String> = emptyList()
) {
    init {
        require(title.isNotBlank()) { "O título do caderno não pode estar vazio" }
    }

    val pageCount: Int
        get() = pageIds.size

    val coverStyleEnum: NotebookCoverStyle
        get() = NotebookCoverStyle.fromId(coverStyle)
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
