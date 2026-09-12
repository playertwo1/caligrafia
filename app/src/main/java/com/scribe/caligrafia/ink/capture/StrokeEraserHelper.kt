package com.scribe.caligrafia.ink.capture

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Utilitário geométrico para detecção de interseção e apagamento por traço (stroke eraser).
 *
 * Princípio: operações puras em memória, alta performance através de Bounding Box (AABB)
 * e teste de distância ponto-a-segmento euclidiana.
 */
object StrokeEraserHelper {

    /**
     * Verifica se o caminho percorrido pela borracha atinge um determinado traço vetorial.
     *
     * @param eraserPoints Amostras do traço da borracha.
     * @param stroke Traço candidato a ser apagado.
     * @param eraserRadius Raio de contato da ponta da borracha (em pixels).
     */
    fun intersects(
        eraserPoints: List<StrokePoint>,
        stroke: Stroke,
        eraserRadius: Float = 24f
    ): Boolean {
        if (eraserPoints.isEmpty() || stroke.points.isEmpty()) return false

        // 1. Teste rápido de Bounding Box (AABB)
        val strokeBounds = computeBounds(stroke.points)
        val eraserBounds = computeBounds(eraserPoints)

        val expandedEraserLeft = eraserBounds[0] - eraserRadius
        val expandedEraserTop = eraserBounds[1] - eraserRadius
        val expandedEraserRight = eraserBounds[2] + eraserRadius
        val expandedEraserBottom = eraserBounds[3] + eraserRadius

        // Se os retângulos envolventes não se sobrepõem, rejeição imediata O(1)
        if (expandedEraserRight < strokeBounds[0] ||
            expandedEraserLeft > strokeBounds[2] ||
            expandedEraserBottom < strokeBounds[1] ||
            expandedEraserTop > strokeBounds[3]
        ) {
            return false
        }

        // 2. Traço de ponto único
        if (stroke.points.size == 1) {
            val pt = stroke.points[0]
            return isPointNearPolyline(pt.x, pt.y, eraserPoints, eraserRadius)
        }

        // 3. Teste bidirecional ponto-a-segmento
        // 3a. Pontos da borracha próximos aos segmentos do traço
        for (ep in eraserPoints) {
            if (isPointNearPolyline(ep.x, ep.y, stroke.points, eraserRadius)) {
                return true
            }
        }

        // 3b. Pontos do traço próximos aos segmentos da borracha (gestos rápidos de borracha)
        for (sp in stroke.points) {
            if (isPointNearPolyline(sp.x, sp.y, eraserPoints, eraserRadius)) {
                return true
            }
        }

        // 3c. Interseção direta entre segmentos
        for (i in 0 until eraserPoints.size - 1) {
            val e1 = eraserPoints[i]
            val e2 = eraserPoints[i + 1]
            for (j in 0 until stroke.points.size - 1) {
                val s1 = stroke.points[j]
                val s2 = stroke.points[j + 1]
                if (segmentsIntersect(e1.x, e1.y, e2.x, e2.y, s1.x, s1.y, s2.x, s2.y)) {
                    return true
                }
            }
        }

        return false
    }

    private fun isPointNearPolyline(
        px: Float,
        py: Float,
        polyline: List<StrokePoint>,
        radius: Float
    ): Boolean {
        if (polyline.size == 1) {
            val p = polyline[0]
            val dx = px - p.x
            val dy = py - p.y
            return (dx * dx + dy * dy) <= (radius * radius)
        }

        for (i in 0 until polyline.size - 1) {
            val p1 = polyline[i]
            val p2 = polyline[i + 1]
            val dist = distancePointToSegment(px, py, p1.x, p1.y, p2.x, p2.y)
            if (dist <= radius) {
                return true
            }
        }
        return false
    }

    private fun distancePointToSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy

        if (lengthSq == 0f) {
            val dpx = px - x1
            val dpy = py - y1
            return sqrt(dpx * dpx + dpy * dpy)
        }

        val t = (((px - x1) * dx + (py - y1) * dy) / lengthSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy

        val diffX = px - projX
        val diffY = py - projY
        return sqrt(diffX * diffX + diffY * diffY)
    }

    private fun segmentsIntersect(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ): Boolean {
        fun ccw(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float {
            return (cy - ay) * (bx - ax) - (by - ay) * (cx - ax)
        }

        val d1 = ccw(x1, y1, x2, y2, x3, y3)
        val d2 = ccw(x1, y1, x2, y2, x4, y4)
        val d3 = ccw(x3, y3, x4, y4, x1, y1)
        val d4 = ccw(x3, y3, x4, y4, x2, y2)

        return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
                ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
    }

    private fun computeBounds(points: List<StrokePoint>): FloatArray {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        return floatArrayOf(minX, minY, maxX, maxY)
    }
}
