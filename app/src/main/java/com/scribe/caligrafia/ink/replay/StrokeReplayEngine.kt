package com.scribe.caligrafia.ink.replay

import com.scribe.caligrafia.core.model.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Motor de reprodução temporal vetorial (Replay).
 *
 * Princípios:
 * 1. Reconstrução vetorial contínua a partir dos deltas reais de tempo:
 *    tRelativo = ponto.tMs - inicioSessao.
 * 2. Suporte a velocidades 0.5x, 1.0x e 2.0x.
 * 3. Funções de interpolação puras e determinísticas para testes na JVM.
 * 4. Loop de animação via Coroutines para 60-120fps fluído em telas ProMotion/120Hz (Galaxy S25 Ultra).
 */
class StrokeReplayEngine(
    strokes: List<Stroke> = emptyList()
) {

    private val sessionStrokes = mutableListOf<Stroke>()
    private var baseTimeMs: Long = 0L
    private var totalDurationMs: Long = 0L

    private var currentPositionMs: Long = 0L
    private var speed: ReplaySpeed = ReplaySpeed.NORMAL
    private var status: ReplayStatus = ReplayStatus.IDLE

    private val _frameFlow = MutableStateFlow(ReplayFrame())
    val frameFlow: StateFlow<ReplayFrame> = _frameFlow.asStateFlow()

    private var playbackJob: Job? = null

    init {
        loadStrokes(strokes)
    }

    /**
     * Carrega uma nova lista de traços para reprodução e reseta o cursor para o início.
     */
    fun loadStrokes(strokes: List<Stroke>) {
        stop()
        sessionStrokes.clear()
        sessionStrokes.addAll(strokes.sortedBy { it.startedAtMs })

        if (sessionStrokes.isNotEmpty()) {
            baseTimeMs = sessionStrokes.first().startedAtMs
            val maxEnded = sessionStrokes.maxOf { it.endedAtMs }
            totalDurationMs = (maxEnded - baseTimeMs).coerceAtLeast(0L)
        } else {
            baseTimeMs = 0L
            totalDurationMs = 0L
        }

        currentPositionMs = 0L
        status = ReplayStatus.IDLE
        updateFrame()
    }

    fun setSpeed(newSpeed: ReplaySpeed) {
        speed = newSpeed
        updateFrame()
    }

    /**
     * Inicia ou retoma a reprodução temporal.
     */
    fun play(scope: CoroutineScope) {
        if (sessionStrokes.isEmpty()) return

        if (status == ReplayStatus.COMPLETED || currentPositionMs >= totalDurationMs) {
            currentPositionMs = 0L
        }

        status = ReplayStatus.PLAYING
        playbackJob?.cancel()

        playbackJob = scope.launch {
            var lastRealTime = System.currentTimeMillis()
            val frameIntervalMs = 16L // ~60fps para o loop de tick

            while (isActive && status == ReplayStatus.PLAYING) {
                delay(frameIntervalMs)
                val now = System.currentTimeMillis()
                val deltaReal = (now - lastRealTime).coerceAtLeast(0L)
                lastRealTime = now

                val deltaVirtual = (deltaReal * speed.multiplier).toLong()
                currentPositionMs += deltaVirtual

                if (currentPositionMs >= totalDurationMs) {
                    currentPositionMs = totalDurationMs
                    status = ReplayStatus.COMPLETED
                    updateFrame()
                    break
                } else {
                    updateFrame()
                }
            }
        }
    }

    fun pause() {
        if (status == ReplayStatus.PLAYING) {
            status = ReplayStatus.PAUSED
            playbackJob?.cancel()
            updateFrame()
        }
    }

    fun resume(scope: CoroutineScope) {
        if (status == ReplayStatus.PAUSED) {
            play(scope)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        status = ReplayStatus.IDLE
        currentPositionMs = 0L
        updateFrame()
    }

    /**
     * Posiciona a cabeça de reprodução em uma fração percentual da linha do tempo (0.0 a 1.0).
     */
    fun seekTo(progressFraction: Float) {
        if (totalDurationMs <= 0L) return
        val clampedFraction = progressFraction.coerceIn(0f, 1f)
        currentPositionMs = (totalDurationMs * clampedFraction).toLong()

        if (currentPositionMs >= totalDurationMs) {
            status = ReplayStatus.COMPLETED
        } else if (status == ReplayStatus.COMPLETED) {
            status = ReplayStatus.PAUSED
        }
        updateFrame()
    }

    /**
     * Calcula o fotograma exato para um determinado timestamp relativo da sessão.
     * Função pura e determinística.
     */
    fun computeFrameAt(elapsedMs: Long): ReplayFrame {
        if (sessionStrokes.isEmpty() || totalDurationMs <= 0L) {
            return ReplayFrame(status = status, speed = speed)
        }

        val clampedElapsed = elapsedMs.coerceIn(0L, totalDurationMs)
        val targetAbsoluteTime = baseTimeMs + clampedElapsed

        val completed = mutableListOf<Stroke>()
        var active: Stroke? = null

        for (stroke in sessionStrokes) {
            if (stroke.endedAtMs <= targetAbsoluteTime) {
                // Traço já terminou completamente antes ou neste instante
                completed.add(stroke)
            } else if (stroke.startedAtMs <= targetAbsoluteTime) {
                // Traço em andamento neste instante
                val partialPoints = stroke.points.filter { it.tMs <= targetAbsoluteTime }
                if (partialPoints.isNotEmpty()) {
                    active = stroke.copy(
                        points = partialPoints,
                        endedAtMs = partialPoints.last().tMs,
                        isCancelled = false
                    )
                }
            }
            // Traços com startedAtMs > targetAbsoluteTime são futuros e ignorados
        }

        return ReplayFrame(
            completedStrokes = completed,
            activeStroke = active,
            currentPositionMs = clampedElapsed,
            totalDurationMs = totalDurationMs,
            status = status,
            speed = speed
        )
    }

    private fun updateFrame() {
        _frameFlow.value = computeFrameAt(currentPositionMs)
    }
}
