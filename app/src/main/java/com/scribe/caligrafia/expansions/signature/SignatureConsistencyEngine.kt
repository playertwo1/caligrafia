package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import kotlin.math.hypot

/**
 * Motor determinístico de cálculo de métricas e repetibilidade de assinaturas.
 * 100% offline, local e fundamentado em cinemática neuromotora.
 */
object SignatureConsistencyEngine {

    fun computeMetrics(strokes: List<Stroke>): SignatureMetrics {
        if (strokes.isEmpty()) {
            return SignatureMetrics(
                strokeCount = 0,
                durationMs = 0L,
                averageSpeedPxPerMs = 0f,
                totalLengthPx = 0f,
                aspectRatio = 1f,
                penUpCount = 0
            )
        }

        val strokeCount = strokes.size
        val firstStart = strokes.first().startedAtMs
        val lastEnd = strokes.maxOf { it.endedAtMs }.coerceAtLeast(firstStart)
        val durationMs = (lastEnd - firstStart).coerceAtLeast(1L)

        var totalLength = 0f
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (stroke in strokes) {
            val pts = stroke.points
            for (i in pts.indices) {
                val p = pts[i]
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y

                if (i > 0) {
                    val prev = pts[i - 1]
                    totalLength += hypot(p.x - prev.x, p.y - prev.y)
                }
            }
        }

        var netDx = 0f
        var netDy = 0f
        var signedArea = 0.0
        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.size >= 2) {
                netDx += pts.last().x - pts.first().x
                netDy += pts.last().y - pts.first().y
                for (i in 0 until pts.size - 1) {
                    signedArea += (pts[i].x * pts[i + 1].y - pts[i + 1].x * pts[i].y).toDouble()
                }
            }
        }
        val dominantAngle = if (netDx != 0f || netDy != 0f) {
            val rad = kotlin.math.atan2(netDy.toDouble(), netDx.toDouble())
            var deg = Math.toDegrees(rad).toFloat()
            if (deg < 0) deg += 360f
            deg
        } else {
            0f
        }
        val windingSign = if (kotlin.math.abs(signedArea) > 50.0) {
            if (signedArea > 0) 1f else -1f
        } else {
            0f
        }

        val w = if (maxX >= minX) (maxX - minX).coerceAtLeast(1f) else 1f
        val h = if (maxY >= minY) (maxY - minY).coerceAtLeast(1f) else 1f
        val aspectRatio = w / h
        val speed = totalLength / durationMs.toFloat()
        val penUpCount = (strokeCount - 1).coerceAtLeast(0)

        return SignatureMetrics(
            strokeCount = strokeCount,
            durationMs = durationMs,
            averageSpeedPxPerMs = speed,
            totalLengthPx = totalLength,
            aspectRatio = aspectRatio,
            penUpCount = penUpCount,
            dominantAngleDegrees = dominantAngle,
            windingSign = windingSign
        )
    }

    fun evaluateConsistency(
        baseline: SignatureMetrics,
        attempt: SignatureMetrics
    ): SignatureConsistencyReport {
        if (baseline.strokeCount == 0 || attempt.strokeCount == 0) {
            return SignatureConsistencyReport(
                repeatabilityScore = 0f,
                isConsistent = false,
                strokeCountMatch = false,
                durationVariationPercent = 100f,
                speedRatio = 0f,
                aspectVariationPercent = 100f,
                feedbackTitle = "Assinatura incompleta",
                feedbackDetails = "Desenhe a assinatura completa na linha de base para calibrar.",
                ergonomicTip = "Apoie confortavelmente a palma da mão na tela do S25 Ultra antes de iniciar o traço."
            )
        }

        // 1. Concordância na quantidade de traços
        val strokeDiff = kotlin.math.abs(baseline.strokeCount - attempt.strokeCount)
        val strokeScore = when (strokeDiff) {
            0 -> 100f
            1 -> 75f
            2 -> 50f
            else -> 20f
        }
        val strokeMatch = strokeDiff == 0

        // 2. Variação de duração temporal
        val dMin = kotlin.math.min(baseline.durationMs, attempt.durationMs).toFloat()
        val dMax = kotlin.math.max(baseline.durationMs, attempt.durationMs).toFloat().coerceAtLeast(1f)
        val durationRatio = dMin / dMax
        val durationScore = (durationRatio * 100f).coerceIn(0f, 100f)
        val durationVariationPercent = ((1f - durationRatio) * 100f)

        // 3. Proporção geométrica (Aspect Ratio)
        val aMin = kotlin.math.min(baseline.aspectRatio, attempt.aspectRatio)
        val aMax = kotlin.math.max(baseline.aspectRatio, attempt.aspectRatio).coerceAtLeast(0.01f)
        val aspectScore = ((aMin / aMax) * 100f).coerceIn(0f, 100f)
        val aspectVariationPercent = ((1f - (aMin / aMax)) * 100f)

        // 4. Velocidade média de traçado
        val sMin = kotlin.math.min(baseline.averageSpeedPxPerMs, attempt.averageSpeedPxPerMs)
        val sMax = kotlin.math.max(baseline.averageSpeedPxPerMs, attempt.averageSpeedPxPerMs).coerceAtLeast(0.001f)
        val speedRatio = if (baseline.averageSpeedPxPerMs > 0) attempt.averageSpeedPxPerMs / baseline.averageSpeedPxPerMs else 1f
        val speedScore = ((sMin / sMax) * 100f).coerceIn(0f, 100f)

        // 5. Alinhamento angular da trajetória líquida (S16)
        val angleDiff = kotlin.math.abs(baseline.dominantAngleDegrees - attempt.dominantAngleDegrees)
        val normalizedAngleDiff = if (angleDiff > 180f) 360f - angleDiff else angleDiff
        val angleScore = (100f - normalizedAngleDiff * (100f / 90f)).coerceIn(0f, 100f)

        // 6. Alinhamento de sentido de rotação/trajetória (winding order)
        val isOppositeWinding = baseline.windingSign != 0f && attempt.windingSign != 0f && (baseline.windingSign * attempt.windingSign < 0)

        // Score ponderado final
        val rawScore = (
            strokeScore * 0.25f +
            durationScore * 0.20f +
            aspectScore * 0.20f +
            speedScore * 0.15f +
            angleScore * 0.20f
        ).coerceIn(0f, 100f)

        val repeatabilityScore = if (isOppositeWinding) {
            (rawScore * 0.35f).coerceAtMost(35f)
        } else {
            rawScore
        }

        val isConsistent = repeatabilityScore >= 75f && normalizedAngleDiff <= 35f && !isOppositeWinding

        val title: String
        val details: String
        val tip: String

        when {
            isOppositeWinding -> {
                title = "Sentido de Traçado Invertido"
                details = "A trajetória da assinatura foi percorrida no sentido inverso ao da referência gravada."
                tip = "Mantenha o mesmo sentido natural de escrita dos contornos e floreios."
            }
            normalizedAngleDiff > 35f -> {
                title = "Direção e Orientação Inconsistentes"
                details = "A orientação geométrica dos traços diverge da referência (desvio angular de ${normalizedAngleDiff.toInt()}°)."
                tip = "Atente para a inclinação e direção natural do traçado da assinatura."
            }
            repeatabilityScore >= 90f -> {
                title = "Excelente Consistência Geométrica!"
                details = "Sua assinatura apresenta ritmo, proporção e velocidade de traçado altamente consistentes com a referência gravada."
                tip = "Mantenha esse mesmo ponto de apoio no punho para garantir estabilidade do traço."
            }
            repeatabilityScore >= 75f -> {
                title = "Assinatura Consistente"
                details = "Boa estabilidade de traçado com pequenas oscilações de tempo ou proporção."
                tip = "Tente executar os floreios finais com velocidade uniforme, sem hesitar na saída da S Pen."
            }
            !strokeMatch -> {
                title = "Variação na Contagem de Traços"
                details = "A referência possui ${baseline.strokeCount} traço(s), mas esta tentativa registrou ${attempt.strokeCount}."
                tip = "Verifique se a caneta foi levantada involuntariamente no meio do monograma."
            }
            aspectVariationPercent > 35f -> {
                title = "Distorção de Proporção"
                details = "A assinatura ficou significativamente mais comprimida ou esticada que a referência."
                tip = "Observe a linha-guia inferior e as marcações de limites para calibrar a largura."
            }
            else -> {
                title = "Ritmo Oscilante"
                details = "Houve variação perceptível na velocidade de execução em relação à sua referência."
                tip = "Assine com o movimento vindo do cotovelo e ombro, evitando contrair em excesso os dedos sobre a caneta."
            }
        }

        return SignatureConsistencyReport(
            repeatabilityScore = repeatabilityScore,
            isConsistent = isConsistent,
            strokeCountMatch = strokeMatch,
            durationVariationPercent = durationVariationPercent,
            speedRatio = speedRatio,
            aspectVariationPercent = aspectVariationPercent,
            feedbackTitle = title,
            feedbackDetails = details,
            ergonomicTip = tip
        )
    }
}
