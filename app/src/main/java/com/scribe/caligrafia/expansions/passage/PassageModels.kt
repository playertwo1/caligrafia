package com.scribe.caligrafia.expansions.passage

import com.scribe.caligrafia.core.model.Stroke
import java.util.UUID

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

/**
 * Registro persistido de uma cópia de texto com texto vinculado, strokes e tempo real (F4.14, F4.15).
 * Mede velocidade (WPM), cadência e strokes reais sem alegações infundadas de OCR textual.
 */
data class PassageCopyRecord(
    val id: String = UUID.randomUUID().toString(),
    val textId: String,
    val title: String,
    val author: String,
    val textContent: String,
    val styleId: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val strokeCount: Int,
    val pageCount: Int = 1,
    val strokesByPage: Map<Int, List<Stroke>> = emptyMap(),
    val isCompleted: Boolean = false,
    val actualWpm: Float = 0f,
    val targetWpm: Int = 14
)

/**
 * Sessão ativa de cópia de texto no canvas do Caderno (F4.12, F4.13).
 */
data class ActiveTextCopySession(
    val passage: PassageItem,
    val styleId: String,
    val isCollapsed: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val recordId: String = UUID.randomUUID().toString(),
    val strokesByPage: Map<Int, List<Stroke>> = emptyMap()
)
