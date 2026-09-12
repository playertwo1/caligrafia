package com.scribe.caligrafia.notebook.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.ink.renderer.GuidelineRenderer
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer
import java.io.File
import java.io.FileOutputStream

/**
 * Configurações para exportação de página vetorial para imagem bitmap rasterizada.
 *
 * Princípio Arquitetural Inviolável:
 * A imagem gerada é estritamente um subproduto derivado de exportação/visualização.
 * Os traços vetoriais brutos (.scribe) originais NUNCA são substituídos, alterados ou descartados.
 */
data class PageExportOptions(
    val widthPx: Int = 1440,
    val heightPx: Int = 2560,
    val sourceWidthPx: Float? = null,
    val sourceHeightPx: Float? = null,
    val includeGuidelines: Boolean = true,
    val backgroundColor: Int = Color.WHITE,
    val compressQuality: Int = 100
)

/**
 * Utilitário de exportação de página caligráfica para formato PNG em alta resolução.
 */
object PageExporter {

    /**
     * Renderiza e grava uma página com seus traços vetoriais em um arquivo PNG em alta resolução.
     *
     * @param page Metadados e configuração de pautas da página.
     * @param strokes Lista imutável de traços vetoriais brutos pertencentes à página.
     * @param targetFile Arquivo de destino no disco (.png).
     * @param options Parâmetros de resolução, pauta e fundo.
     * @return O arquivo PNG gerado com sucesso.
     */
    fun exportToPng(
        page: NotebookPage,
        strokes: List<Stroke>,
        targetFile: File,
        options: PageExportOptions = PageExportOptions()
    ): File {
        val parent = targetFile.parentFile ?: File(".")
        if (!parent.exists()) parent.mkdirs()

        // A09: Erro explícito caso o ambiente não possua subsistema gráfico ou alocação falhe
        val bitmap = try {
            Bitmap.createBitmap(options.widthPx, options.heightPx, Bitmap.Config.ARGB_8888)
        } catch (e: Throwable) {
            null
        } ?: throw java.io.IOException(
            "Falha na renderização de bitmap gráfico para exportação: ambiente sem suporte gráfico ou memória insuficiente para ${options.widthPx}x${options.heightPx}"
        )

        try {
            val canvas = Canvas(bitmap)

            // A10: Projeção de escala caso o canvas de escrita tenha dimensão diferente da imagem de exportação
            val scaleX = if (options.sourceWidthPx != null && options.sourceWidthPx > 0f) {
                options.widthPx.toFloat() / options.sourceWidthPx
            } else 1f
            val scaleY = if (options.sourceHeightPx != null && options.sourceHeightPx > 0f) {
                options.heightPx.toFloat() / options.sourceHeightPx
            } else 1f

            val needsScale = scaleX != 1f || scaleY != 1f
            if (needsScale) {
                canvas.save()
                canvas.scale(scaleX, scaleY)
            }

            // 1. Fundo
            if (options.backgroundColor != Color.TRANSPARENT) {
                canvas.drawColor(options.backgroundColor)
            }

            // 2. Pautas caligráficas (se habilitado)
            if (options.includeGuidelines) {
                val guidelineRenderer = GuidelineRenderer()
                val effectiveW = if (options.sourceWidthPx != null && options.sourceWidthPx > 0f) options.sourceWidthPx else options.widthPx.toFloat()
                val effectiveH = if (options.sourceHeightPx != null && options.sourceHeightPx > 0f) options.sourceHeightPx else options.heightPx.toFloat()
                guidelineRenderer.draw(
                    canvas = canvas,
                    width = effectiveW,
                    height = effectiveH,
                    config = page.guidelineConfig
                )
            }

            // 3. Renderização fiel dos traços vetoriais (ignora borracha)
            val renderer = SmoothedReferenceRenderer()
            for (stroke in strokes) {
                if (stroke.tool != com.scribe.caligrafia.core.model.ToolType.ERASER) {
                    renderer.renderStroke(canvas, stroke)
                }
            }

            if (needsScale) {
                canvas.restore()
            }

            // 4. Compressão em stream PNG atômico para o disco
            val tempFile = File.createTempFile("scribe_export_", ".tmp", parent)
            try {
                FileOutputStream(tempFile).use { fos ->
                    val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, options.compressQuality, fos)
                    if (!compressed) {
                        throw java.io.IOException("Falha na compressão do bitmap em formato PNG")
                    }
                    fos.flush()
                }

                try {
                    java.nio.file.Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: Exception) {
                    java.nio.file.Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } catch (t: Throwable) {
                tempFile.delete()
                throw t
            }
        } finally {
            bitmap.recycle()
        }

        return targetFile
    }
}
