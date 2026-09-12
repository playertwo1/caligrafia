package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.ink.replay.ReplayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Estado instantâneo do frame de reprodução dupla sincronizada (SCR-503).
 */
data class DualReplayFrame(
    val visibleStrokesA: List<Stroke> = emptyList(),
    val visibleStrokesB: List<Stroke> = emptyList(),
    val progress: Float = 0f,
    val status: ReplayStatus = ReplayStatus.IDLE,
    val speed: ReplaySpeed = ReplaySpeed.NORMAL,
    val durationAMs: Long = 0L,
    val durationBMs: Long = 0L
) {
    val isPlaying: Boolean get() = status == ReplayStatus.PLAYING
}

/**
 * Motor de reprodução temporal sincronizado para duas trilhas simultâneas de traços (SCR-503).
 *
 * Permite reproduzir lado a lado a escrita do "Antes" e do "Depois", normalizando a timeline
 * de forma que o calígrafo observe o ductus, paradas e fluidez de ambas as execuções em sincronia.
 */
class DualReplayEngine(
    trackA: List<Stroke> = emptyList(),
    trackB: List<Stroke> = emptyList()
) {

    private val strokesA = mutableListOf<Stroke>()
    private val strokesB = mutableListOf<Stroke>()

    private var baseTimeAMs: Long = 0L
    private var durationAMs: Long = 0L

    private var baseTimeBMs: Long = 0L
    private var durationBMs: Long = 0L

    private var currentProgress: Float = 0f
    private var speed: ReplaySpeed = ReplaySpeed.NORMAL
    private var status: ReplayStatus = ReplayStatus.IDLE

    private val _frameFlow = MutableStateFlow(DualReplayFrame())
    val frameFlow: StateFlow<DualReplayFrame> = _frameFlow.asStateFlow()

    private var playbackJob: Job? = null

    init {
        loadTracks(trackA, trackB)
    }

    /**
     * Carrega os traços das duas faixas e reseta a timeline.
     */
    fun loadTracks(trackA: List<Stroke>, trackB: List<Stroke>) {
        stop()

        strokesA.clear()
        strokesA.addAll(trackA.sortedBy { it.startedAtMs })
        if (strokesA.isNotEmpty()) {
            baseTimeAMs = strokesA.first().startedAtMs
            val maxEnded = strokesA.maxOf { it.endedAtMs }
            durationAMs = (maxEnded - baseTimeAMs).coerceAtLeast(100L)
        } else {
            baseTimeAMs = 0L
            durationAMs = 0L
        }

        strokesB.clear()
        strokesB.addAll(trackB.sortedBy { it.startedAtMs })
        if (strokesB.isNotEmpty()) {
            baseTimeBMs = strokesB.first().startedAtMs
            val maxEnded = strokesB.maxOf { it.endedAtMs }
            durationBMs = (maxEnded - baseTimeBMs).coerceAtLeast(100L)
        } else {
            baseTimeBMs = 0L
            durationBMs = 0L
        }

        currentProgress = 0f
        status = ReplayStatus.IDLE
        dispatchCurrentFrame()
    }

    fun setSpeed(newSpeed: ReplaySpeed) {
        speed = newSpeed
        dispatchCurrentFrame()
    }

    /**
     * Inicia ou retoma a reprodução sincronizada.
     */
    fun play(scope: CoroutineScope) {
        if (status == ReplayStatus.PLAYING) return

        if (currentProgress >= 1.0f) {
            currentProgress = 0f
        }

        status = ReplayStatus.PLAYING
        playbackJob?.cancel()

        val maxDurationMs = maxOf(durationAMs, durationBMs).coerceAtLeast(1000L)
        val frameIntervalMs = 16L // ~60fps

        playbackJob = scope.launch {
            while (isActive && status == ReplayStatus.PLAYING) {
                delay(frameIntervalMs)

                val deltaProgress = (frameIntervalMs * speed.multiplier) / maxDurationMs.toFloat()
                currentProgress += deltaProgress

                if (currentProgress >= 1.0f) {
                    currentProgress = 1.0f
                    status = ReplayStatus.COMPLETED
                    dispatchCurrentFrame()
                    break
                }

                dispatchCurrentFrame()
            }
        }
    }

    fun pause() {
        if (status == ReplayStatus.PLAYING) {
            status = ReplayStatus.PAUSED
            playbackJob?.cancel()
            dispatchCurrentFrame()
        }
    }

    fun stop() {
        status = ReplayStatus.IDLE
        playbackJob?.cancel()
        currentProgress = 0f
        dispatchCurrentFrame()
    }

    fun seekToProgress(progress: Float) {
        currentProgress = progress.coerceIn(0f, 1f)
        if (status == ReplayStatus.COMPLETED && currentProgress < 1.0f) {
            status = ReplayStatus.PAUSED
        }
        dispatchCurrentFrame()
    }

    /**
     * Computa deterministicamente o frame para o progresso especificado (0.0f a 1.0f).
     */
    fun computeDualFrameAt(progress: Float): DualReplayFrame {
        val clampedProgress = progress.coerceIn(0f, 1f)

        val targetRelAMs = (clampedProgress * durationAMs).toLong()
        val visibleA = computeVisibleStrokes(strokesA, baseTimeAMs, targetRelAMs)

        val targetRelBMs = (clampedProgress * durationBMs).toLong()
        val visibleB = computeVisibleStrokes(strokesB, baseTimeBMs, targetRelBMs)

        return DualReplayFrame(
            visibleStrokesA = visibleA,
            visibleStrokesB = visibleB,
            progress = clampedProgress,
            status = status,
            speed = speed,
            durationAMs = durationAMs,
            durationBMs = durationBMs
        )
    }

    private fun dispatchCurrentFrame() {
        _frameFlow.value = computeDualFrameAt(currentProgress)
    }

    private fun computeVisibleStrokes(
        strokes: List<Stroke>,
        baseTimeMs: Long,
        relativePositionMs: Long
    ): List<Stroke> {
        if (strokes.isEmpty()) return emptyList()
        val targetAbsoluteTime = baseTimeMs + relativePositionMs
        val result = mutableListOf<Stroke>()

        for (stroke in strokes) {
            if (stroke.startedAtMs > targetAbsoluteTime) continue

            if (stroke.endedAtMs <= targetAbsoluteTime) {
                result.add(stroke)
            } else {
                val partialPoints = stroke.points.filter { it.tMs <= targetAbsoluteTime }
                if (partialPoints.isNotEmpty()) {
                    result.add(stroke.copy(points = partialPoints))
                }
            }
        }
        return result
    }
}
