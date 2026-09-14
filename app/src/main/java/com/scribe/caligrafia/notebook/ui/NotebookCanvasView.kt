package com.scribe.caligrafia.notebook.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.gesture.EdgeGestureExclusionHelper
import com.scribe.caligrafia.ink.renderer.GuidelineRenderer
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer
import com.scribe.caligrafia.preferences.ScribePreferencesRuntime

/**
 * Superfície de escrita nativa para o Caderno de Caligrafia.
 * Preferências F6 de contraste/pressão alteram somente a renderização derivada, nunca raw strokes.
 */
@SuppressLint("ViewConstructor")
class NotebookCanvasView(
    context: Context,
    val pipeline: StrokeCapturePipeline,
    val strokeRepository: InMemoryStrokeRepository,
    val renderer: SmoothedReferenceRenderer = SmoothedReferenceRenderer(),
    var onStrokeChanged: (() -> Unit)? = null,
    var onActiveWriting: ((Long) -> Unit)? = null
) : View(context) {

    private val guidelineRenderer = GuidelineRenderer()
    var guidelineConfig: GuidelineConfig? = GuidelineConfig.copperplate()
        set(value) {
            field = value
            invalidate()
        }

    var pressureCurve: com.scribe.caligrafia.expansions.styles.PressureCurveType
        get() = renderer.pressureCurve
        set(value) {
            renderer.pressureCurve = value
            invalidate()
        }

    private var currentX = -1f
    private var currentY = -1f
    private var isHovering = false
    private var lastWritingEventTimeMs: Long? = null

    private val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(239, 68, 68)
    }

    private val eraserFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 239, 68, 68)
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        pipeline.onStrokePointAdded = { invalidate() }
        pipeline.onStrokeCompleted = { stroke ->
            if (stroke.tool != ToolType.ERASER) {
                strokeRepository.addStroke(stroke)
                onStrokeChanged?.invoke()
                invalidate()
            }
        }
        pipeline.onEraserPointsAdded = { points ->
            val erased = strokeRepository.eraseStrokesIntersecting(
                eraserPoints = points,
                eraserRadius = pipeline.toolConfig.eraserRadiusPx
            )
            if (erased.isNotEmpty()) onStrokeChanged?.invoke()
            invalidate()
        }
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        currentX = event.x
        currentY = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> isHovering = true
            MotionEvent.ACTION_HOVER_EXIT -> isHovering = false
        }

        pipeline.onHoverEvent(event)
        invalidate()
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateSystemGestureExclusion()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateSystemGestureExclusion()
    }

    private fun updateSystemGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rects = EdgeGestureExclusionHelper.computeEdgeExclusionRects(
                width = width,
                height = height,
                density = resources.displayMetrics.density
            )
            if (rects.isNotEmpty()) ViewCompat.setSystemGestureExclusionRects(this, rects)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        currentX = event.x
        currentY = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }

        val consumed = pipeline.onMotionEvent(event)
        if (consumed) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> lastWritingEventTimeMs = event.eventTime
                MotionEvent.ACTION_MOVE -> {
                    val previous = lastWritingEventTimeMs
                    if (previous != null) onActiveWriting?.invoke((event.eventTime - previous).coerceIn(0L, 1_000L))
                    lastWritingEventTimeMs = event.eventTime
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> lastWritingEventTimeMs = null
            }
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            lastWritingEventTimeMs = null
        }
        invalidate()
        return consumed || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Preferência de pressão é derivada no renderer; os pontos brutos permanecem imutáveis.
        renderer.pressureCurve = ScribePreferencesRuntime.current.pressureCurve

        canvas.drawColor(Color.rgb(253, 252, 248))

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        guidelineRenderer.draw(canvas, w, h, guidelineConfig)

        for (stroke in strokeRepository.allStrokes) {
            renderer.renderStroke(canvas, stroke)
        }

        pipeline.getActiveStrokePreview()?.let { renderer.renderActiveStroke(canvas, it) }

        val isEraserMode = pipeline.toolConfig.mode == ToolMode.ERASER ||
            pipeline.currentActiveTool == ToolType.ERASER

        if (isEraserMode && (isHovering || pipeline.isCapturing) && currentX >= 0f && currentY >= 0f) {
            val radius = pipeline.toolConfig.eraserRadiusPx
            canvas.drawCircle(currentX, currentY, radius, eraserFillPaint)
            canvas.drawCircle(currentX, currentY, radius, eraserCursorPaint)
        }
    }

    fun requestRedraw() {
        invalidate()
    }
}
