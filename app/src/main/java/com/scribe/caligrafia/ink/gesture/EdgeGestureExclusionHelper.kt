package com.scribe.caligrafia.ink.gesture

import android.graphics.Rect

/**
 * Representação pura desacoplada do framework para coordenadas de exclusão de gestos.
 */
data class ExclusionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun toRect(): Rect = Rect(left, top, right, bottom)
}

/**
 * Utilitário para cálculo de zonas de exclusão de gestos de sistema (Android 10+ / One UI).
 *
 * Bloqueia os gestos de "Voltar" (swipe a partir das bordas laterais esquerda e direita),
 * replicando o comportamento do Samsung Notes para permitir que o usuário escreva com a S Pen
 * ou apoie a palma nas bordas sem minimizar ou fechar o aplicativo acidentalmente.
 *
 * A borda inferior é preservada livre de exclusão para que a navegação do sistema
 * (gesto de deslizar para cima para ir para Home / Recents) continue funcionando normalmente por baixo.
 */
object EdgeGestureExclusionHelper {

    const val DEFAULT_EDGE_WIDTH_DP = 56
    const val MIN_EDGE_WIDTH_PX = 120

    /**
     * Calcula as coordenadas de exclusão para as bordas laterais esquerda e direita (desacoplado do framework).
     */
    fun computeEdgeExclusionBounds(
        width: Int,
        height: Int,
        density: Float,
        edgeWidthDp: Int = DEFAULT_EDGE_WIDTH_DP
    ): List<ExclusionBounds> {
        if (width <= 0 || height <= 0) return emptyList()

        val calculatedPx = (edgeWidthDp * density).toInt()
        val edgeWidthPx = calculatedPx.coerceAtLeast(MIN_EDGE_WIDTH_PX).coerceAtMost(width / 3)

        val leftBounds = ExclusionBounds(0, 0, edgeWidthPx, height)
        val rightBounds = ExclusionBounds(width - edgeWidthPx, 0, width, height)

        return listOf(leftBounds, rightBounds)
    }

    /**
     * Retorna a lista de android.graphics.Rect para consumo direto por ViewCompat.setSystemGestureExclusionRects.
     */
    fun computeEdgeExclusionRects(
        width: Int,
        height: Int,
        density: Float,
        edgeWidthDp: Int = DEFAULT_EDGE_WIDTH_DP
    ): List<Rect> {
        return computeEdgeExclusionBounds(width, height, density, edgeWidthDp).map { it.toRect() }
    }
}
