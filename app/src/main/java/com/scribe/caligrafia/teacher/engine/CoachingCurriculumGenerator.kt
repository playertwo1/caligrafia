package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.MaturityLevel
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession

/**
 * Gerador de Treino Sob Medida e Sessão de Coaching (SCR-702).
 *
 * Traduz o diagnóstico biomecânico em uma prescrição pedagógica acionável
 * direcionada à fraqueza prioritária detectada no usuário.
 */
class CoachingCurriculumGenerator {

    /**
     * Gera a prescrição da próxima sessão de treino recomendada pelo Professor IA.
     */
    fun generatePrescription(diagnostic: BiomechanicalDiagnostic): PrescribedPracticeSession {
        val weakness = diagnostic.primaryWeakness

        // Se o diagnóstico for vazio ou inicial, prescreve sessão introdutória honesta (S08)
        if (diagnostic.totalStrokesAnalyzed == 0 || diagnostic.totalAttemptsAnalyzed == 0 ||
            (diagnostic.maturityLevel == MaturityLevel.BEGINNER && weakness == null)) {
            return PrescribedPracticeSession(
                title = "Treino Inicial de Diagnóstico",
                rationale = "Complete sua primeira sessão prática para que o Professor IA identifique suas necessidades biomecânicas personalizadas.",
                targetDimension = BiomechanicalDimension.SLANT_STABILITY,
                recommendedMinutes = 5,
                warmupExerciseId = "basic_slant",
                focusExerciseId = "basic_slant",
                recommendedGhostLevel = 0.80f,
                targetGoalDescription = "Traçar os traços fundamentais com firmeza e ritmo controlado."
            )
        }

        // Se o usuário atingiu maestria global
        if (diagnostic.maturityLevel == MaturityLevel.MASTER_OF_STROKE) {
            return PrescribedPracticeSession(
                title = "Treino de Refinamento e Mestria",
                rationale = "Seu traço apresenta maturidade caligráfica superior. Este treino desafia sua memória muscular com assistência visual mínima.",
                targetDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                recommendedMinutes = 15,
                warmupExerciseId = "ascending_loop",
                focusExerciseId = "to",
                recommendedGhostLevel = 0.10f,
                targetGoalDescription = "Executar conexões clássicas em modo quase autônomo com precisão acima de 88%."
            )
        }

        // Se não houver fraqueza identificada
        if (weakness == null) {
            return PrescribedPracticeSession(
                title = "Treino de Manutenção e Fluidez",
                rationale = "Seus traços estão estáveis e equilibrados. Pratique para manter a consistência neuromuscular.",
                targetDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                recommendedMinutes = 10,
                warmupExerciseId = "ascending_loop",
                focusExerciseId = "to",
                recommendedGhostLevel = 0.40f,
                targetGoalDescription = "Manter o ritmo e a fluidez em sequências completas."
            )
        }

        return when (weakness) {
            BiomechanicalDimension.SLANT_STABILITY -> {
                PrescribedPracticeSession(
                    title = "Treino de Alinhamento e Paralelismo",
                    rationale = "O Professor IA identificou oscilação angular nos traços descendentes. Esta sessão ancora a memória motora no ângulo formal da pauta.",
                    targetDimension = BiomechanicalDimension.SLANT_STABILITY,
                    recommendedMinutes = 10,
                    warmupExerciseId = "basic_slant",
                    focusExerciseId = "t",
                    recommendedGhostLevel = 0.70f,
                    targetGoalDescription = "Manter todos os traços descendentes rigorosamente paralelos com desvio angular menor que 4°."
                )
            }

            BiomechanicalDimension.GUIDELINE_CONTAINMENT -> {
                PrescribedPracticeSession(
                    title = "Treino de Contenção e Limites de Pauta",
                    rationale = "O Professor IA notou pequenas variações no alcance da altura-x e linha de base. Este treino reforça a contenção visual.",
                    targetDimension = BiomechanicalDimension.GUIDELINE_CONTAINMENT,
                    recommendedMinutes = 10,
                    warmupExerciseId = "underturn",
                    focusExerciseId = "a",
                    recommendedGhostLevel = 0.70f,
                    targetGoalDescription = "Conter as curvas perfeitamente entre a waistline e a baseline sem transbordos."
                )
            }

            BiomechanicalDimension.RHYTHM_AND_CADENCE -> {
                PrescribedPracticeSession(
                    title = "Treino de Fluidez e Cadência Contínua",
                    rationale = "O Professor IA detectou micro-pausas e hesitações no traçado. Esta sessão desenvolve velocidade uniforme e ritmo motor.",
                    targetDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                    recommendedMinutes = 15,
                    warmupExerciseId = "compound_curve",
                    focusExerciseId = "it",
                    recommendedGhostLevel = 0.40f,
                    targetGoalDescription = "Escrever o glifo e sua ligadura em movimento único e contínuo a cerca de 0.4 px/ms."
                )
            }

            BiomechanicalDimension.PRESSURE_CONTROL -> {
                PrescribedPracticeSession(
                    title = "Treino de Modulação e Toque da S Pen",
                    rationale = "O Professor IA detectou pressão constante ou aperto excessivo na caneta. Este treino desenvolve o contraste claro/escuro.",
                    targetDimension = BiomechanicalDimension.PRESSURE_CONTROL,
                    recommendedMinutes = 10,
                    warmupExerciseId = "basic_slant",
                    focusExerciseId = "l",
                    recommendedGhostLevel = 0.70f,
                    targetGoalDescription = "Aliviar a pressão nas subidas finas e aplicar peso controlado apenas nas descidas."
                )
            }
        }
    }
}
