package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.TeacherInsight

/**
 * Motor de Insights Pedagógicos em Linguagem Natural do Professor IA (SCR-703, F4.03, F4.04).
 *
 * Gera observações e recomendações técnicas e pedagógicas
 * 100% fundamentadas em dados observados do calígrafo e vinculadas às tentativas reais.
 */
class CoachingFeedbackEngine {

    /**
     * Gera um conjunto estruturado de insights pedagógicos a partir do diagnóstico
     * e das tentativas de escrita que o sustentam.
     */
    fun generateInsights(
        diagnostic: BiomechanicalDiagnostic,
        attempts: List<PracticeAttemptRecord> = emptyList()
    ): List<TeacherInsight> {
        val insights = mutableListOf<TeacherInsight>()

        val bestAttempt = attempts.maxByOrNull { it.scorePercent }
        val worstAttempt = attempts.minByOrNull { it.scorePercent }

        // 1. Elogio Fundamentado (PRAISE)
        diagnostic.primaryStrength?.let { strength ->
            val eval = diagnostic.dimensions[strength]
            val scoreVal = eval?.score ?: 80f
            val (title, msg) = when (strength) {
                BiomechanicalDimension.SLANT_STABILITY -> Pair(
                    "Paralelismo Admirável",
                    "Sua disciplina angular é notável! Seus traços descendentes mantêm um paralelismo coeso, conferindo elegância clássica à sua caligrafia."
                )
                BiomechanicalDimension.GUIDELINE_CONTAINMENT -> Pair(
                    "Precisão de Pauta",
                    "Excelente domínio do espaço de escrita! Você ancora a linha de base e respeita a altura-x com firmeza e sem transbordos."
                )
                BiomechanicalDimension.RHYTHM_AND_CADENCE -> Pair(
                    "Fluidez e Regularidade",
                    "Seu movimento de escrita é contínuo e rítmico. A ausência de paradas intermediárias demonstra coordenação motora consolidada."
                )
                BiomechanicalDimension.PRESSURE_CONTROL -> Pair(
                    "Toque Caligráfico Maduro",
                    "Sua S Pen desliza com leveza onde deve e marca presença nas descidas. Esse contraste dinâmico enriquece o traçado."
                )
            }

            insights.add(
                TeacherInsight(
                    type = InsightType.PRAISE,
                    title = title,
                    message = msg,
                    relatedDimension = strength,
                    metricDelta = "%.0f%%".format(scoreVal),
                    relatedAttemptId = bestAttempt?.attemptId,
                    relatedTargetTitle = bestAttempt?.targetTitle,
                    relatedScore = bestAttempt?.scorePercent
                )
            )
        }

        // 2. Correção Técnica (CORRECTION)
        diagnostic.primaryWeakness?.let { weakness ->
            val eval = diagnostic.dimensions[weakness]
            val (title, msg) = when (weakness) {
                BiomechanicalDimension.SLANT_STABILITY -> Pair(
                    "Ajuste de Ângulo e Paralelismo",
                    "Alguns traços verticais estão variando de inclinação. Mantenha o punho estável e deslize o antebraço ao traçar as descidas."
                )
                BiomechanicalDimension.GUIDELINE_CONTAINMENT -> Pair(
                    "Atenção aos Limites da Pauta",
                    "As curvas estão ultrapassando ligeiramente a altura-x. Desacelere sutilmente antes da linha guia para arredondar a curva nos limites."
                )
                BiomechanicalDimension.RHYTHM_AND_CADENCE -> Pair(
                    "Evite Hesitações no Meio da Letra",
                    "Detectamos micro-paradas na transição entre o corpo do traço e a saída. Pratique executar o movimento em fluxo contínuo."
                )
                BiomechanicalDimension.PRESSURE_CONTROL -> Pair(
                    "Modulação da Pressão na S Pen",
                    "Foi registrada pressão contínua elevada na ponta da caneta. A S Pen do S25 Ultra é sensível: alivie a força de contato com o vidro nas subidas."
                )
            }

            insights.add(
                TeacherInsight(
                    type = InsightType.CORRECTION,
                    title = title,
                    message = msg,
                    relatedDimension = weakness,
                    metricDelta = eval?.shortDiagnosis,
                    relatedAttemptId = worstAttempt?.attemptId,
                    relatedTargetTitle = worstAttempt?.targetTitle,
                    relatedScore = worstAttempt?.scorePercent
                )
            )
        }

        // 3. Dica Ergonômica (ERGONOMIC_TIP)
        val ergonomicTip = selectErgonomicTip(diagnostic)
        insights.add(ergonomicTip)

        // 4. Desafio do Mestre (CHALLENGE)
        val challenge = selectChallenge(diagnostic)
        insights.add(challenge)

        return insights
    }

    private fun selectErgonomicTip(diagnostic: BiomechanicalDiagnostic): TeacherInsight {
        val weakness = diagnostic.primaryWeakness
        return when (weakness) {
            BiomechanicalDimension.PRESSURE_CONTROL -> TeacherInsight(
                type = InsightType.ERGONOMIC_TIP,
                title = "Empunhadura Funcional (Tripod Grip)",
                message = "Segure a S Pen confortavelmente cerca de 2 cm acima da ponta. Aplique contato leve sobre a superfície da tela sem pressionar em excesso.",
                relatedDimension = BiomechanicalDimension.PRESSURE_CONTROL
            )
            BiomechanicalDimension.SLANT_STABILITY -> TeacherInsight(
                type = InsightType.ERGONOMIC_TIP,
                title = "Pivô do Braço vs. Dedos",
                message = "Na caligrafia tradicional, a inclinação consistente vem do deslizamento do antebraço na mesa, não da rotação do punho. Gire levemente o Galaxy S25 Ultra até encontrar seu ângulo natural.",
                relatedDimension = BiomechanicalDimension.SLANT_STABILITY
            )
            else -> TeacherInsight(
                type = InsightType.ERGONOMIC_TIP,
                title = "Postura e Descanso da Palma",
                message = "Com a rejeição de palma ativa do Scribe, você pode descansar confortavelmente a mão espalmada sobre a tela do S25 Ultra, exatamente como faria no papel.",
                relatedDimension = BiomechanicalDimension.GUIDELINE_CONTAINMENT
            )
        }
    }

    private fun selectChallenge(diagnostic: BiomechanicalDiagnostic): TeacherInsight {
        val score = diagnostic.overallScore
        return if (score >= 80f) {
            TeacherInsight(
                type = InsightType.CHALLENGE,
                title = "Desafio Solo: Modo Marca d'Água",
                message = "Seu domínio técnico está alto! Na sua próxima sessão, reduza o Ghost Mode para 10% e escreva a forma inteira com orientação apenas das pautas."
            )
        } else {
            TeacherInsight(
                type = InsightType.CHALLENGE,
                title = "Desafio de Consistência: 3 Tentativas",
                message = "Tente fazer 3 repetições consecutivas do exercício de foco com pontuação acima de 75%. O segredo da caligrafia é a repetição consciente."
            )
        }
    }
}
