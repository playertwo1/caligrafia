package com.scribe.caligrafia.guided.model

/**
 * Métrica de respeito aos limites da pauta caligráfica (linha de base, altura-x, ascender e descender).
 */
data class GuidelineMetric(
    val isWithinBounds: Boolean,
    val overshootTopPx: Float,
    val overshootBottomPx: Float,
    val scorePercent: Int,
    val feedback: String
)

/**
 * Métrica de aderência à inclinação canônica (slant) da caligrafia.
 */
data class SlantMetric(
    val targetAngleDegrees: Float,
    val measuredAngleDegrees: Float?,
    val angularDeviationDegrees: Float?,
    val scorePercent: Int,
    val feedback: String
)

/**
 * Métrica de quantidade, ordem e direção dos traços caligráficos.
 */
data class DirectionMetric(
    val isDirectionCorrect: Boolean,
    val strokesCountExpected: Int,
    val strokesCountReceived: Int,
    val scorePercent: Int,
    val feedback: String
)

/**
 * Métrica de proximidade espacial média entre o traçado do aluno e a referência.
 */
data class ProximityMetric(
    val meanDistancePx: Float,
    val maxDistancePx: Float,
    val scorePercent: Int,
    val feedback: String
)

/**
 * Avaliação matemática determinística completa de uma tentativa de escrita caligráfica.
 *
 * @param scorePercent Pontuação global ponderada (0 a 100%).
 * @param guideline Limites e posicionamento vertical nas linhas de pauta.
 * @param slant Paralelismo e uniformidade angular.
 * @param direction Quantidade e sentido de execução dos traços.
 * @param proximity Proximidade geométrica do caminho de referência.
 * @param feedbackMessages Lista formatada de recomendações pedagógicas imediatas.
 */
data class FeedbackEvaluation(
    val scorePercent: Int,
    val guideline: GuidelineMetric,
    val slant: SlantMetric,
    val direction: DirectionMetric,
    val proximity: ProximityMetric,
    val feedbackMessages: List<String>
) {
    val isMastered: Boolean
        get() = scorePercent >= 85

    val isPassed: Boolean
        get() = scorePercent >= 65

    val gradeBadge: String
        get() = when {
            scorePercent >= 90 -> "Excelente"
            scorePercent >= 75 -> "Muito Bom"
            scorePercent >= 60 -> "Bom Progresso"
            else -> "Continue Praticando"
        }
}
