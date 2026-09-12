package com.scribe.caligrafia.expansions.passage

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Motor de avaliação de ritmo e cadência motora para o Modo de Cópia de Textos.
 * Analisa palavras por minuto (WPM) sob a ótica da caligrafia deliberada.
 */
object PassagePacingEngine {

    fun evaluatePacing(
        passage: PassageItem,
        durationMs: Long
    ): PassagePacingResult {
        val wordCount = passage.totalWordCount.coerceAtLeast(1)
        val minutes = (durationMs.toFloat() / 60000f).coerceAtLeast(0.01f)
        val actualWpm = wordCount / minutes

        val targetWpm = passage.targetWpm.toFloat().coerceAtLeast(5f)
        val ratio = actualWpm / targetWpm

        // Na caligrafia, escrever com pressa degrada a forma; escrever muito devagar indica hesitação.
        // Faixa ideal: entre 0.85 e 1.25 do targetWpm.
        val pacingScore = when {
            ratio in 0.85f..1.25f -> {
                val deviation = abs(1f - ratio)
                (100f - (deviation * 100f)).coerceIn(85f, 100f)
            }
            ratio < 0.85f -> {
                // Mais lento que a meta
                val penalty = (0.85f - ratio) * 80f
                (85f - penalty).coerceIn(30f, 85f)
            }
            else -> {
                // Mais rápido que a meta (risco de rabisco)
                val penalty = (ratio - 1.25f) * 60f
                (85f - penalty).coerceIn(30f, 85f)
            }
        }

        val diagnosis: String
        val recommendation: String

        when {
            pacingScore >= 88f -> {
                diagnosis = "Ritmo Excelente e Cadenciado (%.1f WPM)".format(actualWpm)
                recommendation = "Sua velocidade está perfeitamente sincronizada com o estilo caligráfico pretendido."
            }
            ratio > 1.35f -> {
                diagnosis = "Cadência Acelerada (%.1f WPM)".format(actualWpm)
                recommendation = "Você escreveu consideravelmente mais rápido que a velocidade pedagógica (meta: ${passage.targetWpm} WPM). Desacelere para garantir o ductus correto."
            }
            ratio < 0.70f -> {
                diagnosis = "Cadência Hesitante (%.1f WPM)".format(actualWpm)
                recommendation = "Houve pausas prolongadas ou hesitação entre as letras. Busque manter o fluxo do antebraço sem interrupções."
            }
            else -> {
                diagnosis = "Cadência Razoável (%.1f WPM)".format(actualWpm)
                recommendation = "Boa constância. Ajuste ligeiramente a cadência para aproximar-se da meta de ${passage.targetWpm} WPM."
            }
        }

        return PassagePacingResult(
            totalWords = wordCount,
            durationMs = durationMs,
            actualWpm = actualWpm,
            targetWpm = passage.targetWpm,
            pacingScore = pacingScore,
            diagnosis = diagnosis,
            recommendation = recommendation
        )
    }
}
