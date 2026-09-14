package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke

/** Fonte explicitamente escolhida para preview/exportação. */
enum class SignatureExportSource(val displayName: String) {
    CURRENT_ATTEMPT("Tentativa atual"),
    SAVED_REFERENCE("Referência salva")
}

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
 * Não representa autenticação de identidade; compara somente medidas disponíveis dos traços.
 */
data class SignatureConsistencyReport(
    val repeatabilityScore: Float,
    val isConsistent: Boolean,
    val strokeCountMatch: Boolean,
    val durationVariationPercent: Float,
    val speedRatio: Float,
    val aspectVariationPercent: Float,
    val feedbackTitle: String,
    val feedbackDetails: String,
    val ergonomicTip: String
)
