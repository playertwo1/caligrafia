package com.scribe.caligrafia.notebook.serialization

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.SlantConfig

/**
 * Serializador puro em Kotlin para metadados de Cadernos e Páginas.
 *
 * Não depende de bibliotecas externas ou stubs do android.jar, garantindo
 * portabilidade e testabilidade determinística na JVM e no Android.
 */
object NotebookManifestSerializer {

    // --- Serialização de Notebook ---

    fun serializeNotebook(notebook: Notebook): String {
        val sb = StringBuilder()
        sb.append("id=").append(notebook.id).append("\n")
        sb.append("title=").append(escapeString(notebook.title)).append("\n")
        sb.append("createdAt=").append(notebook.createdAt).append("\n")
        sb.append("updatedAt=").append(notebook.updatedAt).append("\n")
        sb.append("pageIds=").append(notebook.pageIds.joinToString(",")).append("\n")
        return sb.toString()
    }

    fun deserializeNotebook(content: String): Notebook {
        val lines = content.lines()
        var id = ""
        var title = "Caderno Sem Título"
        var createdAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()
        var pageIds = emptyList<String>()

        for (line in lines) {
            val trim = line.trim()
            if (trim.isEmpty() || !trim.contains("=")) continue
            val parts = trim.split("=", limit = 2)
            val key = parts[0].trim()
            val value = parts[1].trim()

            when (key) {
                "id" -> id = value
                "title" -> title = unescapeString(value)
                "createdAt" -> createdAt = value.toLongOrNull() ?: createdAt
                "updatedAt" -> updatedAt = value.toLongOrNull() ?: updatedAt
                "pageIds" -> {
                    pageIds = if (value.isBlank()) emptyList() else value.split(",").filter { it.isNotBlank() }
                }
            }
        }

        require(id.isNotBlank()) { "ID do caderno é obrigatório" }
        return Notebook(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            pageIds = pageIds
        )
    }

    // --- Serialização de NotebookPage ---

    fun serializePage(page: NotebookPage): String {
        val sb = StringBuilder()
        sb.append("id=").append(page.id).append("\n")
        sb.append("notebookId=").append(page.notebookId).append("\n")
        sb.append("pageIndex=").append(page.pageIndex).append("\n")
        sb.append("documentRelativePath=").append(page.documentRelativePath).append("\n")
        sb.append("createdAt=").append(page.createdAt).append("\n")
        sb.append("updatedAt=").append(page.updatedAt).append("\n")

        // GuidelineConfig
        val g = page.guidelineConfig
        sb.append("g.xHeightPx=").append(g.xHeightPx).append("\n")
        val ratioStr = when (g.ratio) {
            is GuidelineRatio.Ratio111 -> "1:1:1"
            is GuidelineRatio.Ratio212 -> "2:1:2"
            is GuidelineRatio.Ratio323 -> "3:2:3"
            is GuidelineRatio.Custom -> "custom:${g.ratio.ascenderRatio}:${g.ratio.descenderRatio}"
        }
        sb.append("g.ratio=").append(ratioStr).append("\n")
        sb.append("g.slantAngle=").append(g.slant?.angleDegrees ?: -1f).append("\n")
        sb.append("g.slantSpacing=").append(g.slant?.spacingPx ?: -1f).append("\n")
        sb.append("g.interlineGapPx=").append(g.interlineGapPx).append("\n")
        sb.append("g.topMarginPx=").append(g.topMarginPx).append("\n")

        return sb.toString()
    }

    fun deserializePage(content: String): NotebookPage {
        val lines = content.lines()
        var id = ""
        var notebookId = ""
        var pageIndex = 0
        var docPath = ""
        var createdAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()

        var xHeight = 50f
        var ratio: GuidelineRatio = GuidelineRatio.Ratio212
        var slantAngle = -1f
        var slantSpacing = -1f
        var interlineGap = 40f
        var topMargin = 60f

        for (line in lines) {
            val trim = line.trim()
            if (trim.isEmpty() || !trim.contains("=")) continue
            val parts = trim.split("=", limit = 2)
            val key = parts[0].trim()
            val value = parts[1].trim()

            when (key) {
                "id" -> id = value
                "notebookId" -> notebookId = value
                "pageIndex" -> pageIndex = value.toIntOrNull() ?: 0
                "documentRelativePath" -> docPath = value
                "createdAt" -> createdAt = value.toLongOrNull() ?: createdAt
                "updatedAt" -> updatedAt = value.toLongOrNull() ?: updatedAt
                "g.xHeightPx" -> xHeight = value.toFloatOrNull() ?: xHeight
                "g.ratio" -> {
                    ratio = when {
                        value == "1:1:1" -> GuidelineRatio.Ratio111
                        value == "2:1:2" -> GuidelineRatio.Ratio212
                        value == "3:2:3" -> GuidelineRatio.Ratio323
                        value.startsWith("custom:") -> {
                            val sub = value.removePrefix("custom:").split(":")
                            if (sub.size == 2) {
                                GuidelineRatio.Custom(sub[0].toFloatOrNull() ?: 1f, sub[1].toFloatOrNull() ?: 1f)
                            } else GuidelineRatio.Ratio212
                        }
                        else -> GuidelineRatio.Ratio212
                    }
                }
                "g.slantAngle" -> slantAngle = value.toFloatOrNull() ?: -1f
                "g.slantSpacing" -> slantSpacing = value.toFloatOrNull() ?: -1f
                "g.interlineGapPx" -> interlineGap = value.toFloatOrNull() ?: interlineGap
                "g.topMarginPx" -> topMargin = value.toFloatOrNull() ?: topMargin
            }
        }

        require(id.isNotBlank()) { "ID da página é obrigatório" }
        require(notebookId.isNotBlank()) { "NotebookId é obrigatório" }

        val slant = if (slantAngle > 0f && slantSpacing > 0f) {
            SlantConfig(angleDegrees = slantAngle, spacingPx = slantSpacing)
        } else null

        val guideline = GuidelineConfig(
            xHeightPx = xHeight,
            ratio = ratio,
            slant = slant,
            interlineGapPx = interlineGap,
            topMarginPx = topMargin
        )

        return NotebookPage(
            id = id,
            notebookId = notebookId,
            pageIndex = pageIndex,
            guidelineConfig = guideline,
            documentRelativePath = if (docPath.isNotBlank()) docPath else "notebooks/$notebookId/pages/$id.scribe",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun escapeString(str: String): String {
        return str.replace("\n", "\\n").replace("\r", "")
    }

    private fun unescapeString(str: String): String {
        return str.replace("\\n", "\n")
    }
}
