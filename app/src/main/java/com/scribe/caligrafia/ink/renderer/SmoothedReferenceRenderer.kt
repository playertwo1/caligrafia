package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.expansions.styles.PressureCalibration
import com.scribe.caligrafia.expansions.styles.PressureCurveType

/**
 * Renderizador de referência nativo em Canvas Android.
 *
 * Utiliza curvas Bézier quadráticas interpoladas nos pontos médios entre amostras consecutivas,
 * produzindo traços naturais e fluidos. A espessura é modulada pela pressão real da caneta quando disponível.
 *
 * Princípio inviolável: os pontos brutos originais do Stroke permanecem 100% inalterados.
 */
class SmoothedReferenceRenderer(
    private val baseStrokeWidth: Float = 5.0f,
    private val strokeColor: Int = Color.rgb(15, 23, 42),      // Slate 900
    private val activeStrokeColor: Int = Color.rgb(37, 99, 235), // Accent Blue
    var pressureCurve: PressureCurveType = PressureCurveType.LINEAR
) : InkRenderer {

    override val type: RendererType = RendererType.SMOOTHED_REFERENCE

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = strokeColor
        strokeWidth = baseStrokeWidth
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = activeStrokeColor
        strokeWidth = baseStrokeWidth
    }

    private val workingPath = Path()

    override fun renderStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        val targetWidth = stroke.baseWidthPx ?: baseStrokeWidth
        val targetColor = stroke.color ?: strokeColor
        val paint = completedPaint.apply {
            color = targetColor
            strokeWidth = computeModulatedWidth(stroke.points, targetWidth)
        }
        drawSmoothedPoints(canvas, stroke.points, paint)
    }

    override fun renderActiveStroke(canvas: Canvas, activeStroke: Stroke) {
        if (activeStroke.points.isEmpty()) return
        val targetWidth = activeStroke.baseWidthPx ?: baseStrokeWidth
        val targetColor = activeStroke.color ?: activeStrokeColor
        val paint = activePaint.apply {
            color = targetColor
            strokeWidth = computeModulatedWidth(activeStroke.points, targetWidth)
        }
        drawSmoothedPoints(canvas, activeStroke.points, paint)
    }

    private fun drawSmoothedPoints(canvas: Canvas, points: List<StrokePoint>, paint: Paint) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            val p = points[0]
            val radius = (paint.strokeWidth / 2f).coerceAtLeast(1.5f)
            canvas.drawCircle(p.x, p.y, radius, paint)
            return
        }

        if (points.size == 2) {
            val p0 = points[0]
            val p1 = points[1]
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
            return
        }

        workingPath.reset()
        val first = points[0]
        workingPath.moveTo(first.x, first.y)

        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            workingPath.quadTo(current.x, current.y, midX, midY)
        }

        val last = points.last()
        workingPath.lineTo(last.x, last.y)

        canvas.drawPath(workingPath, paint)
    }

    private fun computeModulatedWidth(points: List<StrokePoint>, targetBaseWidth: Float = baseStrokeWidth): Float {
        val validPressures = points.mapNotNull { it.pressure }
        if (validPressures.isEmpty()) return targetBaseWidth

        val avgPressure = validPressures.average().toFloat()
        val calibratedPressure = PressureCalibration.transform(avgPressure, pressureCurve)
        // Modulação caligráfica calibrada: com pressão mínima 0.0 -> 40% da espessura; pressão máxima 1.0 -> 160%
        return targetBaseWidth * (0.4f + calibratedPressure * 1.2f)
    }
}
