package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.TeacherInsight

/**
 * Motor de Insights Pedagógicos em Linguagem Natural do Professor IA (SCR-703).
 *
 * Gera observações e recomendações técnicas, ergonômicas e motivadoras
 * 100% fundamentadas em dados observados do calígrafo.
 */
class CoachingFeedbackEngine {

    /**
     * Gera um conjunto estruturado de insights pedagógicos a partir do diagnóstico.
     */
    fun generateInsights(diagnostic: BiomechanicalDiagnostic): List<TeacherInsight> {
        val insights = mutableListOf<TeacherInsight>()

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
                    "Fluidez e Confiança",
                    "Seu movimento de escrita é contínuo e rítmico. A ausência de paradas intermediárias demonstra memória muscular consolidada."
                )
                BiomechanicalDimension.PRESSURE_CONTROL -> Pair(
                    "Toque Caligráfico Maduro",
                    "Sua S Pen desliza com leveza onde deve e marca presença nas descidas. Esse contraste é a marca registrada dos grandes calígrafos."
                )
            }

            insights.add(
                TeacherInsight(
                    type = InsightType.PRAISE,
                    title = title,
                    message = msg,
                    relatedDimension = strength,
                    metricDelta = "%.0f%%".format(scoreVal)
                )
            )
        }

        // 2. Correção Técnica (CORRECTION)
        diagnostic.primaryWeakness?.let { weakness ->
            val eval = diagnostic.dimensions[weakness]
            val (title, msg) = when (weakness) {
                BiomechanicalDimension.SLANT_STABILITY -> Pair(
                    "Ajuste de Ângulo e Paralelismo",
                    "Alguns traços verticais estão variando de inclinação. Lembre-se de apoiar o antebraço na mesa e mover o braço inteiro ao descer a caneta, em vez de dobrar apenas o polegar."
                )
                BiomechanicalDimension.GUIDELINE_CONTAINMENT -> Pair(
                    "Atenção aos Limites da Pauta",
                    "As curvas estão ultrapassando ligeiramente a altura-x. Desacelere sutilmente 2mm antes da linha guia para arredondar a curva com perfeição."
                )
                BiomechanicalDimension.RHYTHM_AND_CADENCE -> Pair(
                    "Evite Hesitações no Meio da Letra",
                    "Detectamos micro-paradas na transição entre o corpo da letra e a ligadura. Pratique o gesto no ar uma vez antes de tocar a tela para executar em fluxo único."
                )
                BiomechanicalDimension.PRESSURE_CONTROL -> Pair(
                    "Alívio de Tensão na S Pen",
                    "Você está segurando a caneta com força excessiva. Afrouxe a pegada nos dedos: a S Pen do S25 Ultra é ultrassensível e não requer força física para marcar o traço."
                )
            }

            insights.add(
                TeacherInsight(
                    type = InsightType.CORRECTION,
                    title = title,
                    message = msg,
                    relatedDimension = weakness,
                    metricDelta = eval?.shortDiagnosis
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
                title = "Empunhadura Relaxada (Tripod Grip)",
                message = "Segure a S Pen cerca de 2 cm acima da ponta. Imagine que você está segurando uma folha fina sem amassá-la: seus nós dos dedos não devem ficar brancos de aperto.",
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
                message = "Com a rejeição de palma ativa do Scribe, você pode descansar confortavelmente a mão espalmada sobre a tela do S25 Ultra, exatamente como faria no papel de linho.",
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
                message = "Seu domínio técnico está alto! Na sua próxima sessão, reduza o Ghost Mode para 10% e escreva a palavra inteira sem olhar para o gabarito."
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
