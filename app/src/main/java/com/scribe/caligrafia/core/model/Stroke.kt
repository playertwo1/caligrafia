package com.scribe.caligrafia.core.model

/**
 * Representação imutável de um traço contínuo completo entre ACTION_DOWN e ACTION_UP/CANCEL.
 *
 * @param id Identificador único universal do traço.
 * @param tool Ferramenta utilizada para produzir o traço (STYLUS, ERASER, FINGER, MOUSE, etc.).
 * @param points Lista imutável de pontos cronologicamente ordenados (incluindo amostras históricas).
 * @param startedAtMs Timestamp em milissegundos do início do traço (ACTION_DOWN).
 * @param endedAtMs Timestamp em milissegundos da conclusão do traço (ACTION_UP ou ACTION_CANCEL).
 * @param isCancelled Indica se o traço foi interrompido/cancelado pelo sistema operacional.
 */
data class Stroke(
    val id: String,
    val tool: ToolType,
    val points: List<StrokePoint>,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val isCancelled: Boolean = false,
    val color: Int? = null,
    val baseWidthPx: Float? = null
) {
    val durationMs: Long
        get() = (endedAtMs - startedAtMs).coerceAtLeast(0L)

    val pointCount: Int
        get() = points.size
}
