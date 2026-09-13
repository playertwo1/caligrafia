package com.scribe.caligrafia.evolution.engine

import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import kotlin.math.abs

/**
 * Motor de análise comparativa determinística entre tentativas caligráficas (SCR-501).
 *
 * Avalia ganhos objetivos de precisão geométrica, fidelidade angular à pauta e cadência temporal.
 */
object AttemptComparator {

    /**
     * Calcula a velocidade média real do traçado em pixels por milissegundo (px/ms) (R18).
     */
    fun calculateVelocityPxPerMs(attempt: PracticeAttemptRecord): Float {
        var totalLengthPx = 0.0
        for (stroke in attempt.strokes) {
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val dx = pts[i + 1].x - pts[i].x
                val dy = pts[i + 1].y - pts[i].y
                totalLengthPx += kotlin.math.hypot(dx.toDouble(), dy.toDouble())
            }
        }
        val duration = attempt.durationMs.coerceAtLeast(1L)
        return (totalLengthPx / duration).toFloat()
    }

    /**
     * Compara duas tentativas de escrita e sintetiza os deltas objetivos de evolução.
     *
     * @param before Tentativa inicial (baseline ou anterior).
     * @param after Tentativa recente ou atual.
     * @param targetSlantDegrees Ângulo alvo de inclinação (respeita o estilo gravado na tentativa ou fallback de 52.0°).
     */
    fun compare(
        before: PracticeAttemptRecord,
        after: PracticeAttemptRecord,
        targetSlantDegrees: Float = after.targetSlantDegrees ?: before.targetSlantDegrees ?: 52.0f
    ): BeforeAfterComparison {
        val scoreGain = after.scorePercent - before.scorePercent

        val beforeSlantDiff = abs(before.averageSlantDegrees - targetSlantDegrees)
        val afterSlantDiff = abs(after.averageSlantDegrees - targetSlantDegrees)
        val slantImprovement = beforeSlantDiff - afterSlantDiff

        val durationDelta = after.durationMs - before.durationMs

        // R18: Cálculo real de velocidade de percurso em px/ms e ganho percentual de agilidade
        val speedBefore = calculateVelocityPxPerMs(before)
        val speedAfter = calculateVelocityPxPerMs(after)
        val speedGain = if (speedBefore > 0.0001f) {
            ((speedAfter - speedBefore) / speedBefore) * 100f
        } else 0f

        val insight = when {
            scoreGain > 15 && slantImprovement > 2.0f ->
                "Evolução notável: ganho de +$scoreGain% na nota e ângulo muito mais alinhado à pauta de ${targetSlantDegrees.toInt()}°."
            scoreGain > 0 && slantImprovement > 0f ->
                "Progresso contínuo: melhora de +$scoreGain% na consistência e paralelismo das hastes."
            scoreGain > 0 ->
                "Ganho de precisão de +$scoreGain%, mantendo firmeza e cadência na linha de base."
            scoreGain == 0 && slantImprovement > 0f ->
                "Maior disciplina angular: inclinação ajustada em direção ao ângulo ideal de ${targetSlantDegrees.toInt()}°."
            else ->
                "Memória muscular em construção ativa: cada repetição estabiliza a pegada da caneta e o ritmo."
        }

        return BeforeAfterComparison(
            targetId = after.targetId,
            targetTitle = after.targetTitle,
            beforeAttempt = before,
            afterAttempt = after,
            scoreGainPercent = scoreGain,
            slantImprovementDegrees = slantImprovement,
            durationDeltaMs = durationDelta,
            summaryInsight = insight,
            speedBeforePxPerMs = speedBefore,
            speedAfterPxPerMs = speedAfter,
            speedGainPercent = speedGain
        )
    }
}
