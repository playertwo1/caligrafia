package com.scribe.caligrafia.expansions.passage

/**
 * Categorias temáticas de textos para prática contínua de caligrafia.
 */
enum class PassageCategory(val displayName: String) {
    PANGRAMS("Pangramas Canônicos"),
    CLASSIC_POETRY("Poesia Clássica"),
    FAMOUS_QUOTES("Citações & Filosofia"),
    CUSTOM("Texto Personalizado")
}

/**
 * Entidade de texto estruturado para treino de frases e parágrafos.
 */
data class PassageItem(
    val id: String,
    val title: String,
    val author: String,
    val category: PassageCategory,
    val lines: List<String>,
    val recommendedStyleId: String = "cursiva_escolar_br",
    val targetWpm: Int = 14
) {
    val totalWordCount: Int
        get() = lines.sumOf { line ->
            line.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
        }
}

/**
 * Resultado do cálculo de ritmo, WPM e cadência motora na cópia de texto.
 */
data class PassagePacingResult(
    val totalWords: Int,
    val durationMs: Long,
    val actualWpm: Float,
    val targetWpm: Int,
    val pacingScore: Float, // 0.0f a 100.0f
    val diagnosis: String,
    val recommendation: String
)
