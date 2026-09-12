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

    init {
        // Observa o estado da sessão ativa e reflete na UI
        viewModelScope.launch {
            sessionTimer.sessionState.collect { sessionState ->
                _uiState.update { it.copy(activeSession = sessionState) }
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

    fun startSession(lesson: CurriculumLesson, duration: SessionDuration = _uiState.value.selectedDuration) {
        sessionTimer.startSession(lesson, duration)
        _uiState.update {
            it.copy(
                isSessionDialogVisible = true,
                selectedDuration = duration
            )
        }
    }

    fun pauseSession() {
        sessionTimer.pause()
    }

    fun resumeSession() {
        sessionTimer.resume()
    }

    fun skipToNextPhase() {
        sessionTimer.skipToNextPhase()
    }

    fun recordAttempt(scorePercent: Int) {
        sessionTimer.recordAttempt(scorePercent.toFloat())
    }

    /**
     * Finaliza a sessão atual, persiste atômica e deterministicamente o histórico
     * e recalcula os intervalos de repetição espaçada.
     */
    fun finishAndSaveSession() {
        val current = sessionTimer.sessionState.value ?: return

        val avgScore = current.averageScore?.roundToInt()
        val record = CompletedSessionRecord(
            sessionId = UUID.randomUUID().toString(),
            lessonId = current.lesson.id,
            lessonTitle = current.lesson.title,
            timestampMs = System.currentTimeMillis(),
            durationMinutes = current.duration.minutes,
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
        sessionTimer.cancelSession()

        _uiState.update {
            it.copy(
                isSessionDialogVisible = false,
                notificationMessage = "Sessão concluída! ${record.durationMinutes} min registrados."
            )
        }

        loadData()
    }

    fun cancelSession() {
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
