package com.scribe.caligrafia.learning.session

import com.scribe.caligrafia.learning.model.CurriculumLesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Temporizador de sessão caligráfica estruturada (SCR-402).
 *
 * Gerencia a contagem regressiva por fases pedagógicas, cálculo proporcional de duração,
 * suporte a pausa, avanço manual e registro de métricas em tempo real.
 */
class SessionTimer(
    private val scope: CoroutineScope? = null
) {
    private var tickerJob: Job? = null
    private val activeScope: CoroutineScope = scope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _sessionState = MutableStateFlow<ActiveSessionState?>(null)
    val sessionState: StateFlow<ActiveSessionState?> = _sessionState.asStateFlow()

    /**
     * Inicia uma nova sessão para a lição e duração especificadas.
     */
    fun startSession(lesson: CurriculumLesson, duration: SessionDuration) {
        tickerJob?.cancel()

        val initialPhase = SessionPhase.WARM_UP
        val initialPhaseSeconds = calculatePhaseSeconds(initialPhase, duration)

        val newState = ActiveSessionState(
            lesson = lesson,
            duration = duration,
            currentPhase = initialPhase,
            phaseElapsedSeconds = 0,
            phaseTotalSeconds = initialPhaseSeconds,
            totalElapsedSeconds = 0,
            isPaused = false,
            isFinished = false,
            attemptsCount = 0,
            averageScore = null
        )

        _sessionState.value = newState
        startTicker()
    }

    /**
     * Pausa a contagem da sessão.
     */
    fun pause() {
        tickerJob?.cancel()
        _sessionState.update { it?.copy(isPaused = true) }
    }

    /**
     * Retoma a contagem caso pausada.
     */
    fun resume() {
        if (_sessionState.value?.isPaused == true && _sessionState.value?.isFinished == false) {
            _sessionState.update { it?.copy(isPaused = false) }
            startTicker()
        }
    }

    /**
     * Avança imediatamente para a próxima fase pedagógica.
     */
    fun skipToNextPhase() {
        val current = _sessionState.value ?: return
        val nextPhase = current.currentPhase.nextPhase()

        if (nextPhase != null) {
            val nextPhaseTotal = calculatePhaseSeconds(nextPhase, current.duration)
            _sessionState.update {
                it?.copy(
                    currentPhase = nextPhase,
                    phaseElapsedSeconds = 0,
                    phaseTotalSeconds = nextPhaseTotal
                )
            }
        } else {
            finishSession()
        }
    }

    /**
     * Registra uma tentativa de escrita avaliada durante a sessão.
     */
    fun recordAttempt(score: Float) {
        _sessionState.update { current ->
            if (current == null) return@update null
            val newCount = current.attemptsCount + 1
            val prevAvg = current.averageScore ?: score
            val newAvg = if (current.attemptsCount == 0) score else (prevAvg * current.attemptsCount + score) / newCount
            current.copy(
                attemptsCount = newCount,
                averageScore = newAvg
            )
        }
    }

    /**
     * Finaliza a sessão antecipadamente ou por conclusão natural.
     */
    fun finishSession() {
        tickerJob?.cancel()
        _sessionState.update {
            it?.copy(
                isFinished = true,
                currentPhase = SessionPhase.REVIEW_SUMMARY
            )
        }
    }

    /**
     * Cancela a sessão e descarta o estado ativo.
     */
    fun cancelSession() {
        tickerJob?.cancel()
        _sessionState.value = null
    }

    /**
     * Incrementa artificialmente um segundo na contagem (método síncrono para testes automatizados determinísticos).
     */
    fun tickOneSecond() {
        val current = _sessionState.value ?: return
        if (current.isPaused || current.isFinished) return

        val newTotalElapsed = current.totalElapsedSeconds + 1
        val newPhaseElapsed = current.phaseElapsedSeconds + 1

        // Verifica se a fase atual terminou
        if (newPhaseElapsed >= current.phaseTotalSeconds) {
            val nextPhase = current.currentPhase.nextPhase()
            if (nextPhase != null && newTotalElapsed < current.totalSeconds) {
                val nextPhaseTotal = calculatePhaseSeconds(nextPhase, current.duration)
                _sessionState.update {
                    it?.copy(
                        currentPhase = nextPhase,
                        phaseElapsedSeconds = 0,
                        phaseTotalSeconds = nextPhaseTotal,
                        totalElapsedSeconds = newTotalElapsed
                    )
                }
            } else {
                tickerJob?.cancel()
                _sessionState.update {
                    it?.copy(
                        totalElapsedSeconds = newTotalElapsed,
                        isFinished = true,
                        currentPhase = SessionPhase.REVIEW_SUMMARY
                    )
                }
            }
        } else {
            _sessionState.update {
                it?.copy(
                    phaseElapsedSeconds = newPhaseElapsed,
                    totalElapsedSeconds = newTotalElapsed
                )
            }
        }
    }

    /**
     * Calcula o total de segundos de uma fase baseado na duração global.
     */
    fun calculatePhaseSeconds(phase: SessionPhase, duration: SessionDuration): Int {
        val seconds = (duration.totalSeconds * phase.percentShare).roundToInt()
        return seconds.coerceAtLeast(10) // Pelo menos 10 segundos por fase
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = activeScope.launch {
            while (true) {
                delay(1000L)
                tickOneSecond()
                if (_sessionState.value?.isFinished == true) {
                    break
                }
            }
        }
    }
}
