package com.scribe.caligrafia.inspector.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.ToolType
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
import com.scribe.caligrafia.ink.replay.ReplayFrame
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel central do Stylus Lab para preservação de estado e resiliência de ciclo de vida.
 *
 * Garante:
 * 1. Sobrevivência total do repositório em memória contra mudanças de configuração (rotação retrato/paisagem,
 *    redimensionamento em multi-janela / Split-Screen e alternância do Samsung DeX).
 * 2. Auto-Save atômico em segundo plano via SessionLifecycleManager (.scribe binário).
 * 3. Restauração automática de contingência após interrupções do sistema operacional.
 * 4. Integração com o detector de silo da S Pen do Galaxy S25 Ultra (SPenInsertionDetector).
 */
/**
 * Predefinições visuais de pauta caligráfica para treino no Stylus Lab.
 */
enum class GuidelinePreset(val displayName: String) {
    NONE("Sem Pauta"),
    COPPERPLATE("Copperplate (52°)"),
    SCHOOL("Escolar (1:1:1)"),
    SPENCERIAN("Spencerian (68°)")
}

class StylusLabViewModel(
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

    val repository = InMemoryStrokeRepository()
    val rendererManager = RendererManager()
    val replayEngine = StrokeReplayEngine()
    val sPenDetector = SPenInsertionDetector()

    // Estados observáveis
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

    private val _selectedInputMode = MutableStateFlow(InputMode.STYLUS_ONLY)
    val selectedInputMode: StateFlow<InputMode> = _selectedInputMode.asStateFlow()

    private val _isReplayMode = MutableStateFlow(false)
    val isReplayMode: StateFlow<Boolean> = _isReplayMode.asStateFlow()

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
            if (stroke.tool != ToolType.ERASER) {
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
        palmPolicy.inputMode = InputMode.STYLUS_ONLY
    }

    init {
        // Configura callback de inserção da S Pen no silo físico:
        // Ao guardar a caneta no aparelho, finaliza com segurança qualquer traço ativo
        sPenDetector.onPenInserted = {
            pipeline.flushActiveStroke(commitIfValid = true)
            updateMetrics()
            triggerAutoSave()
        }

        // Tenta auto-restauração inicial caso haja sessão de contingência
        restoreIfAutoSaveExists()
    }

    private fun updateMetrics() {
        _strokeCount.value = repository.count
        _totalPoints.value = repository.totalPointsCount
        _historicalAbsorbed.value = pipeline.totalHistoricalSamplesAbsorbed
        _isStylusHovering.value = pipeline.palmPolicy.isStylusHovering
    }

    /**
     * Restaura a sessão automaticamente se o repositório estiver vazio e houver auto-save em disco.
     */
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

    /**
     * Executa auto-save em disco em background.
     */
    fun triggerAutoSave(): Long {
        val bytes = lifecycleManager.autoSave(repository.allStrokes)
        if (bytes > 0) {
            _autoSaveFeedback.value = "Auto-Save ativo (${repository.count} traços, $bytes B)"
        }
        return bytes
    }

    /**
     * Invocado no ciclo de vida onPause da Activity ou quando o app perde o foco de janela.
     */
    fun onPauseLifecycle() {
        pipeline.flushActiveStroke(commitIfValid = true)
        updateMetrics()
        triggerAutoSave()
    }

    /**
     * Invocado no ciclo de vida onResume da Activity.
     */
    fun onResumeLifecycle(context: Context) {
        sPenDetector.register(context)
    }

    /**
     * Invocado quando a janela perde o foco.
     */
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
        if (_isReplayMode.value) {
            stopReplay()
        }
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
            _persistenceFeedback.value = "Recarregado com fidelidade 100%: ${loaded.size} traços (${repository.totalPointsCount} pts)"
            return true
        } else {
            _persistenceFeedback.value = "Nenhuma sessão salva encontrada."
            return false
        }
    }

    fun setInputMode(mode: InputMode) {
        _selectedInputMode.value = mode
        pipeline.palmPolicy.inputMode = mode
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
        // Libera receptores e timers
        replayEngine.stop()
    }
}
