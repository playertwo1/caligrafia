package com.scribe.caligrafia.alphabet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.alphabet.repository.PersonalAlphabetRepository
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.style.model.ScribeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Estado reativo da tela Meu Alfabeto (M6 — SCR-601 a SCR-604).
 */
data class AlphabetUiState(
    val alphabet: PersonalAlphabet = PersonalAlphabet(),
    val selectedCategory: AlphabetCategory = AlphabetCategory.LOWERCASE,
    val selectedGlyph: PersonalGlyph? = null,
    val selectedVariantStrokes: List<Stroke> = emptyList(),
    val isCompilingStyle: Boolean = false,
    val lastCompiledStyle: ScribeStyle? = null,
    val compilationSuccessDialogVisible: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * ViewModel responsável pela gestão do Alfabeto Pessoal e compilação do PersonalStyle (SCR-604).
 *
 * Em conformidade com as regras arquiteturais do Scribe:
 * - Utiliza [@JvmOverloads constructor] para compatibilidade irrestrita com [AndroidViewModelFactory].
 * - Não acopla persistência a bibliotecas de nuvem ou IA.
 * - Gerencia de forma atômica o ciclo de vida e a curadoria de variantes caligráficas.
 */
class AlphabetViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PersonalAlphabetRepository = LocalPersonalAlphabetRepository(
        baseDir = File(application.filesDir, "personal_alphabet")
    ),
    private val styleEngine: StyleEngine = StyleEngine(
        customFontsDir = File(application.filesDir, "custom_fonts")
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AlphabetUiState())
    val uiState: StateFlow<AlphabetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAlphabet().collect { alphabet ->
                _uiState.update { current ->
                    val updatedSelectedGlyph = current.selectedGlyph?.let { sel ->
                        alphabet.glyphs[sel.id]
                    }
                    current.copy(
                        alphabet = alphabet,
                        selectedGlyph = updatedSelectedGlyph
                    )
                }
            }
        }
    }

    fun selectCategory(category: AlphabetCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectGlyph(glyph: PersonalGlyph?) {
        _uiState.update { it.copy(selectedGlyph = glyph, selectedVariantStrokes = emptyList()) }
        if (glyph != null) {
            val activeVariant = glyph.activeVariant
            if (activeVariant != null) {
                viewModelScope.launch {
                    val strokes = repository.getVariantStrokes(activeVariant.id)
                    _uiState.update { it.copy(selectedVariantStrokes = strokes) }
                }
            }
        }
    }

    fun loadVariantPreview(variantId: String) {
        viewModelScope.launch {
            val strokes = repository.getVariantStrokes(variantId)
            _uiState.update { it.copy(selectedVariantStrokes = strokes) }
        }
    }

    fun setFavoriteVariant(glyphId: String, variantId: String) {
        viewModelScope.launch {
            val success = repository.setFavoriteVariant(glyphId, variantId)
            if (success) {
                val strokes = repository.getVariantStrokes(variantId)
                _uiState.update {
                    it.copy(
                        selectedVariantStrokes = strokes,
                        feedbackMessage = "Versão definida como favorita!"
                    )
                }
            }
        }
    }

    fun deleteVariant(glyphId: String, variantId: String) {
        viewModelScope.launch {
            val success = repository.deleteVariant(glyphId, variantId)
            if (success) {
                _uiState.update {
                    it.copy(
                        selectedVariantStrokes = emptyList(),
                        feedbackMessage = "Versão excluída."
                    )
                }
            }
        }
    }

    fun addVariant(
        glyphId: String,
        strokes: List<Stroke>,
        score: Float,
        slantAngle: Float,
        setFavorite: Boolean = true
    ) {
        viewModelScope.launch {
            val variant = repository.addVariant(
                glyphId = glyphId,
                strokes = strokes,
                score = score,
                slantAngle = slantAngle,
                setFavorite = setFavorite
            )
            _uiState.update {
                it.copy(
                    selectedVariantStrokes = strokes,
                    feedbackMessage = "Nova versão (${variant.label}) gravada!"
                )
            }
        }
    }

    fun compilePersonalStyle(customName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompilingStyle = true) }
            try {
                val style = repository.compileAndSavePersonalStyle(customName)
                // Registra o novo estilo pessoal no StyleEngine para disponibilidade imediata no Caderno e Treino
                styleEngine.registerCustomStyle(style)

                _uiState.update {
                    it.copy(
                        isCompilingStyle = false,
                        lastCompiledStyle = style,
                        compilationSuccessDialogVisible = true,
                        feedbackMessage = "Estilo pessoal '${style.name}' compilado com sucesso!"
                    )
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isCompilingStyle = false,
                        feedbackMessage = "Erro ao compilar estilo: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun dismissCompilationDialog() {
        _uiState.update { it.copy(compilationSuccessDialogVisible = false) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
