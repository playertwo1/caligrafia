package com.scribe.caligrafia.core.model

import android.graphics.Color

/**
 * Modo primário da ferramenta ativa na interface.
 */
enum class ToolMode {
    PEN,
    ERASER
}

/**
 * Predefinições de espessura de traço caligráfico calibradas em pixels/dp.
 */
enum class PenThickness(val widthPx: Float, val label: String) {
    FINE(2.5f, "Fina"),
    MEDIUM(5.0f, "Média"),
    BROAD(8.5f, "Grossa")
}

/**
 * Cores caligráficas clássicas para treino e escrita artística.
 */
enum class CalligraphyColor(val argb: Int, val displayName: String) {
    NANQUIM(Color.rgb(15, 23, 42), "Nanquim"),       // Slate 900
    SEPIA(Color.rgb(74, 46, 24), "Sépia"),          // Earth warm brown
    ROYAL_BLUE(Color.rgb(30, 58, 138), "Azul Real"), // Deep classic blue
    BURGUNDY(Color.rgb(131, 24, 67), "Vinho"),      // Elegant burgundy
    GRAPHITE(Color.rgb(71, 85, 105), "Grafite"),    // Slate 600
    FOREST_GREEN(Color.rgb(20, 83, 45), "Verde")    // Forest green
}

/**
 * Modo de operação da borracha.
 */
enum class EraserMode(val defaultRadiusPx: Float, val label: String) {
    STROKE(24f, "Por Traço"),
    PRECISION(16f, "Precisão")
}

/**
 * Estado agregado da ferramenta de desenho e escrita atual.
 */
data class ToolConfig(
    val mode: ToolMode = ToolMode.PEN,
    val penThickness: PenThickness = PenThickness.MEDIUM,
    val customStrokeWidthPx: Float? = null,
    val color: CalligraphyColor = CalligraphyColor.NANQUIM,
    val eraserMode: EraserMode = EraserMode.STROKE,
    val eraserRadiusPx: Float = 24f
) {
    val activeStrokeWidthPx: Float
        get() = customStrokeWidthPx ?: penThickness.widthPx

    val activeColorArgb: Int
        get() = color.argb
}
