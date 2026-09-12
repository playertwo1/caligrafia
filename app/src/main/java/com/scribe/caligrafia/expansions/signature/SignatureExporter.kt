package com.scribe.caligrafia.expansions.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.scribe.caligrafia.core.model.Stroke
import java.util.Locale

/**
 * Exportador profissional de assinaturas em formatos vetoriais (SVG) e bitmap transparente (PNG).
 * 100% offline e sem bibliotecas externas.
 */
object SignatureExporter {

    /**
     * Gera uma representação vetorial em SVG (Scalable Vector Graphics) pura a partir dos traços.
     */
    fun exportToSvg(
        strokes: List<Stroke>,
        width: Int,
        height: Int,
        strokeColorHex: String = "#1E293B"
    ): String {
        val w = width.coerceAtLeast(100)
        val h = height.coerceAtLeast(100)

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $w $h\" width=\"$w\" height=\"$h\">\n")
        sb.append("  <g id=\"signature-strokes\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n")

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            val baseWidth = (stroke.baseWidthPx ?: 3.5f).coerceAtLeast(1.5f)

            if (pts.size == 1) {
                val p = pts[0]
                sb.append(
                    String.format(
                        Locale.US,
                        "    <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"%s\" />\n",
                        p.x, p.y, baseWidth / 2f, strokeColorHex
                    )
                )
            } else {
                sb.append("    <path d=\"")
                for (i in pts.indices) {
                    val p = pts[i]
                    if (i == 0) {
                        sb.append(String.format(Locale.US, "M %.2f %.2f", p.x, p.y))
                    } else {
                        sb.append(String.format(Locale.US, " L %.2f %.2f", p.x, p.y))
                    }
                }
                sb.append(
                    String.format(
                        Locale.US,
                        "\" stroke=\"%s\" stroke-width=\"%.2f\" />\n",
                        strokeColorHex, baseWidth
                    )
                )
            }
        }

        sb.append("  </g>\n")
        sb.append("</svg>\n")
        return sb.toString()
    }

    /**
     * Gera um Bitmap com canal alfa transparente para assinatura digital em documentos e contratos.
     */
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

            paint.strokeWidth = (stroke.baseWidthPx ?: 3.5f).coerceAtLeast(2f)

            if (pts.size == 1) {
                val p = pts[0]
                canvas.drawCircle(p.x, p.y, paint.strokeWidth / 2f, paint)
            } else {
                val path = Path()
                path.moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    path.lineTo(pts[i].x, pts[i].y)
                }
                canvas.drawPath(path, paint)
            }
        }

        return bitmap
    }
}
