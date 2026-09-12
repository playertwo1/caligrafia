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

        val overallScore = (
            slantEval.score * 0.30f +
            containmentEval.score * 0.25f +
            rhythmEval.score * 0.25f +
            pressureEval.score * 0.20f
        ).coerceIn(0f, 100f)

        val sortedDims = dimensions.entries.sortedBy { it.value.score }
        val primaryWeakness = sortedDims.firstOrNull()?.key
        val primaryStrength = sortedDims.lastOrNull()?.key

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
        val angles = mutableListOf<Float>()

        for (stroke in strokes) {
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val dy = p2.y - p1.y
                val dx = p2.x - p1.x

                // Segmentos descendentes expressivos
                if (dy > 3.0f && sqrt(dx * dx + dy * dy) > 4.0f) {
                    val angle = atan2(dy, dx) * 180.0f / Math.PI.toFloat()
                    if (angle in 30.0f..90.0f) {
                        angles.add(angle)
                    }
                }
            }
        }

        if (angles.isEmpty()) {
            return DimensionEvaluation(
                dimension = BiomechanicalDimension.SLANT_STABILITY,
                score = 70.0f,
                observedValue = targetSlantDegrees,
                targetValue = targetSlantDegrees,
                status = EvaluationStatus.GOOD,
                shortDiagnosis = "Amostras descendentes insuficientes para cálculo de dispersão angular."
            )
        }

        val meanAngle = angles.average().toFloat()
        val variance = angles.map { (it - meanAngle).pow(2) }.average().toFloat()
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
        // Se temos tentativas com pontuação de precisão geométrica calculada, usamos a média
        val attemptScores = attempts.map { it.scorePercent.toFloat() }
        val baseScore = if (attemptScores.isNotEmpty()) {
            attemptScores.average().toFloat()
        } else {
            // Estimativa por dispersão vertical dos traços
            val allY = strokes.flatMap { s -> s.points.map { it.y } }
            if (allY.isEmpty()) 70.0f
            else {
                val height = (allY.maxOrNull() ?: 0f) - (allY.minOrNull() ?: 0f)
                if (height in 20f..400f) 80.0f else 60.0f
            }
        }

        val score = baseScore.coerceIn(0.0f, 100.0f)
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
                score = 75.0f,
                observedValue = 0.40f,
                targetValue = 0.45f,
                status = EvaluationStatus.GOOD,
                shortDiagnosis = "Cadência regular em amostras curtas."
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
                score = 75.0f,
                observedValue = 0.5f,
                targetValue = 0.5f,
                status = EvaluationStatus.GOOD,
                shortDiagnosis = "Pressão uniforme mantida (sensor neutro ou stylus capacitiva)."
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
                score = 70.0f,
                observedValue = 0.0f,
                targetValue = 100.0f,
                status = EvaluationStatus.GOOD,
                shortDiagnosis = "Aguardando primeiras tentativas de escrita para diagnóstico aprofundado."
            )
        }

        return BiomechanicalDiagnostic(
            overallScore = 70.0f,
            maturityLevel = MaturityLevel.PRACTITIONER,
            dimensions = emptyMap,
            primaryWeakness = null,
            primaryStrength = null,
            totalStrokesAnalyzed = 0,
            totalAttemptsAnalyzed = 0
        )
    }
}
