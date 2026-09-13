package com.scribe.caligrafia.learning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import com.scribe.caligrafia.learning.history.LearningProgressSummary
import com.scribe.caligrafia.learning.history.LocalLearningHistoryRepository
import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.model.CurriculumLesson
import com.scribe.caligrafia.learning.model.CurriculumStage
import com.scribe.caligrafia.learning.review.DailyRecommendation
import com.scribe.caligrafia.learning.review.ReviewScheduler
import com.scribe.caligrafia.learning.session.ActiveSessionState
import com.scribe.caligrafia.learning.session.SessionDuration
import com.scribe.caligrafia.learning.session.SessionTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Estado de UI do Hub de Aprendizado e Sessões Caligráficas (M4 — SCR-401 a SCR-404).
 */
data class LearningUiState(
    val dailyRecommendation: DailyRecommendation? = null,
    val progressSummary: LearningProgressSummary = LearningProgressSummary(),
    val activeSession: ActiveSessionState? = null,
    val allLessons: List<CurriculumLesson> = CurriculumCatalog.ALL_LESSONS,
    val selectedStage: CurriculumStage = CurriculumStage.STAGE_1_STROKES,
    val selectedDuration: SessionDuration = SessionDuration.MIN_10,
    val isSessionDialogVisible: Boolean = false,
    val previewLesson: CurriculumLesson? = null,
    val pendingNewLesson: Pair<CurriculumLesson, SessionDuration>? = null,
    val showConflictDialog: Boolean = false,
    val lastCompletedSession: CompletedSessionRecord? = null,
    val notificationMessage: String? = null
)

/**
 * ViewModel que orquestra a trilha pedagógica, sessões deliberadas com temporizador,
 * algoritmo determinístico de repetição espaçada e histórico local seguro.
 */
class LearningViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: LocalLearningHistoryRepository = LocalLearningHistoryRepository(application.filesDir),
    private val sessionTimer: SessionTimer = SessionTimer()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    private var isFinishing = false

    init {
        // F2.11: Restaura sessão ativa interrompida se existente
        val restored = repository.loadActiveSession()
        if (restored != null && !restored.isFinished) {
            sessionTimer.restoreSession(restored)
        }

        // Observa o estado da sessão ativa, reflete na UI e persiste alterações
        viewModelScope.launch {
            sessionTimer.sessionState.collect { sessionState ->
                _uiState.update { it.copy(activeSession = sessionState) }
                if (sessionState != null && !sessionState.isFinished) {
                    repository.saveActiveSession(sessionState)
                }
            }
        }
        loadData()
    }

    /**
     * Carrega ou recarrega o histórico local e gera as recomendações do dia.
     */
    fun loadData() {
        viewModelScope.launch {
            val summary = repository.getProgressSummary()
            val recommendation = ReviewScheduler.getDailyRecommendation(
                allLessons = CurriculumCatalog.ALL_LESSONS,
                records = summary.spacedRepetitionItems.values.toList()
            )
            _uiState.update {
                it.copy(
                    progressSummary = summary,
                    dailyRecommendation = recommendation
                )
            }
        }
    }

    fun selectStage(stage: CurriculumStage) {
        _uiState.update { it.copy(selectedStage = stage) }
    }

    fun selectDuration(duration: SessionDuration) {
        _uiState.update { it.copy(selectedDuration = duration) }
    }

    /**
     * F2.03 & F2.06: Solicita o início de uma lição.
     * Se houver sessão em andamento, dispara o diálogo de conflito.
     * Caso contrário, exibe o resumo pré-início para confirmação de duração e estilo.
     */
    fun requestStartLesson(lesson: CurriculumLesson, duration: SessionDuration = _uiState.value.selectedDuration) {
        val current = _uiState.value.activeSession
        if (current != null && !current.isFinished) {
            if (current.lesson.id == lesson.id) {
                _uiState.update { it.copy(isSessionDialogVisible = true) }
            } else {
                _uiState.update {
                    it.copy(
                        showConflictDialog = true,
                        pendingNewLesson = Pair(lesson, duration)
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    previewLesson = lesson,
                    selectedDuration = duration
                )
            }
        }
    }

    fun dismissPreviewDialog() {
        _uiState.update { it.copy(previewLesson = null) }
    }

    fun dismissConflictDialog() {
        _uiState.update {
            it.copy(
                showConflictDialog = false,
                pendingNewLesson = null
            )
        }
    }

    /**
     * F2.06: Continua a sessão existente ativa descartando a tentativa de iniciar outra.
     */
    fun continueExistingSession() {
        _uiState.update {
            it.copy(
                showConflictDialog = false,
                pendingNewLesson = null,
                isSessionDialogVisible = true
            )
        }
    }

    /**
     * F2.06: Encerra e salva a sessão atual e inicia a pendente.
     */
    fun finishCurrentAndStartPending() {
        val pending = _uiState.value.pendingNewLesson
        finishAndSaveSession()
        _uiState.update {
            it.copy(
                showConflictDialog = false,
                pendingNewLesson = null
            )
        }
        if (pending != null) {
            startSession(pending.first, pending.second)
        }
    }

    fun startSession(lesson: CurriculumLesson, duration: SessionDuration = _uiState.value.selectedDuration) {
        sessionTimer.startSession(lesson, duration)
        _uiState.update {
            it.copy(
                isSessionDialogVisible = true,
                selectedDuration = duration,
                previewLesson = null
            )
        }
    }

    fun pauseSession(manual: Boolean = true) {
        sessionTimer.pause(manual)
    }

    fun resumeSession(manual: Boolean = true) {
        sessionTimer.resume(manual)
    }

    fun onPauseLifecycle() {
        sessionTimer.pause(manual = false)
        sessionTimer.sessionState.value?.let {
            if (!it.isFinished) repository.saveActiveSession(it)
        }
    }

    fun onResumeLifecycle() {
        sessionTimer.resume(manual = false)
    }

    fun skipToNextPhase() {
        sessionTimer.skipToNextPhase()
    }

    fun recordAttempt(scorePercent: Int) {
        sessionTimer.recordAttempt(scorePercent.toFloat())
    }

    /**
     * F2.21: Finaliza a sessão atual uma única vez (protegido contra reentrância / duplo clique),
     * persiste atômica e deterministicamente o histórico e recalcula a repetição espaçada.
     */
    fun finishAndSaveSession() {
        if (isFinishing) return
        val current = sessionTimer.sessionState.value ?: return

        isFinishing = true
        try {
            val avgScore = current.averageScore?.roundToInt()
            val actualMinutes = (current.totalElapsedSeconds / 60).coerceAtLeast(if (current.totalElapsedSeconds > 0) 1 else 0)
            val record = CompletedSessionRecord(
                sessionId = UUID.randomUUID().toString(),
                lessonId = current.lesson.id,
                lessonTitle = current.lesson.title,
                timestampMs = System.currentTimeMillis(),
                durationMinutes = actualMinutes,
                actualDurationSeconds = current.totalElapsedSeconds,
                attemptsCount = current.attemptsCount,
                averageScorePercent = avgScore
            )

            val currentRepItem = _uiState.value.progressSummary.spacedRepetitionItems[current.lesson.id]
            val updatedRep = if (avgScore != null) {
                ReviewScheduler.updateRepetition(
                    currentItem = currentRepItem,
                    targetId = current.lesson.id,
                    scorePercent = avgScore,
                    nowMs = record.timestampMs
                )
            } else {
                null
            }

            repository.recordSession(record, updatedRep)
            repository.clearActiveSession()
            sessionTimer.cancelSession()

            _uiState.update {
                it.copy(
                    isSessionDialogVisible = false,
                    lastCompletedSession = record,
                    notificationMessage = "Sessão concluída! ${record.durationMinutes} min (${record.actualDurationSeconds}s) registrados."
                )
            }

            loadData()
        } finally {
            isFinishing = false
        }
    }

    /**
     * F2.21: Cancela a sessão e descarta sem registrar dados fictícios.
     */
    fun cancelSession() {
        repository.clearActiveSession()
        sessionTimer.cancelSession()
        _uiState.update { it.copy(isSessionDialogVisible = false) }
    }

    fun showSessionDialog() {
        _uiState.update { it.copy(isSessionDialogVisible = true) }
    }

    fun dismissSessionDialog() {
        _uiState.update { it.copy(isSessionDialogVisible = false) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
