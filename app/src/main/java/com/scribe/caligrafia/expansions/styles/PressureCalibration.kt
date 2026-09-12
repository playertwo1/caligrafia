package com.scribe.caligrafia.expansions.styles

import kotlin.math.exp
import kotlin.math.pow

/**
 * Curvas de calibração de sensibilidade e resposta de pressão da S Pen.
 * Permite adaptar a modulação de espessura ao peso natural da mão do calígrafo.
 */
enum class PressureCurveType(
    val displayName: String,
    val description: String
) {
    LINEAR(
        displayName = "Linear (Direto)",
        description = "Resposta 1:1 proporcional aos sensores eletromagnéticos da S Pen."
    ),
    SOFT(
        displayName = "Toque Suave (Pena Flexível)",
        description = "Aumenta a modulação com menor esforço físico. Ideal para calígrafos de traço leve."
    ),
    FIRM(
        displayName = "Toque Firme (Controle Preciso)",
        description = "Exige maior pressão intencional para alargar o traço. Evita sombras acidentais."
    ),
    SIGMOID_CALLIGRAPHIC(
        displayName = "Sigmoide Caligráfico (Alto Contraste)",
        description = "Transição acentuada: subidas finíssimas e descidas com sombra encorpada."
    )
}

object PressureCalibration {

    /**
     * Transforma a pressão bruta reportada pelo hardware [0.0f, 1.0f]
     * de acordo com a curva motora selecionada.
     */
    fun transform(rawPressure: Float, curve: PressureCurveType): Float {
        val p = rawPressure.coerceIn(0f, 1f)
        if (p == 0f || p == 1f) return p

        return when (curve) {
            PressureCurveType.LINEAR -> p

            PressureCurveType.SOFT -> {
                // p^0.60 eleva valores baixos sem estourar o limite
                p.pow(0.60f).coerceIn(0f, 1f)
            }

            PressureCurveType.FIRM -> {
                // p^1.60 mantém valores baixos próximos de zero até atingir pressão firme
                p.pow(1.60f).coerceIn(0f, 1f)
            }

            PressureCurveType.SIGMOID_CALLIGRAPHIC -> {
                // Curva logística normalizada: S(p) = 1 / (1 + exp(-k * (p - x0)))
                val k = 9.0f
                val x0 = 0.50f
                val rawSigmoid = 1.0f / (1.0f + exp(-k * (p - x0)))
                val sig0 = 1.0f / (1.0f + exp(-k * (0f - x0)))
                val sig1 = 1.0f / (1.0f + exp(-k * (1f - x0)))
                val normalized = (rawSigmoid - sig0) / (sig1 - sig0)
                normalized.coerceIn(0f, 1f)
            }
        }
    }
}
