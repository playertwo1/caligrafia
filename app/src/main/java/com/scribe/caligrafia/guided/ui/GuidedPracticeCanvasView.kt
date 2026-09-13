package com.scribe.caligrafia.guided.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import com.scribe.caligrafia.core.model.GuidelineBand
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.gesture.EdgeGestureExclusionHelper
import com.scribe.caligrafia.ink.renderer.GuidelineRenderer
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import com.scribe.caligrafia.guided.model.ReferenceGlyph

/**
 * Superfície de escrita nativa especializada para Treino Guiado (M2).
 *
 * Características:
 * 1. Projeção de pautas caligráficas clássicas (Copperplate 52° / Spencerian 68°).
 * 2. Renderização do ReferenceGlyph no modo adequado (Trace, Copiar, Sozinho).
 * 3. Controle dinâmico de Ghost Mode (100% a 0%).
 * 4. Captura com baixa latência, rejeição de palma e bloqueio de gestos laterais (estilo Samsung Notes).
 */
@SuppressLint("ViewConstructor")
class GuidedPracticeCanvasView(
    context: Context,
    val pipeline: StrokeCapturePipeline,
    val strokeRepository: InMemoryStrokeRepository,
    val renderer: SmoothedReferenceRenderer = SmoothedReferenceRenderer(),
    var onStrokeChanged: (() -> Unit)? = null
) : View(context) {

    private val guidelineRenderer = GuidelineRenderer()
    private val glyphRenderer = ReferenceGlyphRenderer()

    var guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate(xHeightPx = 65f)
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

    var currentGlyph: ReferenceGlyph? = null
        set(value) {
            field = value
            invalidate()
        }

    var currentStage: PracticeStage = PracticeStage.TRACE
        set(value) {
            field = value
            invalidate()
        }

    var ghostLevel: GhostModeLevel = GhostModeLevel.CLEAR
        set(value) {
            field = value
            invalidate()
        }

    private val modelBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.rgb(180, 195, 220)
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    private val modelTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 116, 139)
        textSize = 22f
        isFakeBoldText = true
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
            if (erased.isNotEmpty()) {
                onStrokeChanged?.invoke()
            }
            invalidate()
        }
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        val consumed = pipeline.onMotionEvent(event)
        invalidate()
        return consumed || super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        pipeline.onHoverEvent(event)
        invalidate()
        return true
    }

    /**
     * Retorna os parâmetros de posicionamento do glifo e da faixa ativa no canvas.
     */
    fun getActiveBandAndOrigin(): Triple<GuidelineBand, Float, Float>? {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return null

        val bands = guidelineConfig.computeBands(h)
        val activeBand = bands.firstOrNull() ?: return null
        val glyph = currentGlyph ?: return null
        val glyphWidthPx = activeBand.xHeight * glyph.widthToXHeightRatio

        val originX = when (currentStage) {
            PracticeStage.TRACE -> (w - glyphWidthPx) / 2f
            PracticeStage.COPY -> w * 0.55f
            PracticeStage.SOLO -> (w - glyphWidthPx) / 2f
        }

        return Triple(activeBand, originX, glyphWidthPx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Papel Suave de Caligrafia
        canvas.drawColor(Color.rgb(253, 252, 248))

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 2. Pautas Caligráficas
        guidelineRenderer.draw(canvas, w, h, guidelineConfig)

        val bands = guidelineConfig.computeBands(h)
        val activeBand = bands.firstOrNull()
        val glyph = currentGlyph

        // 3. Renderização Pedagógica do Glifo de Referência
        if (activeBand != null && glyph != null) {
            val glyphWidthPx = activeBand.xHeight * glyph.widthToXHeightRatio

            when (currentStage) {
                PracticeStage.TRACE -> {
                    // Centralizado diretamente na área de escrita com Ghost Mode ativo
                    val originX = (w - glyphWidthPx) / 2f
                    glyphRenderer.render(
                        canvas = canvas,
                        glyph = glyph,
                        band = activeBand,
                        originX = originX,
                        glyphWidthPx = glyphWidthPx,
                        ghostLevel = ghostLevel,
                        showDirectionalHints = true
                    )
                }
                PracticeStage.COPY -> {
                    // Modelo fixo à esquerda como gabarito (opaco)
                    val modelOriginX = 70f
                    val modelBox = RectF(
                        modelOriginX - 15f,
                        activeBand.ascenderY - 10f,
                        modelOriginX + glyphWidthPx + 15f,
                        activeBand.descenderY + 10f
                    )
                    canvas.drawRoundRect(modelBox, 8f, 8f, modelBoxPaint)
                    canvas.drawText("MODELO", modelOriginX, activeBand.ascenderY - 18f, modelTagPaint)

                    glyphRenderer.render(
                        canvas = canvas,
                        glyph = glyph,
                        band = activeBand,
                        originX = modelOriginX,
                        glyphWidthPx = glyphWidthPx,
                        ghostLevel = GhostModeLevel.FULL,
                        showDirectionalHints = true
                    )
                }
                PracticeStage.SOLO -> {
                    // Sem modelo na linha de escrita
                }
            }
        }

        // 4. Traços do Aluno
        for (stroke in strokeRepository.allStrokes) {
            renderer.renderStroke(canvas, stroke)
        }

        // 5. Traço Ativo em Andamento
        val activeStroke = pipeline.getActiveStrokePreview()
        if (activeStroke != null) {
            renderer.renderActiveStroke(canvas, activeStroke)
        }
    }
}
