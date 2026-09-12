package com.scribe.caligrafia.learning.session

import com.scribe.caligrafia.learning.model.CurriculumLesson

/**
 * Duração pré-programada de uma sessão deliberada de caligrafia (M4 — SCR-402).
 */
enum class SessionDuration(val minutes: Int, val label: String) {
    MIN_5(5, "5 min (Rápida)"),
    MIN_10(10, "10 min (Padrão)"),
    MIN_15(15, "15 min (Aprofundada)"),
    MIN_20(20, "20 min (Completa)");

    val totalSeconds: Int get() = minutes * 60
}

/**
 * Fases pedagógicas sequenciais de uma sessão estruturada (conforme LEARNING_SYSTEM.md).
 *
 * Cada fase recebe uma fatia percentual da duração total da sessão.
 */
enum class SessionPhase(
    val phaseIndex: Int,
    val title: String,
    val description: String,
    val percentShare: Float
) {
    /** 1. Aquecimento (15%): soltura motora do punho, dedos e traços livres. */
    WARM_UP(
        phaseIndex = 1,
        title = "Aquecimento",
        description = "Soltura do pulso e traços leves na pauta para lubrificação motora.",
        percentShare = 0.15f
    ),

    /** 2. Demonstração / Foco (15%): observação do ductus, pistas e proporção. */
    DEMO_FOCUS(
        phaseIndex = 2,
        title = "Foco da Lição",
        description = "Observe atentamente o ductus, o ângulo de inclinação e a proporção do modelo.",
        percentShare = 0.15f
    ),

    /** 3. Prática Assistida (40%): repetições com Ghost Mode sobreposto. */
    ASSISTED_PRACTICE(
        phaseIndex = 3,
        title = "Prática Assistida",
        description = "Repita os traços cobrindo o modelo de referência com Ghost Mode ativo.",
        percentShare = 0.40f
    ),

    /** 4. Prática Autônoma (20%): escrita livre na pauta a partir da memória muscular. */
    SOLO_PRACTICE(
        phaseIndex = 4,
        title = "Prática Autônoma",
        description = "Escreva diretamente na pauta sem gabarito imediato, evocando a memória muscular.",
        percentShare = 0.20f
    ),

    /** 5. Conclusão & Resumo (10%): encerramento, diagnóstico de consistência e foco futuro. */
    REVIEW_SUMMARY(
        phaseIndex = 5,
        title = "Conclusão & Resumo",
        description = "Avaliação da consistência da sessão e direcionamento para a próxima prática.",
        percentShare = 0.10f
    );

    fun nextPhase(): SessionPhase? {
        val nextOrdinal = ordinal + 1
        return if (nextOrdinal < entries.size) entries[nextOrdinal] else null
    }
}

/**
 * Estado dinâmico de uma sessão de treino cronometrada em andamento.
 */
data class ActiveSessionState(
    val lesson: CurriculumLesson,
    val duration: SessionDuration = SessionDuration.MIN_10,
    val currentPhase: SessionPhase = SessionPhase.WARM_UP,
    val phaseElapsedSeconds: Int = 0,
    val phaseTotalSeconds: Int = 90,
    val totalElapsedSeconds: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val attemptsCount: Int = 0,
    val averageScore: Float? = null
) {
    val totalSeconds: Int get() = duration.totalSeconds
    val totalRemainingSeconds: Int get() = (totalSeconds - totalElapsedSeconds).coerceAtLeast(0)
    val phaseRemainingSeconds: Int get() = (phaseTotalSeconds - phaseElapsedSeconds).coerceAtLeast(0)
    val sessionProgress: Float get() = (totalElapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val phaseProgress: Float get() = if (phaseTotalSeconds > 0) (phaseElapsedSeconds.toFloat() / phaseTotalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
}
