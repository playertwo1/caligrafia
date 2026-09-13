package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.MaturityLevel
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Motor de Diagnóstico Biomecânico do Professor IA (SCR-701).
 *
 * 100% Determinístico, matemático e offline: avalia estabilidade angular,
 * contenção de pautas, ritmo/cadência motora e modulação de pressão da S Pen.
 */
class MotorDiagnosticEngine(
    private val defaultTargetSlantDegrees: Float = 52.0f
) {

    /**
     * Diagnostica o perfil motor caligráfico a partir de um conjunto de tentativas salvas.
     */
    fun diagnoseAttempts(
        attempts: List<PracticeAttemptRecord>,
        targetSlantDegrees: Float = defaultTargetSlantDegrees
    ): BiomechanicalDiagnostic {
        if (attempts.isEmpty()) {
            return generateEmptyDiagnostic()
        }

        val allStrokes = attempts.flatMap { it.strokes }
        return diagnoseStrokes(allStrokes, attempts, targetSlantDegrees)
    }

    /**
     * Diagnostica diretamente a partir de uma lista de traços vetoriais brutos.
     */
    fun diagnoseStrokes(
        strokes: List<Stroke>,
        attempts: List<PracticeAttemptRecord> = emptyList(),
        targetSlantDegrees: Float = defaultTargetSlantDegrees
    ): BiomechanicalDiagnostic {
        if (strokes.isEmpty()) {
            return generateEmptyDiagnostic()
        }

        val slantEval = evaluateSlantStability(strokes, targetSlantDegrees)
        val containmentEval = evaluateGuidelineContainment(strokes, attempts)
        val rhythmEval = evaluateRhythmAndCadence(strokes)
        val pressureEval = evaluatePressureControl(strokes)

        val dimensions = mapOf(
            BiomechanicalDimension.SLANT_STABILITY to slantEval,
            BiomechanicalDimension.GUIDELINE_CONTAINMENT to containmentEval,
            BiomechanicalDimension.RHYTHM_AND_CADENCE to rhythmEval,
            BiomechanicalDimension.PRESSURE_CONTROL to pressureEval
        )

        // S09: Dimensões não medidas/insuficientes são excluídas da pontuação geral
        val measuredDims = dimensions.values.filter { it.status != EvaluationStatus.INSUFFICIENT_DATA }
        val overallScore = if (measuredDims.isNotEmpty()) {
            measuredDims.map { it.score }.average().toFloat().coerceIn(0f, 100f)
        } else {
            0.0f
        }

        val sortedDims = measuredDims.sortedBy { it.score }
        val primaryWeakness = sortedDims.firstOrNull()?.dimension
        val primaryStrength = sortedDims.lastOrNull()?.dimension

        return BiomechanicalDiagnostic(
            overallScore = overallScore,
            maturityLevel = MaturityLevel.fromScore(overallScore),
            dimensions = dimensions,
            primaryWeakness = primaryWeakness,
            primaryStrength = primaryStrength,
            totalStrokesAnalyzed = strokes.size,
            totalAttemptsAnalyzed = attempts.size
        )
    }

    // ==========================================
    // 1. Estabilidade Angular (Slant Stability)
    // ==========================================
    private fun evaluateSlantStability(
        strokes: List<Stroke>,
        targetSlantDegrees: Float
    ): DimensionEvaluation {
        var totalWeightedAngle = 0.0
        var totalWeight = 0.0
        val weightedAngles = mutableListOf<Pair<Float, Float>>()

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.size < 2) continue

            var i = 0
            while (i < pts.size - 1) {
                val p1 = pts[i]
                var nextIdx = i + 1
                while (nextIdx < pts.size - 1) {
                    val dx = pts[nextIdx].x - p1.x
                    val dy = pts[nextIdx].y - p1.y
                    if (sqrt(dx * dx + dy * dy) >= 8.0f) break
                    nextIdx++
                }

                val p2 = pts[nextIdx]
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val dist = sqrt(dx * dx + dy * dy)

                // R11 / S09: Segmentos descendentes expressivos com convenção caligráfica canônica.
                // dy > 0 (descendente); o ângulo caligráfico em relação à linha de base é atan2(dy, -dx)
                if (dy > 3.0f && dist > 4.0f) {
                    val angleRad = atan2(dy.toDouble(), (-dx).toDouble())
                    var angleDeg = Math.toDegrees(angleRad).toFloat()
                    if (angleDeg < 0) angleDeg += 180f
                    if (angleDeg in 20.0f..160.0f) {
                        totalWeightedAngle += angleDeg * dist
                        totalWeight += dist
                        weightedAngles.add(Pair(angleDeg, dist))
                    }
                }
                i = nextIdx
            }
        }

        if (totalWeight <= 0.0 || weightedAngles.isEmpty()) {
            return DimensionEvaluation(
                dimension = BiomechanicalDimension.SLANT_STABILITY,
                score = 0.0f,
                observedValue = null,
                targetValue = targetSlantDegrees,
                status = EvaluationStatus.INSUFFICIENT_DATA,
                shortDiagnosis = "Amostras descendentes insuficientes para cálculo de dispersão angular."
            )
        }

        val meanAngle = (totalWeightedAngle / totalWeight).toFloat()
        val variance = weightedAngles.sumOf { (ang, w) ->
            (ang - meanAngle).toDouble().pow(2) * (w / totalWeight)
        }.toFloat()
        val stdDev = sqrt(variance)
        val angularError = kotlin.math.abs(meanAngle - targetSlantDegrees)

        // Penalidade por desvio padrão (instabilidade) e desvio em relação ao alvo formal
        val score = (100.0f - (stdDev * 5.0f) - (angularError * 2.5f)).coerceIn(0.0f, 100.0f)

        val status = when {
            score >= 85.0f -> EvaluationStatus.EXCELLENT
            score >= 70.0f -> EvaluationStatus.GOOD
            score >= 50.0f -> EvaluationStatus.NEEDS_ATTENTION
            else -> EvaluationStatus.CRITICAL
        }

        val shortDiagnosis = when (status) {
            EvaluationStatus.EXCELLENT -> "Inclinação sólida: média de %.1f° com excelente paralelismo (desvio ±%.1f°).".format(meanAngle, stdDev)
            EvaluationStatus.GOOD -> "Boa inclinação (%.1f°), com variação moderada entre traços (±%.1f°).".format(meanAngle, stdDev)
            EvaluationStatus.NEEDS_ATTENTION -> "Oscilação angular perceptível (desvio ±%.1f°). Alinhe os traços a %.1f°.".format(stdDev, targetSlantDegrees)
            EvaluationStatus.CRITICAL -> "Instabilidade angular alta (desvio ±%.1f°). Mantenha o punho fixo e deslize o braço.".format(stdDev)
            EvaluationStatus.INSUFFICIENT_DATA -> "Amostras descendentes insuficientes para cálculo de dispersão angular."
        }

        return DimensionEvaluation(
            dimension = BiomechanicalDimension.SLANT_STABILITY,
            score = score,
            observedValue = meanAngle,
            targetValue = targetSlantDegrees,
            status = status,
            shortDiagnosis = shortDiagnosis
        )
    }

    // ==========================================
    // 2. Contenção de Pauta (Guideline Containment)
    // ==========================================
    private fun evaluateGuidelineContainment(
        strokes: List<Stroke>,
        attempts: List<PracticeAttemptRecord>
    ): DimensionEvaluation {
        val attemptScores = attempts.map { it.scorePercent.toFloat() }
        if (attemptScores.isNotEmpty()) {
            val score = attemptScores.average().toFloat().coerceIn(0.0f, 100.0f)
            val status = when {
                score >= 85.0f -> EvaluationStatus.EXCELLENT
                score >= 70.0f -> EvaluationStatus.GOOD
                score >= 50.0f -> EvaluationStatus.NEEDS_ATTENTION
                else -> EvaluationStatus.CRITICAL
            }
            val shortDiagnosis = when (status) {
                EvaluationStatus.EXCELLENT -> "Controle primoroso de pauta: linhas de base e ascendentes sem estouros."
                EvaluationStatus.GOOD -> "Boa contenção na pauta, com pequenos transbordos em laçadas."
                EvaluationStatus.NEEDS_ATTENTION -> "Atenção aos limites verticais: observe a linha de base e a altura-x."
                EvaluationStatus.CRITICAL -> "Vazamento excessivo de pauta. Desacelere ao atingir as guias."
                EvaluationStatus.INSUFFICIENT_DATA -> "Sem dados de pauta disponíveis."
            }

            return DimensionEvaluation(
                dimension = BiomechanicalDimension.GUIDELINE_CONTAINMENT,
                score = score,
                observedValue = score,
                targetValue = 100.0f,
                status = status,
                shortDiagnosis = shortDiagnosis
            )
        }

        // S09: Sem tentativas avaliadas contra gabarito real, não inventar scores fictícios (60%/80%)
        return DimensionEvaluation(
            dimension = BiomechanicalDimension.GUIDELINE_CONTAINMENT,
            score = 0.0f,
            observedValue = null,
            targetValue = 100.0f,
            status = EvaluationStatus.INSUFFICIENT_DATA,
            shortDiagnosis = "Sem tentativas avaliadas contra pautas de referência para cálculo de contenção."
        )
    }

    // ==========================================
    // 3. Ritmo e Cadência (Rhythm & Cadence)
    // ==========================================
    private fun evaluateRhythmAndCadence(strokes: List<Stroke>): DimensionEvaluation {
        val speeds = mutableListOf<Float>()
        var hesitationsCount = 0

        for (stroke in strokes) {
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val dt = (p2.tMs - p1.tMs).coerceAtLeast(1L)
                val dist = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
                val speed = dist / dt.toFloat() // px/ms

                if (dist > 1.0f) {
                    speeds.add(speed)
                }

                // Pausa abrupta ou desaceleração drástica no meio do traço
                if (dt > 100L && (dist < 4.0f || speed < 0.05f)) {
                    hesitationsCount++
                }
            }
        }

        if (speeds.isEmpty()) {
            return DimensionEvaluation(
                dimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                score = 0.0f,
                observedValue = null,
                targetValue = 0.45f,
                status = EvaluationStatus.INSUFFICIENT_DATA,
                shortDiagnosis = "Amostras temporais insuficientes para cálculo de cadência e fluidez."
            )
        }

        val avgSpeed = speeds.average().toFloat()
        val speedVariance = speeds.map { (it - avgSpeed).pow(2) }.average().toFloat()
        val speedStdDev = sqrt(speedVariance)
        val coeffOfVariation = if (avgSpeed > 0f) speedStdDev / avgSpeed else 1.0f

        // Pontuação favorece velocidade média controlada (0.2 a 0.8 px/ms) e suavidade (CV moderado)
        val speedPenalty = if (avgSpeed in 0.20f..0.85f) 0.0f else 15.0f
        val hesitationPenalty = (hesitationsCount * 3.0f).coerceAtMost(30.0f)
        val score = (100.0f - (coeffOfVariation * 25.0f) - speedPenalty - hesitationPenalty).coerceIn(0.0f, 100.0f)

        val status = when {
            score >= 85.0f -> EvaluationStatus.EXCELLENT
            score >= 70.0f -> EvaluationStatus.GOOD
            score >= 50.0f -> EvaluationStatus.NEEDS_ATTENTION
            else -> EvaluationStatus.CRITICAL
        }

        val shortDiagnosis = when (status) {
            EvaluationStatus.EXCELLENT -> "Cadência exemplar: escrita fluida a %.2f px/ms sem hesitações.".format(avgSpeed)
            EvaluationStatus.GOOD -> "Bom ritmo motor (%.2f px/ms), movimento uniforme.".format(avgSpeed)
            EvaluationStatus.NEEDS_ATTENTION -> "Hesitações detectadas em transições e curvas. Mantenha o fluxo contínuo."
            EvaluationStatus.CRITICAL -> "Traçado fragmentado ou hesitante. Pratique movimentos contínuos sem parar a caneta."
            EvaluationStatus.INSUFFICIENT_DATA -> "Amostras temporais insuficientes para cálculo de cadência."
        }

        return DimensionEvaluation(
            dimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
            score = score,
            observedValue = avgSpeed,
            targetValue = 0.45f,
            status = status,
            shortDiagnosis = shortDiagnosis
        )
    }

    // ==========================================
    // 4. Controle de Pressão (Pressure Control)
    // ==========================================
    private fun evaluatePressureControl(strokes: List<Stroke>): DimensionEvaluation {
        val downPressures = mutableListOf<Float>()
        val upPressures = mutableListOf<Float>()
        val allPressures = mutableListOf<Float>()

        for (stroke in strokes) {
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val dy = p2.y - p1.y
                val pr = p2.pressure

                if (pr != null && pr >= 0f) {
                    allPressures.add(pr)
                    if (dy > 3.0f) downPressures.add(pr)
                    else if (dy < -3.0f) upPressures.add(pr)
                }
            }
        }

        if (allPressures.isEmpty()) {
            return DimensionEvaluation(
                dimension = BiomechanicalDimension.PRESSURE_CONTROL,
                score = 0.0f,
                observedValue = null,
                targetValue = 1.4f,
                status = EvaluationStatus.INSUFFICIENT_DATA,
                shortDiagnosis = "Sem dados de sensor de pressão da caneta disponíveis no dispositivo."
            )
        }

        val avgPressure = allPressures.average().toFloat()
        val avgDown = if (downPressures.isNotEmpty()) downPressures.average().toFloat() else avgPressure
        val avgUp = if (upPressures.isNotEmpty()) upPressures.average().toFloat() else avgPressure
        val contrastRatio = if (avgUp > 0.05f) avgDown / avgUp else 1.0f

        // Penalidade por pressão excessiva (tensão da mão) ou falta de contraste
        val tensionPenalty = if (avgPressure > 0.85f) 25.0f else 0.0f
        val contrastBonus = if (contrastRatio >= 1.25f) 15.0f else 0.0f
        val score = (75.0f + contrastBonus - tensionPenalty).coerceIn(0.0f, 100.0f)

        val status = when {
            score >= 85.0f -> EvaluationStatus.EXCELLENT
            score >= 70.0f -> EvaluationStatus.GOOD
            score >= 50.0f -> EvaluationStatus.NEEDS_ATTENTION
            else -> EvaluationStatus.CRITICAL
        }

        val shortDiagnosis = when {
            tensionPenalty > 0f -> "Tensão excessiva na mão (pressão média %.2f). Alivie o aperto na S Pen.".format(avgPressure)
            contrastRatio >= 1.25f -> "Ótima modulação de pressão: subidas leves e descidas expressivas (contraste %.1fx).".format(contrastRatio)
            else -> "Pressão uniforme e controlada (média %.2f). Para estilos sombreados, alivie na subida.".format(avgPressure)
        }

        return DimensionEvaluation(
            dimension = BiomechanicalDimension.PRESSURE_CONTROL,
            score = score,
            observedValue = contrastRatio,
            targetValue = 1.4f,
            status = status,
            shortDiagnosis = shortDiagnosis
        )
    }

    private fun generateEmptyDiagnostic(): BiomechanicalDiagnostic {
        val emptyMap = BiomechanicalDimension.values().associateWith { dim ->
            DimensionEvaluation(
                dimension = dim,
                score = 0.0f,
                observedValue = null,
                targetValue = 0.0f,
                status = EvaluationStatus.INSUFFICIENT_DATA,
                shortDiagnosis = "Ainda não há escrita suficiente. Faça um treino para receber orientações."
            )
        }

        return BiomechanicalDiagnostic(
            overallScore = 0.0f,
            maturityLevel = MaturityLevel.BEGINNER,
            dimensions = emptyMap,
            primaryWeakness = null,
            primaryStrength = null,
            totalStrokesAnalyzed = 0,
            totalAttemptsAnalyzed = 0
        )
    }
}
