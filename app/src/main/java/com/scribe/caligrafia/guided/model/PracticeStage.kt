package com.scribe.caligrafia.guided.model

/**
 * Estágios pedagógicos do método de caligrafia do Scribe.
 *
 * Progressão clássica:
 * 1. [TRACE] (Cobrir): O glifo é projetado diretamente sob a caneta com Ghost Mode configurável.
 * 2. [COPY] (Copiar): O glifo fica visível à esquerda como gabarito, e a linha de escrita fica livre ao lado.
 * 3. [SOLO] (Sozinho): Prática autônoma orientada apenas pelas pautas caligráficas, estimulando memória muscular.
 */
enum class PracticeStage(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val defaultGhostLevel: GhostModeLevel
) {
    TRACE(
        stepNumber = 1,
        title = "Cobrir",
        description = "Trace a caneta diretamente sobre o modelo com apoio visual",
        defaultGhostLevel = GhostModeLevel.CLEAR
    ),
    COPY(
        stepNumber = 2,
        title = "Copiar",
        description = "Observe o modelo à esquerda e reproduza o movimento ao lado",
        defaultGhostLevel = GhostModeLevel.OFF
    ),
    SOLO(
        stepNumber = 3,
        title = "Sozinho",
        description = "Escreva de memória mantendo apenas as pautas ativas",
        defaultGhostLevel = GhostModeLevel.OFF
    );

    fun nextStage(): PracticeStage = when (this) {
        TRACE -> COPY
        COPY -> SOLO
        SOLO -> SOLO
    }

    fun previousStage(): PracticeStage = when (this) {
        TRACE -> TRACE
        COPY -> TRACE
        SOLO -> COPY
    }
}
