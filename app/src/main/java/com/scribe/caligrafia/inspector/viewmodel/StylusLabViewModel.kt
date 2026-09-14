package com.scribe.caligrafia.inspector.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.lifecycle.SPenInsertionDetector
import com.scribe.caligrafia.ink.lifecycle.SPenSlotState
import com.scribe.caligrafia.ink.lifecycle.SessionLifecycleManager
import com.scribe.caligrafia.ink.persistence.benchmark.BenchmarkSuiteReport
import com.scribe.caligrafia.ink.persistence.benchmark.PersistenceBenchmarkRunner
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.renderer.RendererManager
import com.scribe.caligrafia.ink.renderer.RendererType
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer
import com.scribe.caligrafia.ink.replay.ReplayFrame
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import com.scribe.caligrafia.preferences.ScribePreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Predefinições visuais de pauta caligráfica para treino no Stylus Lab.
 */
enum class GuidelinePreset(val displayName: String) {
    NONE("Sem Pauta"),
    COPPERPLATE("Copperplate (52°)"),
    SCHOOL("Escolar (1:1:1)"),
    SPENCERIAN("Spencerian (68°)")
}

/**
 * ViewModel do Laboratório da S Pen.
 *
 * O laboratório permanece isolado de aprendizado, SRS e alfabeto: todos os seus vetores são
 * persistidos exclusivamente em diretórios de laboratório. Preferências de entrada/render são
 * compartilhadas de forma explícita com o app, mas raw strokes do laboratório nunca alimentam
 * progresso pedagógico.
 */
