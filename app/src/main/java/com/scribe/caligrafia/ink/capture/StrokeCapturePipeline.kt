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
 * 5. Rejeição estrita de palma e política de entrada persistida via PalmRejectionPolicy.
 * 6. Roteamento transparente de borracha (TOOL_TYPE_ERASER e botão lateral S Pen).
 * 7. Tratamento de ACTION_CANCEL sem deixar estado inconsistente.
 */
class StrokeCapturePipeline(
    val palmPolicy: PalmRejectionPolicy = PalmRejectionPolicy(followRuntimeInputMode = true),
    var toolConfig: ToolConfig = ToolConfig(),
    var onStrokeStarted: ((StrokePoint) -> Unit)? = null,
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
        if (activeTool == ToolType.ERASER) return null
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

    fun onPointerDown(
        pointerId: Int,
        toolType: ToolType,
        point: StrokePoint,
        eventTime: Long = point.tMs
    ): Boolean {
        val effectiveTool = if (toolType == ToolType.STYLUS && toolConfig.mode == ToolMode.ERASER) {
            ToolType.ERASER
        } else {
            toolType
        }

        when (val decision = palmPolicy.evaluateTouch(effectiveTool, eventTime)) {
            is PalmDecision.Reject -> {
                onPalmTouchRejected?.invoke(effectiveTool, decision.reason)
                return false
            }
            is PalmDecision.Allow -> Unit
        }

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
        onStrokeStarted?.invoke(point)
        onStrokePointAdded?.invoke(point)

        if (activeTool == ToolType.ERASER) {
            lastEraserPoint = point
            onEraserPointsAdded?.invoke(listOf(point))
        }

        return true
    }

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
        val firstPoint = extractPoint(event, pointerIndex)
        return onPointerDown(pointerId, tool, firstPoint, event.eventTime)
    }

    private fun handleActionPointerDown(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val tool = ToolType.fromMotionEvent(event.getToolType(actionIndex), event.buttonState)

        if (tool == ToolType.STYLUS || tool == ToolType.ERASER) {
            val pointerId = event.getPointerId(actionIndex)
            val point = extractPoint(event, actionIndex)
            return onPointerDown(pointerId, tool, point, event.eventTime)
        }

        when (val decision = palmPolicy.evaluateTouch(tool, event.eventTime)) {
            is PalmDecision.Reject -> onPalmTouchRejected?.invoke(tool, decision.reason)
            is PalmDecision.Allow -> Unit
        }
        return false
    }

    private fun handleActionMove(event: MotionEvent): Boolean {
        if (activeStrokeId == null) return false

        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) return false

        val points = mutableListOf<StrokePoint>()
        val historySize = event.historySize
        for (h in 0 until historySize) {
            points.add(extractPoint(event, pointerIndex, historicalIndex = h))
        }
        points.add(extractPoint(event, pointerIndex))
        return onPointerMove(activePointerId, points, historicalCount = historySize)
    }

    private fun handleActionUp(event: MotionEvent): Boolean {
        if (activeStrokeId == null) return false

        val pointerIndex = event.findPointerIndex(activePointerId)
        val finalPoints = mutableListOf<StrokePoint>()

        if (pointerIndex >= 0) {
            val historySize = event.historySize
            for (h in 0 until historySize) {
                finalPoints.add(extractPoint(event, pointerIndex, historicalIndex = h))
            }
            finalPoints.add(extractPoint(event, pointerIndex))
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
                finalPoints.add(extractPoint(event, actionIndex, historicalIndex = h))
            }
            finalPoints.add(extractPoint(event, actionIndex))
            return onPointerUp(pointerId, finalPoints, event.eventTime)
        }
        return false
    }

    private fun handleActionCancel(event: MotionEvent): Boolean {
        lastEraserPoint = null
        return onPointerCancel(activePointerId, event.eventTime)
    }

    private fun terminateActiveStroke(isCancelled: Boolean, eventTime: Long) {
        val strokeId = activeStrokeId ?: return
        val currentTool = activeTool
        activeStrokeId = null
        activePointerId = -1
        activeTool = ToolType.UNKNOWN
        val startedAt = activeStartedAtMs
        activeStartedAtMs = 0L
        lastEraserPoint = null

        val finalPoints = activePoints.toList()
        activePoints.clear()

        val completedStroke = Stroke(
            id = strokeId,
            tool = currentTool,
            points = finalPoints,
            startedAtMs = startedAt,
            endedAtMs = eventTime,
            isCancelled = isCancelled,
            color = if (currentTool == ToolType.STYLUS) toolConfig.activeColorArgb else null,
            baseWidthPx = if (currentTool == ToolType.STYLUS) toolConfig.activeStrokeWidthPx else null
        )

        if (isCancelled) {
            onStrokeCancelled?.invoke(completedStroke)
        } else if (completedStroke.tool != ToolType.ERASER) {
            onStrokeCompleted?.invoke(completedStroke)
        }
    }

    private fun extractPoint(
        event: MotionEvent,
        pointerIndex: Int,
        historicalIndex: Int? = null
    ): StrokePoint {
        val hasPressure = hasAxis(event, MotionEvent.AXIS_PRESSURE)
        val hasTilt = hasAxis(event, MotionEvent.AXIS_TILT)
        val hasOrientation = hasAxis(event, MotionEvent.AXIS_ORIENTATION)

        val x = if (historicalIndex != null) event.getHistoricalX(pointerIndex, historicalIndex) else event.getX(pointerIndex)
        val y = if (historicalIndex != null) event.getHistoricalY(pointerIndex, historicalIndex) else event.getY(pointerIndex)
        val tMs = if (historicalIndex != null) event.getHistoricalEventTime(historicalIndex) else event.eventTime
        val rawPressure = if (historicalIndex != null) event.getHistoricalPressure(pointerIndex, historicalIndex) else event.getPressure(pointerIndex)
        val rawTilt = if (historicalIndex != null) event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historicalIndex) else event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        val rawOrientation = if (historicalIndex != null) event.getHistoricalOrientation(pointerIndex, historicalIndex) else event.getOrientation(pointerIndex)

        val pressure = when {
            !hasPressure -> null
            rawPressure.isNaN() || rawPressure < 0f -> null
            else -> rawPressure
        }
        val tilt = when {
            !hasTilt -> null
            rawTilt.isNaN() -> null
            else -> rawTilt
        }
        val orientation = when {
            !hasOrientation -> null
            rawOrientation.isNaN() -> null
            else -> rawOrientation
        }

        return StrokePoint(
            x = x,
            y = y,
            tMs = tMs,
            pressure = pressure,
            tiltRad = tilt,
            orientationRad = orientation
        )
    }

    private fun hasAxis(event: MotionEvent, axis: Int): Boolean {
        val dev = try { event.device } catch (_: Throwable) { null }
        return if (dev != null) {
            dev.getMotionRange(axis) != null
        } else {
            true
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
