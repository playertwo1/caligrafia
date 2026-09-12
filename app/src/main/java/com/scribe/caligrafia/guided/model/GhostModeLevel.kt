package com.scribe.caligrafia.guided.model

/**
 * Níveis de assistência visual do Ghost Mode com redução progressiva de auxílio.
 *
 * @param alpha Fator de transparência (0.0f a 1.0f).
 * @param percentageLabel Rótulo percentual para exibição na interface.
 * @param title Nome pedagógico do nível.
 */
enum class GhostModeLevel(
    val alpha: Float,
    val percentageLabel: String,
    val title: String
) {
    FULL(1.0f, "100%", "Total"),
    CLEAR(0.7f, "70%", "Nítido"),
    FAINT(0.4f, "40%", "Tênue"),
    WATERMARK(0.1f, "10%", "Marca d'Água"),
    OFF(0.0f, "0%", "Oculto");

    val isVisible: Boolean
        get() = alpha > 0.001f

    fun nextLowerLevel(): GhostModeLevel = when (this) {
        FULL -> CLEAR
        CLEAR -> FAINT
        FAINT -> WATERMARK
        WATERMARK -> OFF
        OFF -> OFF
    }

    fun nextHigherLevel(): GhostModeLevel = when (this) {
        FULL -> FULL
        CLEAR -> FULL
        FAINT -> CLEAR
        WATERMARK -> FAINT
        OFF -> WATERMARK
    }
}