class StylusLabViewModel @JvmOverloads constructor(
    application: Application,
    val lifecycleManager: SessionLifecycleManager = SessionLifecycleManager(
        File(application.filesDir, "autosave_storage")
    ),
    val persistenceStrategy: DedicatedFileStrategy = DedicatedFileStrategy(
        File(application.filesDir, "spike_storage")
    ),
    val benchmarkRunner: PersistenceBenchmarkRunner = PersistenceBenchmarkRunner(
        File(application.cacheDir, "spike_benchmark")
    )
) : AndroidViewModel(application) {

    private val preferencesStore = ScribePreferencesStore(application)
    private val initialPreferences = preferencesStore.load()

    val repository = InMemoryStrokeRepository()
    val rendererManager = RendererManager(
        smoothedRenderer = SmoothedReferenceRenderer(pressureCurve = initialPreferences.pressureCurve)
    )
    val replayEngine = StrokeReplayEngine()
    val sPenDetector = SPenInsertionDetector()

    private val _strokeCount = MutableStateFlow(0)
    val strokeCount: StateFlow<Int> = _strokeCount.asStateFlow()

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _historicalAbsorbed = MutableStateFlow(0L)
    val historicalAbsorbed: StateFlow<Long> = _historicalAbsorbed.asStateFlow()

    private val _lastStrokeDuration = MutableStateFlow(0L)
    val lastStrokeDuration: StateFlow<Long> = _lastStrokeDuration.asStateFlow()

    private val _palmRejectionsCount = MutableStateFlow(0L)
    val palmRejectionsCount: StateFlow<Long> = _palmRejectionsCount.asStateFlow()

    private val _lastRejectionReason = MutableStateFlow<String?>(null)
    val lastRejectionReason: StateFlow<String?> = _lastRejectionReason.asStateFlow()

    private val _isStylusHovering = MutableStateFlow(false)
    val isStylusHovering: StateFlow<Boolean> = _isStylusHovering.asStateFlow()

    private val _selectedRendererType = MutableStateFlow(rendererManager.activeRenderer.type)
    val selectedRendererType: StateFlow<RendererType> = _selectedRendererType.asStateFlow()

    private val _selectedInputMode = MutableStateFlow(initialPreferences.inputMode)
    val selectedInputMode: StateFlow<InputMode> = _selectedInputMode.asStateFlow()

    private val _pressureCurve = MutableStateFlow(initialPreferences.pressureCurve)
    val pressureCurve: StateFlow<PressureCurveType> = _pressureCurve.asStateFlow()

    private val _isReplayMode = MutableStateFlow(false)
    val isReplayMode: StateFlow<Boolean> = _isReplayMode.asStateFlow()
    private val _isRecording = MutableStateFlow(true)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    val replayFrame: StateFlow<ReplayFrame> = replayEngine.frameFlow

    val sPenSlotState: StateFlow<SPenSlotState> = sPenDetector.state

    private val _autoSaveFeedback = MutableStateFlow<String?>(null)
    val autoSaveFeedback: StateFlow<String?> = _autoSaveFeedback.asStateFlow()

    private val _persistenceFeedback = MutableStateFlow<String?>(null)
    val persistenceFeedback: StateFlow<String?> = _persistenceFeedback.asStateFlow()

    private val _benchmarkReport = MutableStateFlow<BenchmarkSuiteReport?>(null)
    val benchmarkReport: StateFlow<BenchmarkSuiteReport?> = _benchmarkReport.asStateFlow()

    private val _showBenchmarkDialog = MutableStateFlow(false)
    val showBenchmarkDialog: StateFlow<Boolean> = _showBenchmarkDialog.asStateFlow()

    private val _selectedGuidelinePreset = MutableStateFlow(GuidelinePreset.COPPERPLATE)
    val selectedGuidelinePreset: StateFlow<GuidelinePreset> = _selectedGuidelinePreset.asStateFlow()

    private val _guidelineConfig = MutableStateFlow<com.scribe.caligrafia.core.model.GuidelineConfig?>(
        com.scribe.caligrafia.core.model.GuidelineConfig.copperplate()
    )
    val guidelineConfig: StateFlow<com.scribe.caligrafia.core.model.GuidelineConfig?> = _guidelineConfig.asStateFlow()

    fun setGuidelinePreset(preset: GuidelinePreset) {
        _selectedGuidelinePreset.value = preset
        _guidelineConfig.value = when (preset) {
            GuidelinePreset.NONE -> null
            GuidelinePreset.COPPERPLATE -> com.scribe.caligrafia.core.model.GuidelineConfig.copperplate()
            GuidelinePreset.SCHOOL -> com.scribe.caligrafia.core.model.GuidelineConfig.school()
            GuidelinePreset.SPENCERIAN -> com.scribe.caligrafia.core.model.GuidelineConfig.spencerian()
        }
    }

    val pipeline = StrokeCapturePipeline(
        onStrokeCompleted = { stroke ->
            if (_isRecording.value && stroke.tool != ToolType.ERASER) {
                repository.addStroke(stroke)
                updateMetrics()
                _lastStrokeDuration.value = stroke.durationMs
                triggerAutoSave()
            }
        },
        onStrokeCancelled = {
            updateMetrics()
        },
        onEraserPointsAdded = { eraserPoints ->
            val erased = repository.eraseStrokesIntersecting(eraserPoints, eraserRadius = 28f)
            if (erased.isNotEmpty()) {
                updateMetrics()
                triggerAutoSave()
            }
        },
        onPalmTouchRejected = { _, reason ->
            _palmRejectionsCount.value++
            _lastRejectionReason.value = reason
        }
    ).apply {
        palmPolicy.inputMode = initialPreferences.inputMode
    }

    init {
        sPenDetector.onPenInserted = {
            pipeline.flushActiveStroke(commitIfValid = true)
            updateMetrics()
            triggerAutoSave()
        }
        restoreIfAutoSaveExists()
    }

    private fun updateMetrics() {
        _strokeCount.value = repository.count
        _totalPoints.value = repository.totalPointsCount
        _historicalAbsorbed.value = pipeline.totalHistoricalSamplesAbsorbed
        _isStylusHovering.value = pipeline.palmPolicy.isStylusHovering
    }

    fun restoreIfAutoSaveExists(): Boolean {
        if (repository.isEmpty && lifecycleManager.hasAutoSave()) {
            val restored = lifecycleManager.restore()
            if (restored.isNotEmpty()) {
                restored.forEach { repository.addStroke(it) }
                updateMetrics()
                _autoSaveFeedback.value = "Sessão anterior restaurada (${restored.size} traços)"
                return true
            }
        }
        return false
    }

    fun triggerAutoSave(): Long {
        val bytes = lifecycleManager.autoSave(repository.allStrokes)
        if (bytes > 0) {
            _autoSaveFeedback.value = "Auto-Save ativo (${repository.count} traços, $bytes B)"
        }
        return bytes
    }

    fun onPauseLifecycle(context: Context? = null) {
        if (context != null) sPenDetector.unregister(context)
        pipeline.flushActiveStroke(commitIfValid = true)
        updateMetrics()
        triggerAutoSave()
    }

    fun onResumeLifecycle(context: Context) {
        val current = preferencesStore.load()
        pipeline.palmPolicy.inputMode = current.inputMode
        _selectedInputMode.value = current.inputMode
        rendererManager.smoothedRenderer.pressureCurve = current.pressureCurve
        _pressureCurve.value = current.pressureCurve
        sPenDetector.register(context)
    }

    fun onWindowFocusLost() {
        pipeline.flushActiveStroke(commitIfValid = true)
        updateMetrics()
        triggerAutoSave()
    }

    fun undo() {
        if (repository.canUndo) {
            repository.removeLastStroke()
            updateMetrics()
            triggerAutoSave()
        }
    }

    fun clearSession() {
        if (_isReplayMode.value) stopReplay()
        repository.clear()
        pipeline.resetMetrics()
        lifecycleManager.clearAutoSave()
        _strokeCount.value = 0
        _totalPoints.value = 0
        _historicalAbsorbed.value = 0L
        _lastStrokeDuration.value = 0L
        _palmRejectionsCount.value = 0L
        _lastRejectionReason.value = null
        _autoSaveFeedback.value = null
        _persistenceFeedback.value = null
    }

    fun startRecording() {
        if (_isReplayMode.value) stopReplay()
        _isRecording.value = true
        _persistenceFeedback.value = "Gravação iniciada."
    }

    fun stopRecording() {
        pipeline.flushActiveStroke(commitIfValid = true)
        _isRecording.value = false
        updateMetrics()
        _persistenceFeedback.value = "Gravação parada."
    }

    fun saveManual() {
        val bytes = persistenceStrategy.save("current_session", repository.allStrokes)
        _persistenceFeedback.value = "Salvo em disco: ${repository.count} traços ($bytes B)"
    }

    fun loadManual(): Boolean {
        val loaded = persistenceStrategy.load("current_session")
        if (loaded.isNotEmpty()) {
            repository.clear()
            loaded.forEach { repository.addStroke(it) }
            updateMetrics()
            triggerAutoSave()
            _persistenceFeedback.value = "Recarregado com fidelidade vetorial: ${loaded.size} traços (${repository.totalPointsCount} pts)"
            return true
        }
        _persistenceFeedback.value = "Nenhuma sessão salva encontrada."
        return false
    }

    fun setInputMode(mode: InputMode) {
        _selectedInputMode.value = mode
        pipeline.palmPolicy.inputMode = mode
        runCatching { preferencesStore.update { it.copy(inputMode = mode) } }
    }

    fun setPressureCurve(curve: PressureCurveType) {
        val contracted = when (curve) {
            PressureCurveType.LINEAR,
            PressureCurveType.SOFT,
            PressureCurveType.FIRM -> curve
            PressureCurveType.SIGMOID_CALLIGRAPHIC -> PressureCurveType.LINEAR
        }
        rendererManager.smoothedRenderer.pressureCurve = contracted
        _pressureCurve.value = contracted
        runCatching { preferencesStore.update { it.copy(pressureCurve = contracted) } }
    }

    fun setRenderer(type: RendererType) {
        _selectedRendererType.value = type
        rendererManager.selectRenderer(type)
    }

    fun toggleReplay(scope: CoroutineScope) {
        if (!_isReplayMode.value) {
            _isReplayMode.value = true
            replayEngine.loadStrokes(repository.allStrokes)
            replayEngine.play(scope)
        } else {
            stopReplay()
        }
    }

    fun stopReplay() {
        _isReplayMode.value = false
        replayEngine.stop()
    }

    fun playReplay(scope: CoroutineScope) {
        replayEngine.play(scope)
    }

    fun pauseReplay() {
        replayEngine.pause()
    }

    fun seekReplay(fraction: Float) {
        replayEngine.seekTo(fraction)
    }

    fun setReplaySpeed(speed: ReplaySpeed) {
        replayEngine.setSpeed(speed)
    }

    fun runPersistenceBenchmark() {
        val report = benchmarkRunner.runBenchmark(strokeCount = 20, pointsPerStroke = 50)
        _benchmarkReport.value = report
        _showBenchmarkDialog.value = true
    }

    fun dismissBenchmarkDialog() {
        _showBenchmarkDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        replayEngine.stop()
    }
}
