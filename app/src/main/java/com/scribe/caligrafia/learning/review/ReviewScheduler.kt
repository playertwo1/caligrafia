package com.scribe.caligrafia.learning.review

import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.model.CurriculumLesson
import com.scribe.caligrafia.learning.model.CurriculumStage
import kotlin.math.roundToInt

/**
 * Registro de retenção de um exercício ou lição na repetição espaçada local (M4 — SCR-403).
 */
data class SpacedRepetitionItem(
    val targetId: String,
    val repetitionCount: Int = 0,
    val lastScorePercent: Int = 0,
    val lastPracticedTimestampMs: Long = 0L,
    val intervalDays: Int = 1,
    val nextReviewTimestampMs: Long = 0L
) {
    fun isDue(nowMs: Long): Boolean {
        return nowMs >= nextReviewTimestampMs
    }
}

/**
 * Recomendação diária de prática calibrada pelo algoritmo de retenção motora.
 */
data class DailyRecommendation(
    val warmUpLesson: CurriculumLesson,
    val focusLesson: CurriculumLesson,
    val reviewLesson: CurriculumLesson?,
    val reason: String
)

/**
 * Agendador de repetição espaçada (SRS) 100% determinístico e local (zero nuvem).
 *
 * Algoritmo adaptado para consolidação neuromotora caligráfica:
 * 1. Menor pontuação (< 60%): agendamento prioritário e imediato (1 dia).
 * 2. Pontuação intermediária (60% a 79%): avanço suave de retenção (1.5x do intervalo).
 * 3. Alta pontuação (>= 80%): consolidação de memória motora (2.2x do intervalo).
 */
object ReviewScheduler {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    /**
     * Calcula o próximo intervalo e timestamp de revisão a partir de uma nova tentativa.
     */
    fun updateRepetition(
        currentItem: SpacedRepetitionItem?,
        targetId: String,
        scorePercent: Int,
        nowMs: Long = System.currentTimeMillis()
    ): SpacedRepetitionItem {
        val currentRepetitions = currentItem?.repetitionCount ?: 0
        val currentInterval = currentItem?.intervalDays ?: 1

        val newInterval = when {
            scorePercent < 60 -> 1
            scorePercent in 60..79 -> (currentInterval * 1.5f).roundToInt().coerceAtLeast(2)
            else -> (currentInterval * 2.2f).roundToInt().coerceAtLeast(3)
        }

        val nextReviewMs = nowMs + (newInterval * ONE_DAY_MS)

        return SpacedRepetitionItem(
            targetId = targetId,
            repetitionCount = currentRepetitions + 1,
            lastScorePercent = scorePercent,
            lastPracticedTimestampMs = nowMs,
            intervalDays = newInterval,
            nextReviewTimestampMs = nextReviewMs
        )
    }

    /**
     * Gera a recomendação de prática para a data atual.
     */
    fun getDailyRecommendation(
        allLessons: List<CurriculumLesson> = CurriculumCatalog.ALL_LESSONS,
        records: List<SpacedRepetitionItem>,
        nowMs: Long = System.currentTimeMillis()
    ): DailyRecommendation {
        // 1. Aquecimento: lição inicial de controle de traço
        val warmUpLesson = allLessons.firstOrNull { it.stage == CurriculumStage.STAGE_1_STROKES }
            ?: CurriculumCatalog.defaultFirstLesson()

        // 2. Busca lições que venceram o prazo de revisão
        val dueRecords = records.filter { it.isDue(nowMs) }
            .sortedBy { it.lastScorePercent } // Prioriza as menores notas primeiro

        val reviewLesson = dueRecords.firstNotNullOfOrNull { due ->
            allLessons.firstOrNull { it.id == due.targetId }
        }

        // 3. Busca a próxima lição nova da trilha que ainda não foi praticada
        val practicedIds = records.map { it.targetId }.toSet()
        val nextNewLesson = allLessons.firstOrNull { it.id !in practicedIds }

        val focusLesson = when {
            reviewLesson != null && reviewLesson.id != warmUpLesson.id -> reviewLesson
            nextNewLesson != null -> nextNewLesson
            else -> allLessons.firstOrNull { it.stage == CurriculumStage.STAGE_2_FAMILIES } ?: warmUpLesson
        }

        val reason = when {
            dueRecords.isNotEmpty() -> "Revisão sugerida para consolidar a precisão e a inclinação observadas anteriormente."
            nextNewLesson != null -> "Novo passo na trilha caligráfica para expandir seu repertório formal."
            else -> "Manutenção de consistência motora com lições fundamentais."
        }

        return DailyRecommendation(
            warmUpLesson = warmUpLesson,
            focusLesson = focusLesson,
            reviewLesson = reviewLesson?.takeIf { it.id != focusLesson.id },
            reason = reason
        )
    }
}
