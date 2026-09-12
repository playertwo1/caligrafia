package com.scribe.caligrafia.style.model

/**
 * Comportamento esperado da pressão da stylus ao executar o traço no estilo caligráfico.
 */
enum class PressureBehavior {
    /** Pressão uniforme / monoline: traço mantém espessura constante sem modulação. */
    UNIFORM,

    /** Contraste clássico: descidas com pressão pesada (downstroke) e subidas capilares leves (upstroke). */
    DOWNSTROKE_HEAVY,

    /** Spencerian / ornamental: traço predominantemente leve com sombras pontuais em capitulares e extensões. */
    SHADED_ACCENT
}

/**
 * Regra elementar de ductus (direção, ritmo e pressão do movimento caligráfico).
 *
 * @param ruleIndex Índice sequencial pedagógico da regra (1, 2, 3...).
 * @param title Nome descritivo da regra caligráfica.
 * @param instruction Orientação detalhada em português sobre a execução motora do traço.
 * @param pressureBehavior Modulação de pressão exigida pelo estilo.
 */
data class DuctusRule(
    val ruleIndex: Int,
    val title: String,
    val instruction: String,
    val pressureBehavior: PressureBehavior = PressureBehavior.UNIFORM
)
