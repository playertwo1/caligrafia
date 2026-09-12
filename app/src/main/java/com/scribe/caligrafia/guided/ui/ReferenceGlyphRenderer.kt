package com.scribe.caligrafia.guided.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.GuidelineBand
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.ReferenceGlyph
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renderizador visual de alta fidelidade para glifos de referência caligráficos.
 *
 * Suporta:
 * 1. Ghost Mode dinâmico (transparência de 100% a 0%).
 * 2. Suavização vetorial por curvas cúbicas/quadráticas nos pontos canônicos.
 * 3. Indicadores direcionais (círculo numerado de partida e seta de orientação).
 */
class ReferenceGlyphRenderer {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3.5f
    }

    private val hintCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(63, 81, 181) // Indigo elegante
    }

    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2.0f
        color = Color.rgb(63, 81, 181)
    }

    /**
     * Renderiza o glifo de referência no canvas na posição e nível de Ghost Mode especificados.
     */
    fun render(
        canvas: Canvas,
        glyph: ReferenceGlyph,
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float,
        ghostLevel: GhostModeLevel,
        showDirectionalHints: Boolean = true,
        baseColorRgb: Int = Color.rgb(63, 81, 181) // Indigo
    ) {
        if (!ghostLevel.isVisible) return

        val alphaInt = (ghostLevel.alpha * 255).roundToInt().coerceIn(0, 255)
        strokePaint.color = baseColorRgb
        strokePaint.alpha = alphaInt

        // Renderiza cada traço
        for (refStroke in glyph.strokes) {
            val screenPoints = refStroke.points.map { pt ->
                GeometricFeedbackEvaluator.mapToScreen(pt, band, originX, glyphWidthPx)
            }
            if (screenPoints.isEmpty()) continue

            val path = Path()
            path.moveTo(screenPoints.first().x, screenPoints.first().y)

            for (i in 1 until screenPoints.size) {
                val prev = screenPoints[i - 1]
                val curr = screenPoints[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                path.quadTo(prev.x, prev.y, midX, midY)
            }
            val last = screenPoints.last()
            path.lineTo(last.x, last.y)

            canvas.drawPath(path, strokePaint)

            // Indicadores de início e direção (apenas quando o Ghost Mode não for quase imperceptível)
            if (showDirectionalHints && ghostLevel.alpha >= 0.35f && refStroke.hint != null) {
                renderHint(canvas, refStroke, band, originX, glyphWidthPx, alphaInt)
            }
        }
    }

    private fun renderHint(
        canvas: Canvas,
        refStroke: com.scribe.caligrafia.guided.model.ReferenceStroke,
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float,
        alphaInt: Int
    ) {
        val hint = refStroke.hint ?: return
        val startPt = GeometricFeedbackEvaluator.mapToScreen(hint.startPoint, band, originX, glyphWidthPx)

        hintCirclePaint.alpha = (alphaInt * 0.9f).roundToInt().coerceIn(0, 255)
        hintTextPaint.alpha = alphaInt
        arrowPaint.alpha = (alphaInt * 0.8f).roundToInt().coerceIn(0, 255)

        val radius = 13f
        canvas.drawCircle(startPt.x, startPt.y, radius, hintCirclePaint)

        // Desenha número do traço
        val textY = startPt.y - (hintTextPaint.descent() + hintTextPaint.ascent()) / 2f
        canvas.drawText(hint.strokeOrder.toString(), startPt.x, textY, hintTextPaint)

        // Desenha pequena seta indicativa
        val dirAngle = atan2(hint.vectorDy.toDouble(), hint.vectorDx.toDouble()).toFloat()
        val arrowDist = radius + 12f
        val tipX = startPt.x + arrowDist * cos(dirAngle)
        val tipY = startPt.y - arrowDist * sin(dirAngle) // Coordenada Y cresce para baixo

        drawArrowHead(canvas, tipX, tipY, dirAngle)
    }

    private fun drawArrowHead(canvas: Canvas, tipX: Float, tipY: Float, angleRad: Float) {
        val arrowSize = 10f
        val wingAngle1 = angleRad + Math.toRadians(150.0).toFloat()
        val wingAngle2 = angleRad - Math.toRadians(150.0).toFloat()

        val x1 = tipX + arrowSize * cos(wingAngle1)
        val y1 = tipY - arrowSize * sin(wingAngle1)
        val x2 = tipX + arrowSize * cos(wingAngle2)
        val y2 = tipY - arrowSize * sin(wingAngle2)

        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
    }
}
