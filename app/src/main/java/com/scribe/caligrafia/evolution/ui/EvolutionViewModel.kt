package com.scribe.caligrafia.evolution.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.evolution.engine.CalendarConsistencyHelper
import com.scribe.caligrafia.evolution.engine.DualReplayEngine
import com.scribe.caligrafia.evolution.engine.DualReplayFrame
import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.CalendarDayRecord
import com.scribe.caligrafia.evolution.model.EvolutionSummary
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.repository.PracticeAttemptRepository
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.learning.history.LocalLearningHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Abas de visualização da tela de evolução (SCR-501 a SCR-504).
 */
enum class EvolutionTab(val title: String) {
    CALENDAR("Consistência (SCR-504)"),
    BEFORE_AFTER("Antes / Depois (SCR-501)"),
    OVERLAY("Sobreposição (SCR-502)"),
    DUAL_REPLAY("Dual Replay (SCR-503)")
}

/**
 * Estado da tela de evolução caligráfica.
 */
data class EvolutionUiState(
    val activeTab: EvolutionTab = EvolutionTab.CALENDAR,
    val monthDays: List<CalendarDayRecord> = emptyList(),
    val currentMonthTitle: String = "",
    val summary: EvolutionSummary = EvolutionSummary(),
    val availableComparisons: List<BeforeAfterComparison> = emptyList(),
    val selectedComparison: BeforeAfterComparison? = null,
    val overlayAlpha: Float = 0.5f,
    val dualReplayFrame: DualReplayFrame = DualReplayFrame()
)

/**
 * ViewModel responsável pela coordenação de dados e motores de evolução caligráfica (M5).
 */
class EvolutionViewModel @JvmOverloads constructor(
    application: Application,
    private val attemptRepository: PracticeAttemptRepository = LocalPracticeAttemptRepository(application.filesDir),
    private val historyRepository: LocalLearningHistoryRepository = LocalLearningHistoryRepository(application.filesDir),
    private val dualReplayEngine: DualReplayEngine = DualReplayEngine()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EvolutionUiState())
    val uiState: StateFlow<EvolutionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dualReplayEngine.frameFlow.collect { frame ->
                _uiState.update { it.copy(dualReplayFrame = frame) }
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val historySummary = historyRepository.getProgressSummary()
            val sessions = historySummary.recentSessions

            val cal = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
            val monthTitle = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }

            val monthDays = CalendarConsistencyHelper.buildMonthDays(sessions, cal)
            val comparisons = attemptRepository.getAllComparisons()

            val avgGain = if (comparisons.isNotEmpty()) {
                (comparisons.sumOf { it.scoreGainPercent } / comparisons.size).coerceAtLeast(0)
            } else {
                0
            }

            val evolutionSummary = CalendarConsistencyHelper.computeSummary(
                sessions = sessions,
                comparisonsCount = comparisons.size,
                averageGain = avgGain
            )

            val initialComp = comparisons.firstOrNull()
            if (initialComp != null) {
                dualReplayEngine.loadTracks(
                    initialComp.beforeAttempt.strokes,
                    initialComp.afterAttempt.strokes
                )
            }

            _uiState.update {
                it.copy(
                    monthDays = monthDays,
                    currentMonthTitle = monthTitle,
                    summary = evolutionSummary,
                    availableComparisons = comparisons,
                    selectedComparison = initialComp
                )
            }
        }
    }

    fun selectTab(tab: EvolutionTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectComparison(targetId: String) {
        val comp = _uiState.value.availableComparisons.firstOrNull { it.targetId == targetId } ?: return
        _uiState.update { it.copy(selectedComparison = comp) }
        dualReplayEngine.loadTracks(comp.beforeAttempt.strokes, comp.afterAttempt.strokes)
    }

    fun setOverlayAlpha(alpha: Float) {
        _uiState.update { it.copy(overlayAlpha = alpha.coerceIn(0f, 1f)) }
    }

    fun playDualReplay() {
        dualReplayEngine.play(viewModelScope)
    }

    fun pauseDualReplay() {
        dualReplayEngine.pause()
    }

    fun stopDualReplay() {
        dualReplayEngine.stop()
    }

    fun seekDualReplay(progress: Float) {
        dualReplayEngine.seekToProgress(progress)
    }

    fun setDualReplaySpeed(speed: ReplaySpeed) {
        dualReplayEngine.setSpeed(speed)
    }
}
