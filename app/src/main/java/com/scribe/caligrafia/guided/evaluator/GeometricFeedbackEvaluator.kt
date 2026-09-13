package com.scribe.caligrafia.guided.evaluator

import com.scribe.caligrafia.core.model.GuidelineBand
import com.scribe.caligrafia.core.model.SlantConfig
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.guided.model.DirectionMetric
import com.scribe.caligrafia.guided.model.FeedbackEvaluation
import com.scribe.caligrafia.guided.model.GuidelineMetric
import com.scribe.caligrafia.guided.model.ProximityMetric
import com.scribe.caligrafia.guided.model.ReferenceGlyph
import com.scribe.caligrafia.guided.model.ReferencePoint
import com.scribe.caligrafia.guided.model.SlantMetric
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Motor de Avaliação Geométrica 100% Determinístico para caligrafia.
 *
 * Não utiliza IA, rede neural ou heurísticas opacas. Realiza análise vetorial
 * pura comparando os pontos reais capturados com as pautas e o glifo de referência.
 */
object GeometricFeedbackEvaluator {

    /**
     * Avalia uma tentativa do usuário contra o glifo de referência na pauta ativa.
     *
     * @param userStrokes Lista de traços desenhados pelo usuário.
     * @param reference Glifo canônico sendo praticado.
     * @param band Faixa de pauta caligráfica ativa no canvas.
     * @param originX Posição horizontal onde o glifo foi projetado.
     * @param glyphWidthPx Largura em pixels reservada para o glifo.
     * @param slant Configuração de inclinação da pauta (opcional).
     */
    fun evaluate(
        userStrokes: List<Stroke>,
        reference: ReferenceGlyph,
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float,
        slant: SlantConfig?
    ): FeedbackEvaluation {
        if (userStrokes.isEmpty()) {
            return emptyEvaluation()
        }

        val allUserPoints = userStrokes.flatMap { it.points }
        if (allUserPoints.isEmpty()) {
            return emptyEvaluation()
        }

        // 1. Mapeamento de todos os pontos de referência para coordenadas reais de tela
        val mappedRefStrokes = reference.strokes.map { refStroke ->
            refStroke.points.map { pt ->
                mapToScreen(pt, band, originX, glyphWidthPx)
            }
        }
        val allMappedRefPoints = mappedRefStrokes.flatten()

        // 2. Avaliação de Limites de Pauta (Guideline Adherence)
        val guidelineMetric = evaluateGuidelines(allUserPoints, allMappedRefPoints, band)

        // 3. Avaliação de Inclinação (Slant Adherence)
        val slantMetric = evaluateSlant(userStrokes, slant)

        // 4. Avaliação de Ordem e Sentido dos Traços (Direction Adherence)
        val directionMetric = evaluateDirection(userStrokes, mappedRefStrokes)

        // 5. Avaliação de Proximidade Geométrica (Path Proximity)
        val proximityMetric = evaluateProximity(allUserPoints, mappedRefStrokes, band.xHeight)

        // 6. Avaliação de Cobertura / Completude (A12 / R14: independente da taxa de amostragem)
        val coverageRadius = band.xHeight * 0.25f
        val coveredCount = allMappedRefPoints.count { refPt ->
            userStrokes.any { stroke ->
                val pts = stroke.points
                when {
                    pts.isEmpty() -> false
                    pts.size == 1 -> distance(refPt.x, refPt.y, pts[0].x, pts[0].y) <= coverageRadius
                    else -> {
                        (0 until pts.size - 1).any { i ->
                            pointToSegmentDistance(
                                refPt.x, refPt.y,
                                pts[i].x, pts[i].y,
                                pts[i + 1].x, pts[i + 1].y
                            ) <= coverageRadius
                        }
                    }
                }
            }
        }
        val coverageRatio = if (allMappedRefPoints.isNotEmpty()) {
            coveredCount.toFloat() / allMappedRefPoints.size
        } else 1.0f

        // 7. Cálculo da Pontuação Ponderada Global (0 a 100%) modulada pela completude
        val rawScore = (
            guidelineMetric.scorePercent * 0.30f +
            slantMetric.scorePercent * 0.25f +
            directionMetric.scorePercent * 0.20f +
            proximityMetric.scorePercent * 0.25f
        ).roundToInt().coerceIn(0, 100)

        val totalScore = (rawScore * coverageRatio).roundToInt().coerceIn(0, 100)

        // 8. Compilação das mensagens pedagógicas
        val feedbackMessages = mutableListOf<String>()
        if (coverageRatio < 0.65f) {
            feedbackMessages.add("Traço incompleto (${(coverageRatio * 100).roundToInt()}% percorrido). Complete todo o desenho do modelo.")
        }
        feedbackMessages.add(guidelineMetric.feedback)
        feedbackMessages.add(slantMetric.feedback)
        feedbackMessages.add(directionMetric.feedback)
        feedbackMessages.add(proximityMetric.feedback)

        // Determinação de aprovação: nota >= 70% e todas as dimensões com nota >= 60%
        val isPassed = totalScore >= 70 &&
            guidelineMetric.scorePercent >= 55 &&
            slantMetric.scorePercent >= 55 &&
            directionMetric.scorePercent >= 55 &&
            proximityMetric.scorePercent >= 55 &&
            coverageRatio >= 0.65f

        return FeedbackEvaluation(
            scorePercent = totalScore,
            guideline = guidelineMetric,
            slant = slantMetric,
            direction = directionMetric,
            proximity = proximityMetric,
            feedbackMessages = feedbackMessages
        )
    }

