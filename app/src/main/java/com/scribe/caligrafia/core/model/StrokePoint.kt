package com.scribe.caligrafia.core.model

/**
 * Representação imutável de uma amostra física pontual capturada via MotionEvent.
 *
 * @param x Coordenada X local em pixels.
 * @param y Coordenada Y local em pixels.
 * @param tMs Timestamp do evento em milissegundos (eventTime).
 * @param pressure Pressão física normalizada (0.0 a 1.0) ou null se o hardware não fornecer.
 * @param tiltRad Inclinação da caneta em radianos (0.0 a PI/2) ou null se não disponível.
 * @param orientationRad Orientação azimutal da caneta em radianos (-PI a PI) ou null se não disponível.
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val tMs: Long,
    val pressure: Float? = null,
    val tiltRad: Float? = null,
    val orientationRad: Float? = null
)
