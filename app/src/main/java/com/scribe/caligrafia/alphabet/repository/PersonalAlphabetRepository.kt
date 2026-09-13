package com.scribe.caligrafia.alphabet.repository

import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.style.model.ScribeStyle
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositório para o gerenciamento do Alfabeto Pessoal e suas variantes (M6).
 */
interface PersonalAlphabetRepository {
    /**
     * Fluxo reativo do catálogo do alfabeto pessoal.
     */
    fun getAlphabet(): Flow<PersonalAlphabet>

    /**
     * Obtém um glifo específico pelo seu identificador.
     */
    suspend fun getGlyph(glyphId: String): PersonalGlyph?

    /**
     * Carrega os traços vetoriais brutos de uma variante a partir do arquivo .scribe associado.
     */
    suspend fun getVariantStrokes(variantId: String): List<Stroke>

    /**
     * Registra uma nova versão/variante para um glifo.
     */
    suspend fun addVariant(
        glyphId: String,
        strokes: List<Stroke>,
        score: Float,
        slantAngle: Float,
        setFavorite: Boolean = true,
        sourceAttemptId: String? = null
    ): GlyphVariant

    /**
     * Define uma variante específica como favorita/ativa de um glifo.
     */
    suspend fun setFavoriteVariant(glyphId: String, variantId: String): Boolean

    /**
     * F3.15: Renomeia o rótulo da variante.
     */
    suspend fun renameVariant(glyphId: String, variantId: String, newLabel: String): Boolean

    /**
     * F3.15 & F3.16: Duplica uma variante existente sem criar novo treino nem amostra independente para métricas pessoais.
     */
    suspend fun duplicateVariant(glyphId: String, variantId: String): GlyphVariant?

    /**
     * Remove uma variante gravada.
     */
    suspend fun deleteVariant(glyphId: String, variantId: String): Boolean

    /**
     * Compila o PersonalStyle a partir das variantes favoritas e atualiza o manifesto.
     */
    suspend fun compileAndSavePersonalStyle(styleName: String? = null): ScribeStyle
}