    /**
     * Converte um [ReferencePoint] normalizado para coordenadas de tela em pixels.
     */
    fun mapToScreen(
        pt: ReferencePoint,
        band: GuidelineBand,
        originX: Float,
        glyphWidthPx: Float
    ): ScreenPoint {
        val screenX = originX + pt.xRatio * glyphWidthPx
        val screenY = band.baselineY - (pt.yRatio * band.xHeight)
        return ScreenPoint(screenX, screenY)
    }

    private fun evaluateGuidelines(
        userPoints: List<StrokePoint>,
        refPoints: List<ScreenPoint>,
        band: GuidelineBand
    ): GuidelineMetric {
        val minY = userPoints.minOf { it.y }
        val maxY = userPoints.maxOf { it.y }

        val refMinY = refPoints.minOf { it.y }
        val refMaxY = refPoints.maxOf { it.y }

        // Tolerância de 12% da altura-x
        val tolerance = band.xHeight * 0.12f

        val topOvershoot = max(0f, (refMinY - tolerance) - minY)
        val bottomOvershoot = max(0f, maxY - (refMaxY + tolerance))

        val totalOvershoot = topOvershoot + bottomOvershoot
        val penalty = (totalOvershoot / (band.xHeight * 0.05f)) * 5f
        val score = (100f - penalty).roundToInt().coerceIn(0, 100)

        val feedback = when {
            topOvershoot > 0f && bottomOvershoot > 0f ->
                "Ultrapassou a pauta no topo (+${topOvershoot.roundToInt()}px) e na base (+${bottomOvershoot.roundToInt()}px)."
            topOvershoot > 0f ->
                "Atenção à guia superior: ultrapassou em ${topOvershoot.roundToInt()}px."
            bottomOvershoot > 0f ->
                "Atenção à linha de base: ultrapassou em ${bottomOvershoot.roundToInt()}px."
            else ->
                "Excelente respeito às pautas caligráficas."
        }

        return GuidelineMetric(
            isWithinBounds = totalOvershoot == 0f,
            overshootTopPx = topOvershoot,
            overshootBottomPx = bottomOvershoot,
            scorePercent = score,
            feedback = feedback
        )
    }

    private fun evaluateSlant(
        userStrokes: List<Stroke>,
        slantConfig: SlantConfig?
    ): SlantMetric {
        val targetAngle = slantConfig?.angleDegrees ?: 90.0f
        var totalWeightedAngle = 0.0
        var totalWeight = 0.0

        for (stroke in userStrokes) {
            val pts = stroke.points
            if (pts.size < 2) continue

            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val dist = sqrt(dx * dx + dy * dy)

                // Apenas segmentos descendentes com comprimento mínimo considerável
                if (dy > 4f && dist > 5f) {
                    val angleRad = atan2(dy.toDouble(), (-dx).toDouble())
                    var angleDeg = Math.toDegrees(angleRad).toFloat()
                    if (angleDeg < 0) angleDeg += 180f
                    if (angleDeg in 20f..160f) {
                        totalWeightedAngle += angleDeg * dist
                        totalWeight += dist
                    }
                }
            }
        }

        if (totalWeight <= 0.0) {
            return SlantMetric(
                targetAngleDegrees = targetAngle,
                measuredAngleDegrees = null,
                angularDeviationDegrees = null,
                scorePercent = 85,
                feedback = "Inclinação neutra / traços majoritariamente curvos."
            )
        }

        val avgAngle = (totalWeightedAngle / totalWeight).toFloat()
        val deviation = abs(avgAngle - targetAngle)
        val score = (100f - deviation * 2.2f).roundToInt().coerceIn(0, 100)

        val feedback = when {
            deviation <= 6.0f ->
                "Paralelismo excelente com as guias (${avgAngle.roundToInt()}° vs ${targetAngle.roundToInt()}°)."
            avgAngle > targetAngle + 6.0f ->
                "Traço muito vertical (${avgAngle.roundToInt()}°). Incline mais para frente (${targetAngle.roundToInt()}°)."
            else ->
                "Traço muito inclinado (${avgAngle.roundToInt()}°). Erga um pouco mais o ângulo (${targetAngle.roundToInt()}°)."
        }

