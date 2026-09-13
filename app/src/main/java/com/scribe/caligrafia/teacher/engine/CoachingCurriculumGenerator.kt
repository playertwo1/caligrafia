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
                rationale = "Seu traço apresenta maturidade caligráfica superior. Este treino exercita a coordenação motora fina com assistência visual mínima de ghost.",
                targetDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                recommendedMinutes = 15,
                warmupExerciseId = "basic_ascending_loop",
                focusExerciseId = "letter_o",
                recommendedGhostLevel = 0.10f,
                targetGoalDescription = "Executar formas clássicas com autonomia motora e precisão geométrica acima de 88%."
            )
        }

        // Se não houver fraqueza identificada
        if (weakness == null) {
            return PrescribedPracticeSession(
                title = "Treino de Manutenção e Fluidez",
                rationale = "Seus traços estão regulares e equilibrados. Pratique para manter a consistência e estabilidade do traço.",
                targetDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                recommendedMinutes = 10,
                warmupExerciseId = "basic_ascending_loop",
                focusExerciseId = "letter_o",
                recommendedGhostLevel = 0.40f,
                targetGoalDescription = "Manter o ritmo e a fluidez em sequências completas de traçado."
            )
        }

        return when (weakness) {
            BiomechanicalDimension.SLANT_STABILITY -> {
                PrescribedPracticeSession(
                    title = "Treino de Alinhamento e Paralelismo",
                    rationale = "O Professor IA identificou oscilação angular nos traços descendentes. Esta sessão ancora a referência motora no ângulo formal da pauta.",
                    targetDimension = BiomechanicalDimension.SLANT_STABILITY,
                    recommendedMinutes = 10,
                    warmupExerciseId = "basic_slant",
                    focusExerciseId = "letter_t",
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
                    focusExerciseId = "letter_a",
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
                    focusExerciseId = "letter_i",
                    recommendedGhostLevel = 0.40f,
                    targetGoalDescription = "Escrever o glifo e sua saída em movimento único e contínuo a cerca de 0.4 px/ms."
                )
            }

            BiomechanicalDimension.PRESSURE_CONTROL -> {
                PrescribedPracticeSession(
                    title = "Treino de Modulação e Toque da S Pen",
                    rationale = "O Professor IA detectou pressão constante ou contato excessivo no vidro. Este treino desenvolve o contraste dinâmico claro/escuro.",
                    targetDimension = BiomechanicalDimension.PRESSURE_CONTROL,
                    recommendedMinutes = 10,
                    warmupExerciseId = "basic_slant",
                    focusExerciseId = "letter_l",
                    recommendedGhostLevel = 0.70f,
                    targetGoalDescription = "Aliviar a pressão nas subidas finas e aplicar peso controlado apenas nas descidas."
                )
            }
        }
    }
}
