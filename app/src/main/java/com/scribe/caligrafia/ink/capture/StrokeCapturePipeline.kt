package com.scribe.caligrafia.ink.capture

import android.view.MotionEvent
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.palm.PalmDecision
import com.scribe.caligrafia.ink.palm.PalmRejectionPolicy
import java.util.UUID

/**
 * Pipeline de ingestão de eventos para captura vetorial de alta fidelidade e controle de ferramentas.
 *
 * Garante:
 * 1. Consumo integral de historical samples em ordem cronológica estrita.
 * 2. Preservação de coordenadas x/y, timestamps, pressão, tilt e orientação.
 * 3. Ausência de dados fictícios (sensores não disponíveis ficam null).
 * 4. Isolamento do traço ativo por pointerId para evitar corrupção por multi-touch espúrio.
 * 5. Rejeição estrita de palma (Palm Rejection) e modo Stylus-Only via PalmRejectionPolicy.
 * 6. Roteamento transparente de borracha (TOOL_TYPE_ERASER e botão lateral S Pen).
 * 7. Tratamento de ACTION_CANCEL sem deixar estado inconsistente.
 * 8. Núcleo 100% testável independente do runtime do Android Framework.
 */
class StrokeCapturePipeline(
    val palmPolicy: PalmRejectionPolicy = PalmRejectionPolicy(),
    var toolConfig: ToolConfig = ToolConfig(),
    var onStrokeCompleted: ((Stroke) -> Unit)? = null,
    var onStrokeCancelled: ((Stroke) -> Unit)? = null,
    var onStrokePointAdded: ((StrokePoint) -> Unit)? = null,
    var onEraserPointsAdded: ((List<StrokePoint>) -> Unit)? = null,
    var onPalmTouchRejected: ((ToolType, String) -> Unit)? = null
) {

    private var activeStrokeId: String? = null
    private var activePointerId: Int = -1
    private var activeTool: ToolType = ToolType.UNKNOWN
    private var activeStartedAtMs: Long = 0L
    private val activePoints = mutableListOf<StrokePoint>()
    private var lastEraserPoint: StrokePoint? = null

    var totalHistoricalSamplesAbsorbed: Long = 0L
        private set

    var totalPointsCaptured: Long = 0L
        private set

    val isCapturing: Boolean
        get() = activeStrokeId != null

    val currentActiveTool: ToolType
        get() = activeTool

    fun getActiveStrokePreview(): Stroke? {
        val id = activeStrokeId ?: return null
        if (activeTool == ToolType.ERASER) return null // A04: Borracha não produz prévia de tinta
        return Stroke(
            id = id,
            tool = activeTool,
            points = activePoints.toList(),
            startedAtMs = activeStartedAtMs,
            endedAtMs = activePoints.lastOrNull()?.tMs ?: activeStartedAtMs,
            isCancelled = false,
            color = if (activeTool == ToolType.STYLUS) toolConfig.activeColorArgb else null,
            baseWidthPx = if (activeTool == ToolType.STYLUS) toolConfig.activeStrokeWidthPx else null
        )
    }

    /**
     * Processa um MotionEvent recebido da superfície de escrita do Android.
     * @return true se o evento foi consumido com sucesso pelo pipeline.
     */
    fun onMotionEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handleActionPointerDown(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_POINTER_UP -> handleActionPointerUp(event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel(event)
            else -> false
        }
    }

    /**
     * Processa eventos de proximidade (hover) da S Pen / Stylus.
     */
    fun onHoverEvent(event: MotionEvent): Boolean {
        val tool = ToolType.fromMotionEvent(event.getToolType(0), event.buttonState)
        val eventTime = event.eventTime

        if (tool == ToolType.STYLUS || tool == ToolType.ERASER) {
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> palmPolicy.onStylusHoverEnter(eventTime)
                MotionEvent.ACTION_HOVER_MOVE -> palmPolicy.onStylusHoverMove(eventTime)
                MotionEvent.ACTION_HOVER_EXIT -> palmPolicy.onStylusHoverExit(eventTime)
            }
            return true
        }
        return false
    }

    /**
     * Início de um traço desacoplado do MotionEvent (testável diretamente).
     */
    fun onPointerDown(
        pointerId: Int,
        toolType: ToolType,
        point: StrokePoint,
        eventTime: Long = point.tMs
    ): Boolean {
        // Se a ferramenta na UI estiver definida como Borracha e for Stylus, redireciona para ERASER
        val effectiveTool = if (toolType == ToolType.STYLUS && toolConfig.mode == ToolMode.ERASER) {
            ToolType.ERASER
        } else {
            toolType
        }

        // 1. Avaliação pelas regras de Palm Rejection e modo de entrada
        when (val decision = palmPolicy.evaluateTouch(effectiveTool, eventTime)) {
            is PalmDecision.Reject -> {
                onPalmTouchRejected?.invoke(effectiveTool, decision.reason)
                return false
            }
            is PalmDecision.Allow -> {
                // Admitido para escrita
            }
        }

        // Se a caneta acabou de tocar enquanto havia um traço de dedo ativo, preempção imediata
        if (activeStrokeId != null) {
            terminateActiveStroke(isCancelled = true, eventTime = eventTime)
        }

        if (effectiveTool == ToolType.STYLUS || effectiveTool == ToolType.ERASER) {
            palmPolicy.onStylusDown(eventTime)
        }

        activePointerId = pointerId
        activeStrokeId = UUID.randomUUID().toString()
        activeTool = effectiveTool
        activeStartedAtMs = eventTime
        activePoints.clear()

        activePoints.add(point)
        totalPointsCaptured++
        onStrokePointAdded?.invoke(point)

        if (activeTool == ToolType.ERASER) {
            lastEraserPoint = point
            onEraserPointsAdded?.invoke(listOf(point))
        }

        return true
    }

    /**
     * Adiciona uma sequência de pontos de movimento (incluindo amostras históricas).
     */
    fun onPointerMove(
        pointerId: Int,
        points: List<StrokePoint>,
        historicalCount: Int = 0
    ): Boolean {
        if (activeStrokeId == null || activePointerId != pointerId) return false
        if (points.isEmpty()) return false

        activePoints.addAll(points)
        totalHistoricalSamplesAbsorbed += historicalCount
        totalPointsCaptured += points.size
        points.forEach { onStrokePointAdded?.invoke(it) }

        if (activeTool == ToolType.ERASER) {
            // A05: Conecta o último ponto do evento anterior ao lote atual para não perder interseções
            val eraserBatch = if (lastEraserPoint != null) {
                listOf(lastEraserPoint!!) + points
            } else {
                points
            }
            lastEraserPoint = points.lastOrNull() ?: lastEraserPoint
            onEraserPointsAdded?.invoke(eraserBatch)
        }

        return true
    }

    /**
     * Finaliza o traço atual com sucesso.
     */
    fun onPointerUp(
        pointerId: Int,
        finalPoints: List<StrokePoint> = emptyList(),
        eventTime: Long
    ): Boolean {
        if (activeStrokeId == null || activePointerId != pointerId) return false

        if (finalPoints.isNotEmpty()) {
            activePoints.addAll(finalPoints)
            totalPointsCaptured += finalPoints.size
            finalPoints.forEach { onStrokePointAdded?.invoke(it) }
            if (activeTool == ToolType.ERASER) {
                val eraserBatch = if (lastEraserPoint != null) {
                    listOf(lastEraserPoint!!) + finalPoints
                } else {
                    finalPoints
                }
                onEraserPointsAdded?.invoke(eraserBatch)
            }
        }
        lastEraserPoint = null

        if (activeTool == ToolType.STYLUS || activeTool == ToolType.ERASER) {
            palmPolicy.onStylusUp(eventTime)
        }

        terminateActiveStroke(isCancelled = false, eventTime = eventTime)
        return true
    }

    /**
     * Cancela o traço atual devido a interrupção do sistema operacional.
     */
    fun onPointerCancel(
        pointerId: Int = activePointerId,
        eventTime: Long
    ): Boolean {
        if (activeStrokeId == null) return false

        if (activeTool == ToolType.STYLUS || activeTool == ToolType.ERASER) {
            palmPolicy.onStylusCancel(eventTime)
        }

        terminateActiveStroke(isCancelled = true, eventTime = eventTime)
        return true
    }

    /**
     * Efetua a descarga de segurança (flush) de qualquer traço ativo em andamento.
     * Invocado quando o app perde o foco de janela (ex: notificação, diálogo, split-screen resize),
     * quando a S Pen é guardada no silo físico, ou ao entrar em segundo plano (onPause/bloqueio de tela).
     *
     * @param commitIfValid se true e o traço tiver >= 2 pontos, finaliza-o como concluído para não descartar a escrita do usuário.
     *                      se false ou tiver < 2 pontos, cancela-o com segurança.
     * @return true se havia um traço ativo e ele foi descarregado.
     */
    fun flushActiveStroke(commitIfValid: Boolean = true): Boolean {
        if (activeStrokeId == null) return false

        val eventTime = activePoints.lastOrNull()?.tMs ?: activeStartedAtMs

        if (activeTool == ToolType.STYLUS || activeTool == ToolType.ERASER) {
            palmPolicy.onStylusUp(eventTime)
        }

        val shouldCommit = commitIfValid && activePoints.size >= 2
        terminateActiveStroke(isCancelled = !shouldCommit, eventTime = eventTime)
        return true
    }

    private fun handleActionDown(event: MotionEvent): Boolean {
        val pointerIndex = 0
        val pointerId = event.getPointerId(pointerIndex)
        val tool = ToolType.fromMotionEvent(event.getToolType(pointerIndex), event.buttonState)

        val firstPoint = createPoint(
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
            tMs = event.eventTime,
            pressure = event.getPressure(pointerIndex),
            tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
            orientation = event.getOrientation(pointerIndex)
        )
        return onPointerDown(pointerId, tool, firstPoint, event.eventTime)
    }

    private fun handleActionPointerDown(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val tool = ToolType.fromMotionEvent(event.getToolType(actionIndex), event.buttonState)

        // Se uma caneta tocar a tela enquanto havia outro ponteiro (ou multi-touch),
        // o stylus toma posse do traço
        if (tool == ToolType.STYLUS || tool == ToolType.ERASER) {
            val pointerId = event.getPointerId(actionIndex)
            val point = createPoint(
                x = event.getX(actionIndex),
                y = event.getY(actionIndex),
                tMs = event.eventTime,
                pressure = event.getPressure(actionIndex),
                tilt = event.getAxisValue(MotionEvent.AXIS_TILT, actionIndex),
                orientation = event.getOrientation(actionIndex)
            )
            return onPointerDown(pointerId, tool, point, event.eventTime)
        }

        // Caso seja um dedo secundário (ex: palma repousando), rejeita
        when (val decision = palmPolicy.evaluateTouch(tool, event.eventTime)) {
            is PalmDecision.Reject -> {
                onPalmTouchRejected?.invoke(tool, decision.reason)
            }
            is PalmDecision.Allow -> {}
        }
        return false
    }

    private fun handleActionMove(event: MotionEvent): Boolean {
        if (activeStrokeId == null) return false

        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) return false

        val points = mutableListOf<StrokePoint>()
        val historySize = event.historySize

        // 1. Consumir todas as amostras históricas geradas entre frames em ordem cronológica
        for (h in 0 until historySize) {
            points.add(
                createPoint(
                    x = event.getHistoricalX(pointerIndex, h),
                    y = event.getHistoricalY(pointerIndex, h),
                    tMs = event.getHistoricalEventTime(h),
                    pressure = event.getHistoricalPressure(pointerIndex, h),
                    tilt = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, h),
                    orientation = event.getHistoricalOrientation(pointerIndex, h)
                )
            )
        }

        // 2. Adicionar o ponto atual do evento
        points.add(
            createPoint(
                x = event.getX(pointerIndex),
                y = event.getY(pointerIndex),
                tMs = event.eventTime,
                pressure = event.getPressure(pointerIndex),
                tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
                orientation = event.getOrientation(pointerIndex)
            )
        )

        return onPointerMove(activePointerId, points, historicalCount = historySize)
    }

    private fun handleActionUp(event: MotionEvent): Boolean {
        if (activeStrokeId == null) return false

        val pointerIndex = event.findPointerIndex(activePointerId)
        val finalPoints = mutableListOf<StrokePoint>()

        if (pointerIndex >= 0) {
            val historySize = event.historySize
            for (h in 0 until historySize) {
                finalPoints.add(
                    createPoint(
                        x = event.getHistoricalX(pointerIndex, h),
                        y = event.getHistoricalY(pointerIndex, h),
                        tMs = event.getHistoricalEventTime(h),
                        pressure = event.getHistoricalPressure(pointerIndex, h),
                        tilt = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, h),
                        orientation = event.getHistoricalOrientation(pointerIndex, h)
                    )
                )
            }

            finalPoints.add(
                createPoint(
                    x = event.getX(pointerIndex),
                    y = event.getY(pointerIndex),
                    tMs = event.eventTime,
                    pressure = event.getPressure(pointerIndex),
                    tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
                    orientation = event.getOrientation(pointerIndex)
                )
            )
        }

        return onPointerUp(activePointerId, finalPoints, event.eventTime)
    }

    private fun handleActionPointerUp(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        if (pointerId == activePointerId) {
            val finalPoints = mutableListOf<StrokePoint>()
            val historySize = event.historySize
            for (h in 0 until historySize) {
                finalPoints.add(
                    createPoint(
                        x = event.getHistoricalX(actionIndex, h),
                        y = event.getHistoricalY(actionIndex, h),
                        tMs = event.getHistoricalEventTime(h),
                        pressure = event.getHistoricalPressure(actionIndex, h),
                        tilt = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, actionIndex, h),
                        orientation = event.getHistoricalOrientation(actionIndex, h)
                    )
                )
            }
            finalPoints.add(
                createPoint(
                    x = event.getX(actionIndex),
                    y = event.getY(actionIndex),
                    tMs = event.eventTime,
                    pressure = event.getPressure(actionIndex),
                    tilt = event.getAxisValue(MotionEvent.AXIS_TILT, actionIndex),
                    orientation = event.getOrientation(actionIndex)
                )
            )
            return onPointerUp(pointerId, finalPoints, event.eventTime)
        }
        return false
    }

    private fun handleActionCancel(event: MotionEvent): Boolean {
        lastEraserPoint = null
        return onPointerCancel(activePointerId, event.eventTime)
    }

    private fun terminateActiveStroke(isCancelled: Boolean, eventTime: Long) {
        val id = activeStrokeId ?: return
        val completedStroke = Stroke(
            id = id,
            tool = activeTool,
            points = activePoints.toList(),
            startedAtMs = activeStartedAtMs,
            endedAtMs = eventTime,
            isCancelled = isCancelled,
            color = if (activeTool == ToolType.STYLUS) toolConfig.activeColorArgb else null,
            baseWidthPx = if (activeTool == ToolType.STYLUS) toolConfig.activeStrokeWidthPx else null
        )

        activeStrokeId = null
        activePointerId = -1
        activeTool = ToolType.UNKNOWN
        activeStartedAtMs = 0L
        activePoints.clear()
        lastEraserPoint = null

        if (isCancelled) {
            onStrokeCancelled?.invoke(completedStroke)
        } else {
            // A04: Ações de borracha nunca são adicionadas ao histórico de traços de tinta
            if (completedStroke.tool != ToolType.ERASER) {
                onStrokeCompleted?.invoke(completedStroke)
            }
        }
    }

    private fun createPoint(
        x: Float,
        y: Float,
        tMs: Long,
        pressure: Float,
        tilt: Float,
        orientation: Float
    ): StrokePoint {
        return StrokePoint(
            x = x,
            y = y,
            tMs = tMs,
            pressure = if (!pressure.isNaN() && pressure >= 0f) pressure else null,
            tiltRad = if (!tilt.isNaN()) tilt else null,
            orientationRad = if (!orientation.isNaN()) orientation else null
        )
    }

    fun resetMetrics() {
        totalHistoricalSamplesAbsorbed = 0L
        totalPointsCaptured = 0L
        palmPolicy.resetMetrics()
    }
}
