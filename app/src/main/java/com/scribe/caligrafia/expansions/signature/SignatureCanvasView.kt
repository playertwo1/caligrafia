package com.scribe.caligrafia.expansions.signature

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.capture.StrokeEraserHelper

/**
 * Superfície nativa de escrita otimizada para assinatura, rubricas e monogramas com a S Pen.
 *
 * F5.01: usa o mesmo [StrokeCapturePipeline] da escrita principal para manter uma única política de
 * pointer, historical samples, endpoint de ACTION_UP, pressão/tilt/orientação e borracha.
 * A assinatura continua sendo armazenada como raw strokes; a View apenas mantém o conjunto atual.
 */
class SignatureCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val completedStrokes = mutableListOf<Stroke>()

    var onStrokeFinished: ((List<Stroke>) -> Unit)? = null

    private val eraserRadiusPx = 24f

    private val baselinePaint = Paint().apply {
        color = Color.parseColor("#3B82F6")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val guidelinePaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        isAntiAlias = true
    }

    private val flourishPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val guidelinePath = Path()

    private val capturePipeline = StrokeCapturePipeline(
        toolConfig = ToolConfig(
            customStrokeWidthPx = strokePaint.strokeWidth,
            eraserRadiusPx = eraserRadiusPx
        ),
        onStrokeCompleted = { stroke ->
            completedStrokes.add(stroke)
            invalidate()
            notifyStrokeSetChanged()
        },
        onStrokeCancelled = {
            invalidate()
        },
        onStrokePointAdded = {
            invalidate()
        },
        onEraserPointsAdded = { eraserPoints ->
            val removed = completedStrokes.removeAll { stroke ->
                StrokeEraserHelper.intersects(
                    eraserPoints = eraserPoints,
                    stroke = stroke,
                    eraserRadius = eraserRadiusPx
                )
            }
            if (removed) {
                invalidate()
                notifyStrokeSetChanged()
            }
        }
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun getStrokes(): List<Stroke> = completedStrokes.toList()

    /**
     * Restaura o conjunto bruto no canvas. Usado também quando a AndroidView é recriada após
     * navegação/configuration change (F5.04).
     */
    fun setStrokes(newStrokes: List<Stroke>) {
        capturePipeline.flushActiveStroke(commitIfValid = false)
        completedStrokes.clear()
        completedStrokes.addAll(newStrokes)
        invalidate()
    }

    fun clearCanvas() {
        capturePipeline.flushActiveStroke(commitIfValid = false)
        completedStrokes.clear()
        invalidate()
        notifyStrokeSetChanged()
    }

    fun undoLastStroke() {
        capturePipeline.flushActiveStroke(commitIfValid = false)
        if (completedStrokes.isNotEmpty()) {
            completedStrokes.removeAt(completedStrokes.size - 1)
            invalidate()
            notifyStrokeSetChanged()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }

        val consumed = capturePipeline.onMotionEvent(event)
        if (consumed) invalidate()
        return consumed || super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val consumed = capturePipeline.onHoverEvent(event)
        return consumed || super.onHoverEvent(event)
    }

    override fun onDetachedFromWindow() {
        capturePipeline.flushActiveStroke(commitIfValid = true)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val baselineY = h * 0.65f
        val xHeightY = h * 0.45f
        val ascenderY = h * 0.22f
        val descenderY = h * 0.85f

        canvas.drawLine(w * 0.05f, baselineY, w * 0.95f, baselineY, baselinePaint)

        guidelinePath.reset()
        guidelinePath.moveTo(w * 0.05f, xHeightY)
        guidelinePath.lineTo(w * 0.95f, xHeightY)
        guidelinePath.moveTo(w * 0.05f, ascenderY)
        guidelinePath.lineTo(w * 0.95f, ascenderY)
        guidelinePath.moveTo(w * 0.05f, descenderY)
        guidelinePath.lineTo(w * 0.95f, descenderY)
        canvas.drawPath(guidelinePath, guidelinePaint)

        canvas.drawOval(
            w * 0.08f,
            h * 0.15f,
            w * 0.92f,
            h * 0.88f,
            flourishPaint
        )

        for (stroke in completedStrokes) {
            drawStroke(
                canvas = canvas,
                points = stroke.points,
                color = stroke.color ?: strokePaint.color,
                widthPx = stroke.baseWidthPx ?: strokePaint.strokeWidth
            )
        }

        capturePipeline.getActiveStrokePreview()?.let { activeStroke ->
            drawStroke(
                canvas = canvas,
                points = activeStroke.points,
                color = activeStroke.color ?: strokePaint.color,
                widthPx = activeStroke.baseWidthPx ?: strokePaint.strokeWidth
            )
        }
    }

    private fun notifyStrokeSetChanged() {
        onStrokeFinished?.invoke(completedStrokes.toList())
    }

    private fun drawStroke(canvas: Canvas, points: List<StrokePoint>, color: Int, widthPx: Float) {
        if (points.isEmpty()) return

        strokePaint.color = color
        strokePaint.strokeWidth = widthPx

        if (points.size == 1) {
            val p = points[0]
            canvas.drawCircle(p.x, p.y, widthPx / 2f, strokePaint)
            return
        }

        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        canvas.drawPath(path, strokePaint)
    }
}
