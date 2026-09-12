package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint

/**
 * Renderizador de controle direto (Raw Polyline).
 *
 * Conecta cada amostra física consecutiva por segmentos retos sem qualquer curva ou interpolação.
 * Serve como referência básica de latência, densidade de pontos capturados e geometria bruta.
 */
class RawPolylineRenderer(
    private val strokeWidth: Float = 4.0f,
    private val strokeColor: Int = Color.rgb(15, 23, 42),
    private val activeStrokeColor: Int = Color.rgb(37, 99, 235)
) : InkRenderer {

    override val type: RendererType = RendererType.RAW_POLYLINE

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = strokeColor
        strokeWidth = this@RawPolylineRenderer.strokeWidth
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = activeStrokeColor
        strokeWidth = this@RawPolylineRenderer.strokeWidth
    }

    private val workingPath = Path()

    override fun renderStroke(canvas: Canvas, stroke: Stroke) {
        val paint = completedPaint.apply {
            color = stroke.color ?: strokeColor
            strokeWidth = stroke.baseWidthPx ?: this@RawPolylineRenderer.strokeWidth
        }
        drawPolyline(canvas, stroke.points, paint)
    }

    override fun renderActiveStroke(canvas: Canvas, activeStroke: Stroke) {
        val paint = activePaint.apply {
            color = activeStroke.color ?: activeStrokeColor
            strokeWidth = activeStroke.baseWidthPx ?: this@RawPolylineRenderer.strokeWidth
        }
        drawPolyline(canvas, activeStroke.points, paint)
    }

    private fun drawPolyline(canvas: Canvas, points: List<StrokePoint>, paint: Paint) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            val p = points[0]
            canvas.drawPoint(p.x, p.y, paint)
            return
        }

        workingPath.reset()
        val first = points[0]
        workingPath.moveTo(first.x, first.y)

        for (i in 1 until points.size) {
            val p = points[i]
            workingPath.lineTo(p.x, p.y)
        }

        canvas.drawPath(workingPath, paint)
    }
}
