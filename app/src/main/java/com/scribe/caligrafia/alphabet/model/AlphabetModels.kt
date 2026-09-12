package com.scribe.caligrafia.alphabet.model

import com.scribe.caligrafia.core.model.Stroke

/**
 * Categoria dos glifos do Alfabeto Pessoal (M6 — Meu Alfabeto).
 */
enum class AlphabetCategory(val id: String, val displayName: String) {
    LOWERCASE("lowercase", "Minúsculas (a-z)"),
    UPPERCASE("uppercase", "Maiúsculas (A-Z)"),
    NUMBER("number", "Números (0-9)"),
    CONNECTOR("connector", "Conexões e Símbolos")
}

/**
 * Variante individual registrada para um glifo específico (v1, v2, v3...).
 *
 * @param id Identificador universal da variante.
 * @param glyphId Identificador do glifo pai (ex: "glyph_lower_a").
 * @param version Número sequencial da versão (1, 2, 3...).
 * @param label Rótulo amigável (ex: "v1", "v2").
 * @param score Pontuação de precisão/qualidade geométrica calculada (0 a 100%).
 * @param slantAngleDegrees Ângulo de inclinação observado nos traços.
 * @param isFavorite Indica se esta versão foi eleita como a variante padrão do glifo.
 * @param strokes Traços vetoriais brutos imutáveis que compõem a escrita da variante.
 * @param strokeCount Quantidade total de traços do traçado.
 * @param createdAtTimestamp Timestamp em milissegundos do registro.
 * @param sourceAttemptId ID opcional da tentativa de treino que originou esta variante.
 */
data class GlyphVariant(
    val id: String,
    val glyphId: String,
    val version: Int,
    val label: String,
    val score: Float,
    val slantAngleDegrees: Float,
    val isFavorite: Boolean = false,
    val strokes: List<Stroke> = emptyList(),
    val strokeCount: Int = strokes.size,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val sourceAttemptId: String? = null
)

/**
 * Representação de um caractere ou símbolo caligráfico no Alfabeto Pessoal.
 *
 * @param id Identificador único (ex: "glyph_lower_a", "glyph_upper_A", "glyph_digit_0").
 * @param symbol Caractere textual visível (ex: "a", "A", "0", "it").
 * @param name Nome pedagógico do caractere (ex: "Letra a", "Dígito 0").
 * @param category Categoria caligráfica à qual o glifo pertence.
 * @param selectedVariantId ID da variante ativa selecionada como padrão.
 * @param variants Lista de todas as variantes gravadas pelo usuário.
 * @param bestScore Maior pontuação geométrica obtida entre as variantes.
 * @param updatedAtTimestamp Timestamp de última alteração.
 */
data class PersonalGlyph(
    val id: String,
    val symbol: String,
    val name: String,
    val category: AlphabetCategory,
    val selectedVariantId: String? = null,
    val variants: List<GlyphVariant> = emptyList(),
    val bestScore: Float = variants.maxOfOrNull { it.score } ?: 0f,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = variants.isNotEmpty()

    val variantCount: Int
        get() = variants.size

    val activeVariant: GlyphVariant?
        get() = variants.firstOrNull { it.id == selectedVariantId }
            ?: variants.firstOrNull { it.isFavorite }
            ?: variants.lastOrNull()
}

/**
 * Estatísticas acumuladas por categoria de caracteres.
 */
data class AlphabetCategoryStats(
    val category: AlphabetCategory,
    val totalGlyphs: Int,
    val completedGlyphs: Int,
    val completionPercentage: Float,
    val averageScore: Float
)

/**
 * Catálogo consolidado do Alfabeto Pessoal do usuário.
 *
 * @param id Identificador do catálogo.
 * @param name Nome atribuído pelo usuário.
 * @param glyphs Mapa indexado de glifos por ID.
 * @param lastCompiledStyleId ID do último ScribeStyle compilado a partir deste alfabeto.
 * @param lastCompiledTimestamp Timestamp da última compilação.
 */
data class PersonalAlphabet(
    val id: String = "my_personal_alphabet",
    val name: String = "Meu Alfabeto",
    val glyphs: Map<String, PersonalGlyph> = emptyMap(),
    val lastCompiledStyleId: String? = null,
    val lastCompiledTimestamp: Long? = null
) {
    val totalGlyphsCount: Int
        get() = glyphs.size

    val completedGlyphsCount: Int
        get() = glyphs.values.count { it.isCompleted }

    val completionPercentage: Float
        get() = if (totalGlyphsCount > 0) {
            (completedGlyphsCount.toFloat() / totalGlyphsCount.toFloat()) * 100f
        } else 0f

    val averageScore: Float
        get() {
            val completed = glyphs.values.filter { it.isCompleted }
            return if (completed.isNotEmpty()) {
                completed.map { it.bestScore }.average().toFloat()
            } else 0f
        }

    fun statsFor(category: AlphabetCategory): AlphabetCategoryStats {
        val catGlyphs = glyphs.values.filter { it.category == category }
        val total = catGlyphs.size
        val completed = catGlyphs.count { it.isCompleted }
        val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
        val avg = if (completed > 0) {
            catGlyphs.filter { it.isCompleted }.map { it.bestScore }.average().toFloat()
        } else 0f
        return AlphabetCategoryStats(category, total, completed, pct, avg)
    }
}
