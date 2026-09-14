package com.scribe.caligrafia.expansions.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.Stroke
import java.util.Locale
import kotlin.math.min

/**
 * Exportador offline de assinaturas em SVG e PNG transparente.
 *
 * F5.07: SVG e PNG compartilham a mesma transformação uniforme de enquadramento. Os limites
 * incluem metade da espessura do traço, portanto coordenadas negativas, floreios largos e pontos
 * únicos não são cortados nem deformados.
 */
object SignatureExporter {

    private data class ExportTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float
    ) {
        fun x(value: Float): Float = value * scale + offsetX
        fun y(value: Float): Float = value * scale + offsetY
        fun width(value: Float): Float = value * scale
    }

    fun exportToSvg(
        strokes: List<Stroke>,
        width: Int,
        height: Int,
        strokeColorHex: String = "#1E293B"
    ): String {
        val w = width.coerceAtLeast(100)
        val h = height.coerceAtLeast(100)
        val transform = computeTransform(strokes, w, h)

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $w $h\" width=\"$w\" height=\"$h\">\n")
        sb.append("  <g id=\"signature-strokes\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n")

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            val baseWidth = (stroke.baseWidthPx ?: 3.5f).coerceAtLeast(1.5f)
            val exportedWidth = transform.width(baseWidth).coerceAtLeast(1f)

            if (pts.size == 1) {
                val p = pts[0]
                sb.append(
                    String.format(
                        Locale.US,
                        "    <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"%s\" />\n",
                        transform.x(p.x), transform.y(p.y), exportedWidth / 2f, strokeColorHex
                    )
                )
            } else {
                sb.append("    <path d=\"")
                for (i in pts.indices) {
                    val p = pts[i]
                    val px = transform.x(p.x)
                    val py = transform.y(p.y)
                    if (i == 0) {
                        sb.append(String.format(Locale.US, "M %.2f %.2f", px, py))
                    } else {
                        sb.append(String.format(Locale.US, " L %.2f %.2f", px, py))
                    }
                }
                sb.append(
                    String.format(
                        Locale.US,
                        "\" stroke=\"%s\" stroke-width=\"%.2f\" />\n",
                        strokeColorHex, exportedWidth
                    )
                )
            }
        }

        sb.append("  </g>\n")
        sb.append("</svg>\n")
        return sb.toString()
    }

    fun exportToTransparentPng(
        strokes: List<Stroke>,
        width: Int,
        height: Int,
        strokeColor: Int = 0xFF1E293B.toInt()
    ): Bitmap {
        val w = width.coerceAtLeast(200)
        val h = height.coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val transform = computeTransform(strokes, w, h)

        val paint = Paint().apply {
            isAntiAlias = true
            color = strokeColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            val baseWidth = (stroke.baseWidthPx ?: 3.5f).coerceAtLeast(1.5f)
            paint.strokeWidth = transform.width(baseWidth).coerceAtLeast(1f)

            if (pts.size == 1) {
                val p = pts[0]
                paint.style = Paint.Style.FILL
                canvas.drawCircle(
                    transform.x(p.x),
                    transform.y(p.y),
                    paint.strokeWidth / 2f,
                    paint
                )
                paint.style = Paint.Style.STROKE
            } else {
                val path = Path()
                path.moveTo(transform.x(pts[0].x), transform.y(pts[0].y))
                for (i in 1 until pts.size) {
                    path.lineTo(transform.x(pts[i].x), transform.y(pts[i].y))
                }
                canvas.drawPath(path, paint)
            }
        }

        return bitmap
    }

    private fun computeTransform(strokes: List<Stroke>, width: Int, height: Int): ExportTransform {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var hasPoints = false

        for (stroke in strokes) {
            val halfWidth = (stroke.baseWidthPx ?: 3.5f).coerceAtLeast(1.5f) / 2f
            for (p in stroke.points) {
                hasPoints = true
                minX = kotlin.math.min(minX, p.x - halfWidth)
                minY = kotlin.math.min(minY, p.y - halfWidth)
                maxX = kotlin.math.max(maxX, p.x + halfWidth)
                maxY = kotlin.math.max(maxY, p.y + halfWidth)
            }
        }

        if (!hasPoints) {
            return ExportTransform(scale = 1f, offsetX = 0f, offsetY = 0f)
        }

        val contentWidth = (maxX - minX).coerceAtLeast(1f)
        val contentHeight = (maxY - minY).coerceAtLeast(1f)
        val padding = min(width, height) * 0.05f
        val availableWidth = (width - 2f * padding).coerceAtLeast(1f)
        val availableHeight = (height - 2f * padding).coerceAtLeast(1f)
        val scale = min(availableWidth / contentWidth, availableHeight / contentHeight).coerceAtLeast(0.0001f)

        val renderedWidth = contentWidth * scale
        val renderedHeight = contentHeight * scale
        val offsetX = (width - renderedWidth) / 2f - minX * scale
        val offsetY = (height - renderedHeight) / 2f - minY * scale

        return ExportTransform(scale = scale, offsetX = offsetX, offsetY = offsetY)
    }
}
