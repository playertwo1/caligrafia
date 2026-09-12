package com.scribe.caligrafia.teacher.model

import java.util.UUID

/**
 * Dimensões biomecânicas fundamentais avaliadas pelo Professor IA.
 */
enum class BiomechanicalDimension(val displayName: String, val description: String) {
    SLANT_STABILITY(
        "Estabilidade Angular",
        "Consistência do paralelismo e ângulo de inclinação dos traços descendentes."
    ),
    GUIDELINE_CONTAINMENT(
        "Contenção de Pauta",
        "Respeito às linhas mestras (baseline, waistline, ascendente e descendente) sem vazamento."
    ),
    RHYTHM_AND_CADENCE(
        "Ritmo e Cadência",
        "Fluidez temporal, aceleração suave e ausência de hesitações ou micro-paradas no traçado."
    ),
    PRESSURE_CONTROL(
        "Controle de Pressão",
        "Modulação entre descidas com pressão e subidas leves na S Pen sem tensão manual excessiva."
    )
}

/**
 * Status de conformidade de uma dimensão biomecânica.
 */
enum class EvaluationStatus(val label: String) {
    EXCELLENT("Excelente"),
    GOOD("Bom"),
    NEEDS_ATTENTION("Requer Atenção"),
    CRITICAL("Crítico")
}

/**
 * Nível de maturidade caligráfica global do usuário.
 */
enum class MaturityLevel(val title: String, val minScore: Float) {
    BEGINNER("Iniciante no Traço", 0f),
    PRACTITIONER("Praticante Dedicado", 50f),
    DEVELOPING_CALLIGRAPHER("Calígrafo em Desenvolvimento", 70f),
    MASTER_OF_STROKE("Mestre do Traço", 85f);

    companion object {
        fun fromScore(score: Float): MaturityLevel = when {
            score >= 85f -> MASTER_OF_STROKE
            score >= 70f -> DEVELOPING_CALLIGRAPHER
            score >= 50f -> PRACTITIONER
            else -> BEGINNER
        }
    }
}

/**
 * Avaliação individual de uma dimensão biomecânica.
 */
data class DimensionEvaluation(
    val dimension: BiomechanicalDimension,
    val score: Float, // 0.0f a 100.0f
    val observedValue: Float,
    val targetValue: Float,
    val status: EvaluationStatus,
    val shortDiagnosis: String
)

/**
 * Diagnóstico completo gerado pelo motor biomecânico do Professor IA.
 */
data class BiomechanicalDiagnostic(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val overallScore: Float, // 0.0f a 100.0f
    val maturityLevel: MaturityLevel,
    val dimensions: Map<BiomechanicalDimension, DimensionEvaluation>,
    val primaryWeakness: BiomechanicalDimension?,
    val primaryStrength: BiomechanicalDimension?,
    val totalStrokesAnalyzed: Int,
    val totalAttemptsAnalyzed: Int
)

/**
 * Tipos de insights pedagógicos emitidos pelo Professor IA.
 */
enum class InsightType(val label: String) {
    PRAISE("Elogio"),
    CORRECTION("Correção Técnica"),
    ERGONOMIC_TIP("Dica Ergonômica"),
    CHALLENGE("Desafio do Mestre")
}

/**
 * Observação pedagógica individual em linguagem natural fundamentada em dados.
 */
data class TeacherInsight(
    val id: String = UUID.randomUUID().toString(),
    val type: InsightType,
    val title: String,
    val message: String,
    val relatedDimension: BiomechanicalDimension? = null,
    val metricDelta: String? = null
)

/**
 * Sessão de treino prescrita especificamente para o perfil motor atual do calígrafo.
 */
data class PrescribedPracticeSession(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val title: String,
    val rationale: String,
    val targetDimension: BiomechanicalDimension,
    val recommendedMinutes: Int = 10,
    val warmupExerciseId: String,
    val focusExerciseId: String,
    val recommendedGhostLevel: Float = 0.7f,
    val targetGoalDescription: String,
    val isCompleted: Boolean = false
)
