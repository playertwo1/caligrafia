package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineLineStyle

/**
 * Renderizador de alta performance de pautas caligráficas diretamente sobre o Canvas nativo do Android.
 *
 * Desenhado no plano de fundo (z-index inferior) para servir de guia visual sem interferir na tinta
 * ou no histórico de traços vetoriais.
 */
class GuidelineRenderer {

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val xHeightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val ascenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val descenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val slantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val pathBuffer = Path()

    /**
     * Aplica o estilo de linha ao Paint correspondente.
     */
    private fun applyStyle(paint: Paint, style: GuidelineLineStyle) {
        paint.color = style.colorArgb.toInt()
        paint.strokeWidth = style.strokeWidthPx
        if (style.isDashed && style.dashIntervals != null) {
            paint.pathEffect = DashPathEffect(style.dashIntervals, 0f)
        } else {
            paint.pathEffect = null
        }
    }

    /**
     * Desenha as pautas caligráficas na superfície.
     *
     * @param canvas O canvas do Android onde as guias serão desenhadas.
     * @param width Largura da superfície/página em pixels.
     * @param height Altura da superfície/página em pixels.
     * @param config Configuração da pauta (ou null para não desenhar).
     */
    fun draw(canvas: Canvas, width: Float, height: Float, config: GuidelineConfig?) {
        if (config == null || width <= 0f || height <= 0f) return

        // 1. Atualiza estilos dos Paints
        applyStyle(baselinePaint, config.baselineStyle)
        applyStyle(xHeightPaint, config.xHeightStyle)
        applyStyle(ascenderPaint, config.ascenderStyle)
        applyStyle(descenderPaint, config.descenderStyle)
        applyStyle(slantPaint, config.slantStyle)

        // 2. Calcula faixas de pauta
        val bands = config.computeBands(height)
        val left = config.leftMarginPx
        val right = width - config.rightMarginPx

        if (left >= right) return

        for (band in bands) {
            // 3. Desenha linhas de inclinação (se ativadas)
            val slantSegments = config.computeSlantSegments(width, band)
            for (seg in slantSegments) {
                canvas.drawLine(seg.startX, seg.startY, seg.endX, seg.endY, slantPaint)
            }

            // 4. Desenha as 4 linhas horizontais de caligrafia
            // Ascender
            canvas.drawLine(left, band.ascenderY, right, band.ascenderY, ascenderPaint)
            // Altura-X (Waistline)
            canvas.drawLine(left, band.xHeightY, right, band.xHeightY, xHeightPaint)
            // Baseline (Linha de base principal)
            canvas.drawLine(left, band.baselineY, right, band.baselineY, baselinePaint)
            // Descender
            canvas.drawLine(left, band.descenderY, right, band.descenderY, descenderPaint)
        }
    }
}
