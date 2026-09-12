package com.scribe.caligrafia.notebook.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.os.Build
import androidx.core.view.ViewCompat
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.gesture.EdgeGestureExclusionHelper
import com.scribe.caligrafia.ink.renderer.GuidelineRenderer
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer

/**
 * Superfície de escrita nativa para o Caderno de Caligrafia (M1).
 *
 * Características:
 * 1. Fundo com textura/cor de papel suave para caligrafia.
 * 2. Pautas caligráficas renderizadas no plano de fundo (GuidelineRenderer).
 * 3. Ingestão de alta precisão via MotionEvent histórico e rejeição de palma.
 * 4. Renderização em tempo real de traços concluídos e do traço ativo em movimento.
 * 5. Cursor visual para feedback quando em modo de borracha.
 */
@SuppressLint("ViewConstructor")
class NotebookCanvasView(
    context: Context,
    val pipeline: StrokeCapturePipeline,
    val strokeRepository: InMemoryStrokeRepository,
    val renderer: SmoothedReferenceRenderer = SmoothedReferenceRenderer(),
    var onStrokeChanged: (() -> Unit)? = null
) : View(context) {

    private val guidelineRenderer = GuidelineRenderer()
    var guidelineConfig: GuidelineConfig? = GuidelineConfig.copperplate()
        set(value) {
            field = value
            invalidate()
        }

    private var currentX = -1f
    private var currentY = -1f
    private var isHovering = false

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

        // Callbacks de atualização de tela quando o pipeline captura pontos
        pipeline.onStrokePointAdded = {
            invalidate()
        }
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
            if (erased.isNotEmpty()) {
                onStrokeChanged?.invoke()
            }
            invalidate()
        }
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        currentX = event.x
        currentY = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                isHovering = true
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                isHovering = false
            }
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
            if (rects.isNotEmpty()) {
                ViewCompat.setSystemGestureExclusionRects(this, rects)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        currentX = event.x
        currentY = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Impede que contêineres pais (Scaffold, Column) furtem toques na borda
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        val consumed = pipeline.onMotionEvent(event)
        invalidate()
        return consumed || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Fundo de Papel Caligráfico (Ivory / Off-White suave)
        canvas.drawColor(Color.rgb(253, 252, 248))

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 2. Renderizar Pautas Caligráficas (GuidelineRenderer)
        guidelineRenderer.draw(canvas, w, h, guidelineConfig)

        // 3. Renderizar Traços Vetoriais Concluídos
        for (stroke in strokeRepository.allStrokes) {
            renderer.renderStroke(canvas, stroke)
        }

        // 4. Renderizar Traço Ativo em Andamento
        val activeStroke = pipeline.getActiveStrokePreview()
        if (activeStroke != null) {
            renderer.renderActiveStroke(canvas, activeStroke)
        }

        // 5. Cursor visual da Borracha
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
