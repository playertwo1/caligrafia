package com.scribe.caligrafia.ink.persistence

/**
 * Métricas e resultados da avaliação de uma estratégia de persistência.
 */
data class PersistenceResult(
    val strategyName: String,
    val writeTimeMs: Double,
    val readTimeMs: Double,
    val sizeBytes: Long,
    val strokeCount: Int,
    val totalPoints: Int,
    val bytesPerPoint: Double = if (totalPoints > 0) sizeBytes.toDouble() / totalPoints else 0.0,
    val isFidelityPreserved: Boolean = true
)
