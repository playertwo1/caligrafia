package com.scribe.caligrafia.guided.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.scribe.caligrafia.core.model.GuidelineBand
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.SlantConfig
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.guided.model.FeedbackEvaluation
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import com.scribe.caligrafia.guided.model.ReferenceGlyph
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.style.model.ScribeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado observável da tela de Treino Guiado (M2 e M3).
 */
data class GuidedPracticeState(
    val selectedGlyph: ReferenceGlyph = ReferenceGlyphCatalog.BASIC_SLANT,
    val currentStage: PracticeStage = PracticeStage.TRACE,
    val ghostModeLevel: GhostModeLevel = GhostModeLevel.CLEAR,
    val guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate(xHeightPx = 65f),
    val strokeCount: Int = 0,
    val evaluation: FeedbackEvaluation? = null,
    val availableGlyphs: List<ReferenceGlyph> = ReferenceGlyphCatalog.ALL_GLYPHS,
    val availableStyles: List<ScribeStyle> = emptyList(),
    val currentStyle: ScribeStyle = BuiltInStyles.COPPERPLATE
)

/**
 * ViewModel para o gerenciamento pedagógico do Treino Guiado (M2 e M3).
 */
class GuidedPracticeViewModel @JvmOverloads constructor(
    application: Application,
    val strokeRepository: InMemoryStrokeRepository = InMemoryStrokeRepository(),
    val pipeline: StrokeCapturePipeline = StrokeCapturePipeline(toolConfig = ToolConfig())
) : AndroidViewModel(application) {

    val styleEngine = StyleEngine()

    private val _state = MutableStateFlow(
        GuidedPracticeState(
            availableStyles = styleEngine.getAvailableStyles(),
            currentStyle = styleEngine.getStyle("copperplate")
        )
    )
    val state: StateFlow<GuidedPracticeState> = _state.asStateFlow()

    fun selectStyle(styleId: String) {
        val style = styleEngine.getStyle(styleId)
        val newConfig = style.toGuidelineConfig(xHeightPx = _state.value.guidelineConfig.xHeightPx)
        _state.update {
            it.copy(
                currentStyle = style,
                guidelineConfig = newConfig,
                evaluation = null
            )
        }
    }

    fun selectGlyph(glyph: ReferenceGlyph) {
        clearAttempt()
        _state.update {
            it.copy(
                selectedGlyph = glyph,
                evaluation = null
            )
        }
    }

    fun selectGlyphById(id: String) {
        val glyph = com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog.findById(id) ?: return
        selectGlyph(glyph)
    }

    fun selectStage(stage: PracticeStage) {
        clearAttempt()
        _state.update {
            it.copy(
                currentStage = stage,
                ghostModeLevel = stage.defaultGhostLevel,
                evaluation = null
            )
        }
    }

    fun setGhostMode(level: GhostModeLevel) {
        _state.update { it.copy(ghostModeLevel = level) }
    }

    fun clearAttempt() {
        strokeRepository.clear()
        if (pipeline.isCapturing) {
            pipeline.onPointerCancel(0, System.currentTimeMillis())
        }
        _state.update {
            it.copy(
                strokeCount = 0,
                evaluation = null
            )
        }
    }

    fun undo() {
        strokeRepository.undo()
        _state.update {
            it.copy(
                strokeCount = strokeRepository.count,
                evaluation = null
            )
        }
    }

    fun notifyStrokeChanged() {
        _state.update {
            it.copy(
                strokeCount = strokeRepository.count,
                evaluation = null // A14: Invalida avaliação prévia após alteração de traços
            )
        }
    }

    /**
     * Executa a avaliação geométrica determinística com os dados espaciais atuais do canvas.
     */
    fun evaluateCurrentAttempt(
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float,
        slant: SlantConfig?
    ) {
        val strokes = strokeRepository.allStrokes
        val currentGlyph = _state.value.selectedGlyph

        val eval = GeometricFeedbackEvaluator.evaluate(
            userStrokes = strokes,
            reference = currentGlyph,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = slant
        )

        _state.update { it.copy(evaluation = eval) }
    }

    /**
     * Avança para o próximo estágio pedagógico (Cobrir -> Copiar -> Sozinho), ou próximo exercício.
     */
    fun advanceProgress() {
        val currentStage = _state.value.currentStage
        if (currentStage != PracticeStage.SOLO) {
            selectStage(currentStage.nextStage())
        } else {
            // Avança para o próximo glyph da lista
            val all = _state.value.availableGlyphs
            val curIdx = all.indexOfFirst { it.id == _state.value.selectedGlyph.id }
            val nextIdx = if (curIdx in 0 until all.size - 1) curIdx + 1 else 0
            val nextGlyph = all[nextIdx]
            selectGlyph(nextGlyph)
            selectStage(PracticeStage.TRACE)
        }
    }
}
