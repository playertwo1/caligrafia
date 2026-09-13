package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke

/**
 * Representação de uma tentativa de assinatura capturada com dados vetoriais brutos.
 */
data class SignatureAttempt(
    val id: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val strokes: List<Stroke>,
    val durationMs: Long,
    val baselineAngleDeg: Float = 0f,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
    val aspectRatio: Float get() = width / height
}

/**
 * Métricas cinemáticas e geométricas de uma assinatura.
 */
data class SignatureMetrics(
    val strokeCount: Int,
    val durationMs: Long,
    val averageSpeedPxPerMs: Float,
    val totalLengthPx: Float,
    val aspectRatio: Float,
    val penUpCount: Int,
    val dominantAngleDegrees: Float = 0f,
    val windingSign: Float = 0f
)

/**
 * Relatório de consistência e repetibilidade de assinatura em relação a uma referência.
 */
data class SignatureConsistencyReport(
    val repeatabilityScore: Float, // 0.0f a 100.0f
    val isConsistent: Boolean,
    val strokeCountMatch: Boolean,
    val durationVariationPercent: Float,
    val speedRatio: Float,
    val aspectVariationPercent: Float,
    val feedbackTitle: String,
    val feedbackDetails: String,
    val ergonomicTip: String
)
