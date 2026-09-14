package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import kotlin.math.hypot

/**
 * Motor determinístico de comparação de assinaturas baseado somente em medidas temporais e
 * geométricas disponíveis nos raw strokes. Não autentica identidade e não infere condição muscular.
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
        val firstStart = strokes.minOf { it.startedAtMs }
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
                feedbackTitle = "Dados insuficientes para comparação",
                feedbackDetails = "Desenhe a tentativa completa para comparar medidas geométricas e temporais com a referência.",
                ergonomicTip = "Use uma posição confortável e estável durante a escrita."
            )
        }

        val strokeDiff = kotlin.math.abs(baseline.strokeCount - attempt.strokeCount)
        val strokeScore = when (strokeDiff) {
            0 -> 100f
            1 -> 75f
            2 -> 50f
            else -> 20f
        }
        val strokeMatch = strokeDiff == 0

        val dMin = kotlin.math.min(baseline.durationMs, attempt.durationMs).toFloat()
        val dMax = kotlin.math.max(baseline.durationMs, attempt.durationMs).toFloat().coerceAtLeast(1f)
        val durationRatio = dMin / dMax
        val durationScore = (durationRatio * 100f).coerceIn(0f, 100f)
        val durationVariationPercent = ((1f - durationRatio) * 100f)

        val aMin = kotlin.math.min(baseline.aspectRatio, attempt.aspectRatio)
        val aMax = kotlin.math.max(baseline.aspectRatio, attempt.aspectRatio).coerceAtLeast(0.01f)
        val aspectScore = ((aMin / aMax) * 100f).coerceIn(0f, 100f)
        val aspectVariationPercent = ((1f - (aMin / aMax)) * 100f)

        val sMin = kotlin.math.min(baseline.averageSpeedPxPerMs, attempt.averageSpeedPxPerMs)
        val sMax = kotlin.math.max(baseline.averageSpeedPxPerMs, attempt.averageSpeedPxPerMs).coerceAtLeast(0.001f)
        val speedRatio = if (baseline.averageSpeedPxPerMs > 0) {
            attempt.averageSpeedPxPerMs / baseline.averageSpeedPxPerMs
        } else {
            1f
        }
        val speedScore = ((sMin / sMax) * 100f).coerceIn(0f, 100f)

        val angleDiff = kotlin.math.abs(baseline.dominantAngleDegrees - attempt.dominantAngleDegrees)
        val normalizedAngleDiff = if (angleDiff > 180f) 360f - angleDiff else angleDiff
        val angleScore = (100f - normalizedAngleDiff * (100f / 90f)).coerceIn(0f, 100f)

        val isOppositeWinding = baseline.windingSign != 0f &&
            attempt.windingSign != 0f &&
            (baseline.windingSign * attempt.windingSign < 0)

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
                title = "Sentido de Traçado Diferente"
                details = "A trajetória observada foi percorrida no sentido oposto ao da referência salva."
                tip = "Repita a tentativa buscando o mesmo sentido de percurso dos traços da referência."
            }
            normalizedAngleDiff > 35f -> {
                title = "Orientação Geométrica Diferente"
                details = "A direção líquida dos traços divergiu da referência em ${normalizedAngleDiff.toInt()}°."
                tip = "Use as guias visuais para aproximar a inclinação da referência."
            }
            repeatabilityScore >= 90f -> {
                title = "Alta Repetibilidade das Medidas"
                details = "Contagem de traços, duração, proporção, velocidade média e orientação ficaram próximas da referência."
                tip = "Mantenha condições de escrita semelhantes quando quiser comparar novas tentativas."
            }
            repeatabilityScore >= 75f -> {
                title = "Medidas Consistentes com a Referência"
                details = "As medidas observadas ficaram próximas, com pequenas diferenças de tempo, velocidade ou proporção."
                tip = "Compare novamente após outra tentativa para observar a repetibilidade."
            }
            !strokeMatch -> {
                title = "Variação na Contagem de Traços"
                details = "A referência possui ${baseline.strokeCount} traço(s), mas esta tentativa registrou ${attempt.strokeCount}."
                tip = "Observe onde a caneta foi levantada em relação à referência."
            }
            aspectVariationPercent > 35f -> {
                title = "Variação de Proporção"
                details = "A relação entre largura e altura ficou diferente da referência salva."
                tip = "Use a linha-guia e os limites visuais para comparar escala e proporção."
            }
            else -> {
                title = "Variação de Ritmo Medido"
                details = "A velocidade média ou a duração ficaram diferentes da referência nesta tentativa."
                tip = "Faça outra tentativa em ritmo confortável e compare as medidas novamente."
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
