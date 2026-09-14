package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineLineStyle
import com.scribe.caligrafia.preferences.ScribePreferencesRuntime

/**
 * Renderizador de pautas caligráficas no Canvas nativo.
 * A preferência de contraste afeta somente o Paint derivado; configurações e raw strokes não mudam.
 */
class GuidelineRenderer {

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val xHeightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ascenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val descenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val slantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pathBuffer = Path()

    private fun applyStyle(paint: Paint, style: GuidelineLineStyle) {
        val baseColor = style.colorArgb.toInt()
        val contrastAlpha = ScribePreferencesRuntime.current.guideContrast.alpha
        paint.color = Color.argb(
            (255f * contrastAlpha).toInt().coerceIn(0, 255),
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
        paint.strokeWidth = style.strokeWidthPx
        if (style.isDashed && style.dashIntervals != null) {
            paint.pathEffect = DashPathEffect(style.dashIntervals, 0f)
        } else {
            paint.pathEffect = null
        }
    }

    fun draw(canvas: Canvas, width: Float, height: Float, config: GuidelineConfig?) {
        if (config == null || width <= 0f || height <= 0f) return

        applyStyle(baselinePaint, config.baselineStyle)
        applyStyle(xHeightPaint, config.xHeightStyle)
        applyStyle(ascenderPaint, config.ascenderStyle)
        applyStyle(descenderPaint, config.descenderStyle)
        applyStyle(slantPaint, config.slantStyle)

        val bands = config.computeBands(height)
        val left = config.leftMarginPx
        val right = width - config.rightMarginPx
        if (left >= right) return

        for (band in bands) {
            val slantSegments = config.computeSlantSegments(width, band)
            for (seg in slantSegments) {
                canvas.drawLine(seg.startX, seg.startY, seg.endX, seg.endY, slantPaint)
            }

            canvas.drawLine(left, band.ascenderY, right, band.ascenderY, ascenderPaint)
            canvas.drawLine(left, band.xHeightY, right, band.xHeightY, xHeightPaint)
            canvas.drawLine(left, band.baselineY, right, band.baselineY, baselinePaint)
            canvas.drawLine(left, band.descenderY, right, band.descenderY, descenderPaint)
        }
    }
}