        return SlantMetric(
            targetAngleDegrees = targetAngle,
            measuredAngleDegrees = avgAngle,
            angularDeviationDegrees = deviation,
            scorePercent = score,
            feedback = feedback
        )
    }

    private fun evaluateDirection(
        userStrokes: List<Stroke>,
        refStrokes: List<List<ScreenPoint>>
    ): DirectionMetric {
        val expectedCount = refStrokes.size
        val actualCount = userStrokes.size

        var correctDirections = 0
        val strokesToCheck = min(expectedCount, actualCount)

        for (i in 0 until strokesToCheck) {
            val userStroke = userStrokes[i]
            val refStroke = refStrokes[i]
            if (userStroke.points.size < 2 || refStroke.size < 2) continue

            val userStart = userStroke.points.first()
            val userEnd = userStroke.points.last()
            val refStart = refStroke.first()
            val refEnd = refStroke.last()

            val distStartToStart = distance(userStart.x, userStart.y, refStart.x, refStart.y)
            val distEndToStart = distance(userEnd.x, userEnd.y, refStart.x, refStart.y)

            // Se o início do usuário estiver mais perto do início da referência que o fim, sentido é correto
            if (distStartToStart <= distEndToStart) {
                correctDirections++
            }
        }

        val countDifference = abs(actualCount - expectedCount)
        val countPenalty = countDifference * 15
        val directionScore = if (strokesToCheck > 0) (correctDirections.toFloat() / strokesToCheck * 100f).roundToInt() else 50
        val finalScore = (directionScore - countPenalty).coerceIn(0, 100)

        val isDirectionCorrect = correctDirections == strokesToCheck && countDifference == 0

        val feedback = when {
            isDirectionCorrect ->
                "Sentido e ordem dos traços corretos ($actualCount/$expectedCount)."
            correctDirections < strokesToCheck ->
                "Atenção: traço realizado em sentido inverso ao movimento caligráfico recomendado."
            actualCount < expectedCount ->
                "Traço incompleto ($actualCount de $expectedCount traço(s) desenhado(s))."
            else ->
                "Quantidade de traços maior que o modelo ($actualCount ao invés de $expectedCount)."
        }

        return DirectionMetric(
            isDirectionCorrect = isDirectionCorrect,
            strokesCountExpected = expectedCount,
            strokesCountReceived = actualCount,
            scorePercent = finalScore,
            feedback = feedback
        )
    }

    private fun evaluateProximity(
        userPoints: List<StrokePoint>,
        refStrokes: List<List<ScreenPoint>>,
        xHeight: Float
    ): ProximityMetric {
        val allRefSegments = mutableListOf<Pair<ScreenPoint, ScreenPoint>>()
        for (stroke in refStrokes) {
            for (i in 0 until stroke.size - 1) {
                allRefSegments.add(Pair(stroke[i], stroke[i + 1]))
            }
        }

        if (allRefSegments.isEmpty() || userPoints.isEmpty()) {
            return ProximityMetric(0f, 0f, 50, "Sem dados de proximidade suficientes.")
        }

        var totalDist = 0.0
        var maxDist = 0.0f

        for (uPt in userPoints) {
            var minDistToAnySegment = Float.MAX_VALUE
            for ((p1, p2) in allRefSegments) {
                val d = pointToSegmentDistance(uPt.x, uPt.y, p1.x, p1.y, p2.x, p2.y)
                if (d < minDistToAnySegment) {
                    minDistToAnySegment = d
                }
            }
            totalDist += minDistToAnySegment
            if (minDistToAnySegment > maxDist) {
                maxDist = minDistToAnySegment
            }
        }

        val meanDist = (totalDist / userPoints.size).toFloat()

        // Tolerância de desvio: desvio médio de 10% da altura-x dá nota ~85%
        val relativeMeanError = meanDist / xHeight
        val score = (100f - (relativeMeanError * 150f)).roundToInt().coerceIn(0, 100)

        val feedback = when {
            relativeMeanError <= 0.08f ->
                "Fidelidade de curva e forma excelentes (desvio médio ${meanDist.roundToInt()}px)."
            relativeMeanError <= 0.18f ->
                "Boa aproximação da forma do modelo (desvio médio ${meanDist.roundToInt()}px)."
            else ->
                "Curvatura com desvio em relação ao gabarito (desvio médio ${meanDist.roundToInt()}px)."
        }

        return ProximityMetric(
            meanDistancePx = meanDist,
            maxDistancePx = maxDist,
            scorePercent = score,
            feedback = feedback
        )
    }

    private fun emptyEvaluation(): FeedbackEvaluation = FeedbackEvaluation(
        scorePercent = 0,
        guideline = GuidelineMetric(false, 0f, 0f, 0, "Nenhum traço capturado."),
        slant = SlantMetric(52f, null, null, 0, "Nenhum traço capturado."),
        direction = DirectionMetric(false, 1, 0, 0, "Nenhum traço capturado."),
        proximity = ProximityMetric(0f, 0f, 0, "Nenhum traço capturado."),
        feedbackMessages = listOf("Pratique desenhando sobre o modelo para obter feedback.")
    )

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun pointToSegmentDistance(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return distance(px, py, x1, y1)

        val t = (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        return distance(px, py, projX, projY)
    }

    data class ScreenPoint(val x: Float, val y: Float)
}
