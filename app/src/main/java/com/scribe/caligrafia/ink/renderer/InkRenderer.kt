package com.scribe.caligrafia.ink.renderer

import android.graphics.Canvas
import com.scribe.caligrafia.core.model.Stroke

enum class RendererType(val displayName: String, val description: String) {
    SMOOTHED_REFERENCE(
        displayName = "Referência Nativa (Bézier)",
        description = "Suavização quadrática derivada nos pontos médios com modulação por pressão"
    ),
    RAW_POLYLINE(
        displayName = "Raw Polyline (Direto)",
        description = "Segmentos retos diretos entre amostras reais para análise de latência e geometria"
    ),
    ANDROID_INK_API(
        displayName = "Android Ink API",
        description = "Biblioteca oficial androidx.ink para renderização acelerada de traços"
    )
}

/**
 * Interface comum para motores de renderização no Stylus Lab.
 *
 * Garante que qualquer renderizador:
 * 1. Opere exclusivamente sobre os modelos imutáveis Stroke e StrokePoint.
 * 2. Nunca altere ou sobrescreva a coleção original de pontos brutos.
 * 3. Permita comparação em tempo real no dispositivo-alvo (Galaxy S25 Ultra).
 */
interface InkRenderer {
    val type: RendererType

    val name: String
        get() = type.displayName

    /**
     * Renderiza um traço concluído no Canvas fornecido.
     */
    fun renderStroke(canvas: Canvas, stroke: Stroke)

    /**
     * Renderiza o traço ativo em tempo real enquanto o usuário escreve.
     */
    fun renderActiveStroke(canvas: Canvas, activeStroke: Stroke)

    /**
     * Renderiza todos os traços acumulados da sessão e o traço ativo opcional.
     */
    fun renderAll(canvas: Canvas, strokes: List<Stroke>, activeStroke: Stroke? = null) {
        for (stroke in strokes) {
            renderStroke(canvas, stroke)
        }
        if (activeStroke != null && activeStroke.points.isNotEmpty()) {
            renderActiveStroke(canvas, activeStroke)
        }
    }
}
