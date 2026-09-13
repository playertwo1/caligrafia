package com.scribe.caligrafia.evolution.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.evolution.engine.AttemptComparator
import com.scribe.caligrafia.evolution.engine.CalendarConsistencyHelper
import com.scribe.caligrafia.evolution.engine.DualReplayEngine
import com.scribe.caligrafia.evolution.engine.DualReplayFrame
import com.scribe.caligrafia.evolution.engine.ReplaySyncMode
import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.CalendarDayRecord
import com.scribe.caligrafia.evolution.model.EvolutionSummary
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.repository.PracticeAttemptRepository
import com.scribe.caligrafia.ink.replay.ReplayFrame
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
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
    val dualReplayFrame: DualReplayFrame = DualReplayFrame(),
    // F3.02 & F3.05: Filtros e seleção de tentativas
    val selectedTargetFilter: String? = null,
    val selectedStyleFilter: String? = null,
    val allAttempts: List<PracticeAttemptRecord> = emptyList(),
    val filteredAttempts: List<PracticeAttemptRecord> = emptyList(),
    val comparisonErrorMessage: String? = null,
    // F3.04: Replay individual de tentativa única
    val singleReplayAttempt: PracticeAttemptRecord? = null,
    val singleReplayFrame: ReplayFrame = ReplayFrame(),
    val isSingleReplayActive: Boolean = false
)

/**
 * ViewModel responsável pela coordenação de dados e motores de evolução caligráfica (M5).
 */
class EvolutionViewModel @JvmOverloads constructor(
    application: Application,
    private val attemptRepository: PracticeAttemptRepository = LocalPracticeAttemptRepository(application.filesDir),
    private val historyRepository: LocalLearningHistoryRepository = LocalLearningHistoryRepository(application.filesDir),
    private val dualReplayEngine: DualReplayEngine = DualReplayEngine(),
    private val singleReplayEngine: StrokeReplayEngine = StrokeReplayEngine(),
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : AndroidViewModel(application) {

    private val effectiveScope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(EvolutionUiState())
    val uiState: StateFlow<EvolutionUiState> = _uiState.asStateFlow()

    init {
        effectiveScope.launch {
            dualReplayEngine.frameFlow.collect { frame ->
                _uiState.update { it.copy(dualReplayFrame = frame) }
            }
        }
        effectiveScope.launch {
            singleReplayEngine.frameFlow.collect { frame ->
                _uiState.update { it.copy(singleReplayFrame = frame) }
            }
        }
        loadData()
    }

    fun loadData() {
        effectiveScope.launch {
            // F3.01: Histórico completo e sem limite artificial de 20
            val historySummary = historyRepository.getProgressSummary()
            val sessions = historySummary.recentSessions

            val cal = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
            val monthTitle = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }

            val monthDays = CalendarConsistencyHelper.buildMonthDays(sessions, cal)
            val comparisons = attemptRepository.getAllComparisons()
            val allAttempts = attemptRepository.getAllAttempts()

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

            val currentTarget = _uiState.value.selectedTargetFilter
            val currentStyle = _uiState.value.selectedStyleFilter

            val filtered = allAttempts.filter { att ->
                (currentTarget == null || att.targetId == currentTarget) &&
                (currentStyle == null || att.styleId == currentStyle)
            }

            val initialComp = _uiState.value.selectedComparison ?: comparisons.firstOrNull()
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
                    selectedComparison = initialComp,
                    allAttempts = allAttempts,
                    filteredAttempts = filtered
                )
            }
        }
    }

    fun selectTab(tab: EvolutionTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setTargetFilter(targetId: String?) {
        _uiState.update { current ->
            val filtered = current.allAttempts.filter { att ->
                (targetId == null || att.targetId == targetId) &&
                (current.selectedStyleFilter == null || att.styleId == current.selectedStyleFilter)
            }
            current.copy(selectedTargetFilter = targetId, filteredAttempts = filtered)
        }
    }

    fun setStyleFilter(styleId: String?) {
        _uiState.update { current ->
            val filtered = current.allAttempts.filter { att ->
                (current.selectedTargetFilter == null || att.targetId == current.selectedTargetFilter) &&
                (styleId == null || att.styleId == styleId)
            }
            current.copy(selectedStyleFilter = styleId, filteredAttempts = filtered)
        }
    }

    fun selectComparison(targetId: String) {
        val comp = _uiState.value.availableComparisons.firstOrNull { it.targetId == targetId } ?: return
        _uiState.update { it.copy(selectedComparison = comp, comparisonErrorMessage = null) }
        dualReplayEngine.loadTracks(comp.beforeAttempt.strokes, comp.afterAttempt.strokes)
    }

    /**
     * F3.05: Permite escolher explicitamente duas tentativas do mesmo alvo e contexto compatível;
     * explica com clareza qualquer incompatibilidade observada.
     */
    fun selectExplicitComparison(
        attemptA: PracticeAttemptRecord,
        attemptB: PracticeAttemptRecord
    ): Result<BeforeAfterComparison> {
        if (attemptA.targetId != attemptB.targetId) {
            val error = "Incompatibilidade: as duas tentativas pertencem a alvos diferentes ('${attemptA.targetTitle}' e '${attemptB.targetTitle}'). Selecione duas tentativas do mesmo exercício para comparação."
            _uiState.update { it.copy(comparisonErrorMessage = error) }
            return Result.failure(IllegalArgumentException(error))
        }

        // Ordena por timestamp para manter antes (mais antiga) e depois (mais recente)
        val (before, after) = if (attemptA.timestampMs <= attemptB.timestampMs) {
            Pair(attemptA, attemptB)
        } else {
            Pair(attemptB, attemptA)
        }

        val comp = AttemptComparator.compare(before, after)
        _uiState.update {
            it.copy(
                selectedComparison = comp,
                comparisonErrorMessage = null
            )
        }
        dualReplayEngine.loadTracks(comp.beforeAttempt.strokes, comp.afterAttempt.strokes)
        return Result.success(comp)
    }

    fun setOverlayAlpha(alpha: Float) {
        _uiState.update { it.copy(overlayAlpha = alpha.coerceIn(0f, 1f)) }
    }

    // --- Dual Replay (F3.07) ---

    fun setDualReplaySyncMode(mode: ReplaySyncMode) {
        dualReplayEngine.syncMode = mode
        _uiState.update { it.copy(dualReplayFrame = dualReplayEngine.frameFlow.value) }
    }

    fun playDualReplay() {
        dualReplayEngine.play(effectiveScope)
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

    // --- Single Attempt Replay (F3.04) ---

    fun openSingleReplay(attempt: PracticeAttemptRecord) {
        singleReplayEngine.loadStrokes(attempt.strokes)
        _uiState.update {
            it.copy(
                singleReplayAttempt = attempt,
                isSingleReplayActive = true
            )
        }
    }

    fun playSingleReplay() {
        singleReplayEngine.play(effectiveScope)
    }

    fun pauseSingleReplay() {
        singleReplayEngine.pause()
    }

    fun stopSingleReplay() {
        singleReplayEngine.stop()
    }

    fun seekSingleReplay(progress: Float) {
        singleReplayEngine.seekTo(progress)
    }

    fun setSingleReplaySpeed(speed: ReplaySpeed) {
        singleReplayEngine.setSpeed(speed)
    }

    fun closeSingleReplay() {
        singleReplayEngine.stop()
        _uiState.update {
            it.copy(
                singleReplayAttempt = null,
                isSingleReplayActive = false
            )
        }
    }

    fun clearComparisonError() {
        _uiState.update { it.copy(comparisonErrorMessage = null) }
    }
}
