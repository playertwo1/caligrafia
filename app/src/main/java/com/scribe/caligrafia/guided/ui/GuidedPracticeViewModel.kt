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
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.alphabet.repository.PersonalAlphabetRepository
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.repository.PracticeAttemptRepository
import com.scribe.caligrafia.guided.model.DirectionalHint
import com.scribe.caligrafia.guided.model.GlyphCategory
import com.scribe.caligrafia.guided.model.ReferencePoint
import com.scribe.caligrafia.guided.model.ReferenceStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession

/**
 * Estado observável da tela de Treino Guiado (M2 e M3, F4.08, F4.09).
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
    val currentStyle: ScribeStyle = BuiltInStyles.COPPERPLATE,
    val elapsedSeconds: Long = 0L,
    val isTimerRunning: Boolean = true,
    val feedbackMessage: String? = null,
    val activePrescription: PrescribedPracticeSession? = null,
    val activePrescriptionId: String? = null
)

/**
 * ViewModel para o gerenciamento pedagógico do Treino Guiado (M2 e M3).
 */
class GuidedPracticeViewModel @JvmOverloads constructor(
    application: Application,
    val strokeRepository: InMemoryStrokeRepository = InMemoryStrokeRepository(),
    val pipeline: StrokeCapturePipeline = StrokeCapturePipeline(toolConfig = ToolConfig()),
    alphabetRepo: PersonalAlphabetRepository? = null,
    attemptRepo: PracticeAttemptRepository? = null
) : AndroidViewModel(application) {

    val styleEngine = StyleEngine(
        customFontsDir = java.io.File(application.filesDir, "custom_fonts")
    )
    private val alphabetRepository: PersonalAlphabetRepository = alphabetRepo
        ?: LocalPersonalAlphabetRepository(
            java.io.File(application.filesDir ?: java.io.File(System.getProperty("java.io.tmpdir", "."), "scribe_guided_test"), "personal_alphabet")
        )
    val attemptRepository: PracticeAttemptRepository = attemptRepo
        ?: LocalPracticeAttemptRepository(
            application.filesDir ?: java.io.File(System.getProperty("java.io.tmpdir", "."), "scribe_guided_attempts")
        )

    var onAttemptEvaluated: ((glyphId: String, scorePercent: Int) -> Unit)? = null
    var onPrescriptionEvaluated: ((prescriptionId: String, scorePercent: Int) -> Unit)? = null

    private val _state = MutableStateFlow(
        GuidedPracticeState(
            availableStyles = styleEngine.getAvailableStyles(),
            currentStyle = styleEngine.getStyle("copperplate")
        )
    )
    val state: StateFlow<GuidedPracticeState> = _state.asStateFlow()

    init {
        // Inicia o cronômetro da sessão ativa de caligrafia (R01/R08)
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000L)
                if (_state.value.isTimerRunning) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private var wasManuallyPaused: Boolean = false

    fun toggleTimer() {
        val newRunning = !_state.value.isTimerRunning
        wasManuallyPaused = !newRunning
        _state.update { it.copy(isTimerRunning = newRunning) }
    }

    /**
     * A06 & F2.10: Pausa o cronômetro ativo e faz flush de segurança no pipeline de escrita
     * quando a tela é pausada ou o aplicativo entra em segundo plano.
     */
    fun onPauseLifecycle() {
        pipeline.flushActiveStroke(commitIfValid = true)
        _state.update { it.copy(isTimerRunning = false) }
    }

    /**
     * A06 & F2.11: Retoma o cronômetro ativo apenas se NÃO foi pausado manualmente pelo usuário.
     */
    fun onResumeLifecycle() {
        if (!wasManuallyPaused) {
            _state.update { it.copy(isTimerRunning = true) }
        }
    }

    /**
     * F2.07: Transporta targetId e styleId simultaneamente sem recorrer a fallbacks genéricos.
     */
    fun selectTargetAndStyle(exerciseId: String, styleId: String) {
        selectStyleById(styleId)
        selectGlyphBySymbolOrId(exerciseId)
    }

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

    /**
     * Seleciona um estilo caligráfico formal e reconfigura as pautas e o ângulo alvo (R17, S20).
     */
    fun selectStyle(style: ScribeStyle) {
        clearAttempt()
        val currentXHeight = _state.value.guidelineConfig.xHeightPx
        val newGuidelines = GuidelineConfig.fromStyle(style, currentXHeight)
        _state.update {
            it.copy(
                currentStyle = style,
                guidelineConfig = newGuidelines,
                evaluation = null
            )
        }
    }

    fun selectStyleById(styleId: String) {
        val style = styleEngine.getStyle(styleId)
        selectStyle(style)
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
     * Persiste a tentativa com traços vetoriais reais no repositório de evolução (R03, R18).
     */
    fun evaluateCurrentAttempt(
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float,
        slant: SlantConfig?
    ): FeedbackEvaluation {
        val strokes = strokeRepository.allStrokes
        val currentGlyph = _state.value.selectedGlyph
        val currentStyle = _state.value.currentStyle

        val effectiveSlant = slant ?: _state.value.guidelineConfig.slant ?: SlantConfig(
            angleDegrees = currentStyle.defaultSlantAngle
        )

        val eval = GeometricFeedbackEvaluator.evaluate(
            userStrokes = strokes,
            reference = currentGlyph,
            band = band,
            originX = originX,
            glyphWidthPx = glyphWidthPx,
            slant = effectiveSlant
        )

        _state.update { it.copy(evaluation = eval) }

        // Persiste a tentativa real no repositório de evolução (R03, R18)
        if (strokes.isNotEmpty()) {
            val existingAttempts = attemptRepository.getAttemptsForTarget(currentGlyph.id)
            val isBaseline = existingAttempts.isEmpty()
            val measuredSlant = eval.slant.measuredAngleDegrees ?: effectiveSlant.angleDegrees
            val attemptRecord = PracticeAttemptRecord(
                attemptId = UUID.randomUUID().toString(),
                targetId = currentGlyph.id,
                targetTitle = currentGlyph.name.ifBlank { currentGlyph.symbol },
                timestampMs = System.currentTimeMillis(),
                strokes = strokes,
                scorePercent = eval.scorePercent,
                averageSlantDegrees = measuredSlant,
                durationMs = (_state.value.elapsedSeconds * 1000L).coerceAtLeast(1000L),
                isBaseline = isBaseline,
                targetSlantDegrees = effectiveSlant.angleDegrees,
                styleId = currentStyle.id
            )
            attemptRepository.saveAttempt(attemptRecord)
            onAttemptEvaluated?.invoke(currentGlyph.id, eval.scorePercent)

            // F4.09: Se o treino veio de uma prescrição do Professor IA, relaciona o resultado
            _state.value.activePrescription?.let { presc ->
                onPrescriptionEvaluated?.invoke(presc.id, eval.scorePercent)
            }
        }

        return eval
    }

    /**
     * Inicia uma sessão de treino prescrita pelo Professor IA (F4.08).
     * Transporta prescriptionId e parâmetros completos, aplicando 70% (ou outro nível)
     * à opacidade do Ghost Mode, e não à escala da tela.
     */
    fun startPrescribedPractice(prescription: PrescribedPracticeSession) {
        val ghostLevel = GhostModeLevel.fromAlpha(prescription.recommendedGhostLevel)
        _state.update {
            it.copy(
                activePrescription = prescription,
                activePrescriptionId = prescription.id,
                ghostModeLevel = ghostLevel
            )
        }
        selectGlyphBySymbolOrId(prescription.focusExerciseId)
    }

    /**
     * Seleciona um glifo ou exercício por ID canônico ou símbolo textual (R04, S18).
     */
    fun selectGlyphBySymbolOrId(symbolOrId: String) {
        val clean = symbolOrId.trim()
        val lower = clean.lowercase()

        // 1. Verificação direta no catálogo canônico por ID
        var target = ReferenceGlyphCatalog.findById(clean)
            ?: ReferenceGlyphCatalog.findById(lower)

        // 2. Mapeamento de prescrições e exercícios pedagógicos (S18)
        if (target == null) {
            target = when {
                lower == "basic_slant" || lower == "slant" || lower.contains("slant") -> ReferenceGlyphCatalog.BASIC_SLANT
                lower == "basic_underturn" || lower == "underturn" || lower.contains("underturn") -> ReferenceGlyphCatalog.BASIC_UNDERTURN
                lower == "basic_overturn" || lower == "overturn" || lower.contains("overturn") -> ReferenceGlyphCatalog.BASIC_OVERTURN
                lower == "basic_compound" || lower == "compound" || lower.contains("compound") -> ReferenceGlyphCatalog.BASIC_COMPOUND
                lower == "basic_oval" || lower == "oval" -> ReferenceGlyphCatalog.BASIC_OVAL
                lower.contains("ascend") || lower.contains("ascender") || lower == "loop" -> ReferenceGlyphCatalog.BASIC_ASCENDING_LOOP
                else -> null
            }
        }

        // 3. Resolução de glifos do Alfabeto Pessoal (R04) ex: "glyph_lower_a" -> 'a'
        if (target == null) {
            val letterPart = clean
                .removePrefix("glyph_lower_")
                .removePrefix("glyph_upper_")
                .removePrefix("glyph_digit_")

            // Procura no catálogo primeiro por símbolo exato
            target = com.scribe.caligrafia.style.model.StyleExerciseMatrix.getReference(clean, _state.value.currentStyle.id)
                ?: ReferenceGlyphCatalog.ALL_GLYPHS.find { it.symbol == letterPart }
                ?: ReferenceGlyphCatalog.ALL_GLYPHS.find { it.symbol.equals(letterPart, ignoreCase = true) }
                ?: ReferenceGlyphCatalog.ALL_GLYPHS.find { it.id == clean || it.id == "letter_$letterPart" }
                ?: _state.value.availableGlyphs.find { it.symbol == letterPart }
                ?: _state.value.availableGlyphs.find { it.symbol.equals(letterPart, ignoreCase = true) }
                ?: _state.value.availableGlyphs.find { it.id.equals(clean, ignoreCase = true) }
        }

        val glyph = target ?: createDynamicGlyph(clean)
        selectGlyph(glyph)
    }

    /**
     * F3.20: Cria glifo dinâmico sem recorrer a triângulo artificial para glifos ausentes.
     * Os traços de referência permanecem vazios quando não há modelo vetorial canônico.
     */
    private fun createDynamicGlyph(symbolOrId: String): ReferenceGlyph {
        val letter = symbolOrId.removePrefix("glyph_lower_").removePrefix("glyph_upper_").removePrefix("glyph_digit_")
        val isLower = symbolOrId.startsWith("glyph_lower_") || (letter.length == 1 && letter[0].isLowerCase())
        val category = if (isLower) GlyphCategory.LOWERCASE else GlyphCategory.UPPERCASE
        val displaySymbol = if (letter.isNotBlank()) letter.take(2) else symbolOrId.take(2)
        return ReferenceGlyph(
            id = symbolOrId,
            symbol = displaySymbol,
            name = "Glifo '$displaySymbol'",
            category = category,
            instructions = "Pratique a forma do glifo '$displaySymbol' mantendo atenção ao paralelismo e altura-x.",
            widthToXHeightRatio = 1.0f,
            strokes = emptyList() // F3.20: Proibido triângulo genérico para glifo ausente
        )
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

    /**
     * Salva a tentativa vetorial atual como uma variante no Alfabeto Pessoal (R03/R04/R10).
     */
    fun saveAttemptToPersonalAlphabet(onComplete: (Boolean) -> Unit = {}) {
        val strokes = strokeRepository.allStrokes
        if (strokes.isEmpty()) {
            onComplete(false)
            return
        }
        val glyph = _state.value.selectedGlyph
        val eval = _state.value.evaluation
        val score = eval?.scorePercent?.toFloat() ?: 0f
        val slant = eval?.slant?.measuredAngleDegrees ?: _state.value.currentStyle.defaultSlantAngle

        val alphabetGlyphId = when {
            glyph.id.startsWith("glyph_") -> glyph.id
            glyph.symbol.length == 1 && glyph.symbol[0].isLowerCase() -> "glyph_lower_${glyph.symbol.lowercase()}"
            glyph.symbol.length == 1 && glyph.symbol[0].isUpperCase() -> "glyph_upper_${glyph.symbol.uppercase()}"
            glyph.symbol.length == 1 && glyph.symbol[0].isDigit() -> "glyph_digit_${glyph.symbol}"
            else -> "glyph_lower_${glyph.id.lowercase()}"
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                alphabetRepository.addVariant(
                    glyphId = alphabetGlyphId,
                    strokes = strokes,
                    score = score,
                    slantAngle = slant,
                    setFavorite = true
                )
                _state.update { it.copy(feedbackMessage = "Letra '${glyph.symbol}' salva no seu Alfabeto!") }
                onComplete(true)
            } catch (e: Exception) {
                _state.update { it.copy(feedbackMessage = "Falha ao salvar no Alfabeto: ${e.message}") }
                onComplete(false)
            }
        }
    }

    fun dismissFeedbackMessage() {
        _state.update { it.copy(feedbackMessage = null) }
    }
}
