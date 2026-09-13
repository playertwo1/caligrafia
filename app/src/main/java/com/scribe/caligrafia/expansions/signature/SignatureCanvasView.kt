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
import com.scribe.caligrafia.core.model.ToolType
import java.util.UUID

/**
 * Superfície nativa de escrita otimizada para assinatura, rubricas e monogramas com a S Pen.
 * Inclui pautas específicas de assinatura e zona elíptica de floreio.
 */
class SignatureCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val completedStrokes = mutableListOf<Stroke>()
    private var activeStrokePoints = mutableListOf<StrokePoint>()
    private var activeStrokeStartTime = 0L

    var onStrokeFinished: ((List<Stroke>) -> Unit)? = null

    // Pautas de assinatura
    private val baselinePaint = Paint().apply {
        color = Color.parseColor("#3B82F6") // Azul
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val guidelinePaint = Paint().apply {
        color = Color.parseColor("#94A3B8") // Slate claro tracejado
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        isAntiAlias = true
    }

    private val flourishPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1") // Cinza muito sutil
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.parseColor("#0F172A") // Nanquim profundo
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val guidelinePath = Path()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun getStrokes(): List<Stroke> = completedStrokes.toList()

    fun setStrokes(newStrokes: List<Stroke>) {
        completedStrokes.clear()
        completedStrokes.addAll(newStrokes)
        invalidate()
    }

    fun clearCanvas() {
        completedStrokes.clear()
        activeStrokePoints.clear()
        invalidate()
        onStrokeFinished?.invoke(emptyList())
    }

    fun undoLastStroke() {
        if (completedStrokes.isNotEmpty()) {
            completedStrokes.removeAt(completedStrokes.size - 1)
            invalidate()
            onStrokeFinished?.invoke(completedStrokes.toList())
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = when (event.getToolType(0)) {
            MotionEvent.TOOL_TYPE_STYLUS -> ToolType.STYLUS
            MotionEvent.TOOL_TYPE_ERASER -> ToolType.ERASER
            else -> ToolType.FINGER
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activeStrokePoints = mutableListOf()
                activeStrokeStartTime = event.eventTime

                val pt = StrokePoint(
                    x = event.x,
                    y = event.y,
                    tMs = event.eventTime,
                    pressure = if (event.pressure > 0f) event.pressure else null,
                    tiltRad = null,
                    orientationRad = null
                )
                activeStrokePoints.add(pt)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val histSize = event.historySize
                for (h in 0 until histSize) {
                    val hx = event.getHistoricalX(h)
                    val hy = event.getHistoricalY(h)
                    val ht = event.getHistoricalEventTime(h)
                    val hp = event.getHistoricalPressure(h)
                    activeStrokePoints.add(
                        StrokePoint(
                            x = hx,
                            y = hy,
                            tMs = ht,
                            pressure = if (hp > 0f) hp else null,
                            tiltRad = null,
                            orientationRad = null
                        )
                    )
                }

                activeStrokePoints.add(
                    StrokePoint(
                        x = event.x,
                        y = event.y,
                        tMs = event.eventTime,
                        pressure = if (event.pressure > 0f) event.pressure else null,
                        tiltRad = null,
                        orientationRad = null
                    )
                )
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (activeStrokePoints.isNotEmpty()) {
                    val lastPt = activeStrokePoints.last()
                    if (lastPt.x != event.x || lastPt.y != event.y) {
                        activeStrokePoints.add(
                            StrokePoint(
                                x = event.x,
                                y = event.y,
                                tMs = event.eventTime,
                                pressure = if (event.pressure > 0f) event.pressure else null,
                                tiltRad = null,
                                orientationRad = null
                            )
                        )
                    }
                    val stroke = Stroke(
                        id = UUID.randomUUID().toString(),
                        tool = toolType,
                        points = activeStrokePoints.toList(),
                        startedAtMs = activeStrokeStartTime,
                        endedAtMs = event.eventTime,
                        isCancelled = false,
                        color = strokePaint.color,
                        baseWidthPx = strokePaint.strokeWidth
                    )
                    completedStrokes.add(stroke)
                    activeStrokePoints.clear()
                    invalidate()
                    onStrokeFinished?.invoke(completedStrokes.toList())
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                activeStrokePoints.clear()
                invalidate()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Desenha guias caligráficas da assinatura
        val baselineY = h * 0.65f
        val xHeightY = h * 0.45f
        val ascenderY = h * 0.22f
        val descenderY = h * 0.85f

        // Linha base principal contínua
        canvas.drawLine(w * 0.05f, baselineY, w * 0.95f, baselineY, baselinePaint)

        // Linhas auxiliares tracejadas
        guidelinePath.reset()
        guidelinePath.moveTo(w * 0.05f, xHeightY)
        guidelinePath.lineTo(w * 0.95f, xHeightY)

        guidelinePath.moveTo(w * 0.05f, ascenderY)
        guidelinePath.lineTo(w * 0.95f, ascenderY)

        guidelinePath.moveTo(w * 0.05f, descenderY)
        guidelinePath.lineTo(w * 0.95f, descenderY)

        canvas.drawPath(guidelinePath, guidelinePaint)

        // Elipse sutil delimitadora de floreios
        canvas.drawOval(
            w * 0.08f,
            h * 0.15f,
            w * 0.92f,
            h * 0.88f,
            flourishPaint
        )

        // 2. Desenha traços completados
        for (stroke in completedStrokes) {
            drawStroke(canvas, stroke.points, stroke.color ?: strokePaint.color, stroke.baseWidthPx ?: strokePaint.strokeWidth)
        }

        // 3. Desenha traço ativo em tempo real
        if (activeStrokePoints.isNotEmpty()) {
            drawStroke(canvas, activeStrokePoints, strokePaint.color, strokePaint.strokeWidth)
        }
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
