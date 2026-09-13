package com.scribe.caligrafia.core.model

import com.scribe.caligrafia.style.model.ScribeStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Proporções clássicas de pauta caligráfica (Ascendente : Altura-X : Descendente).
 */
sealed class GuidelineRatio(
    val ascenderRatio: Float,
    val descenderRatio: Float,
    val displayName: String
) {
    /** Pauta escolar/infantil comum (1 : 1 : 1). */
    object Ratio111 : GuidelineRatio(1.0f, 1.0f, "1:1:1 (Escolar)")

    /** Pauta clássica Copperplate / English Roundhand (2 : 1 : 2). */
    object Ratio212 : GuidelineRatio(2.0f, 2.0f, "2:1:2 (Copperplate)")

    /** Pauta Itálica / Spencerian (1.5 : 1 : 1.5 equivalente a 3:2:3). */
    object Ratio323 : GuidelineRatio(1.5f, 1.5f, "3:2:3 (Itálica)")

    /** Proporção customizada. */
    class Custom(ascenderRatio: Float, descenderRatio: Float) :
        GuidelineRatio(ascenderRatio, descenderRatio, "Customizada")
}

/**
 * Configuração de linhas de inclinação (slant lines) para uniformidade de ângulo.
 *
 * @param angleDegrees Ângulo em relação à horizontal (ex: 52° para Copperplate, 68° para Spencerian, 90° para vertical).
 * @param spacingPx Espaçamento horizontal entre as linhas de inclinação sobre a linha de base.
 */
data class SlantConfig(
    val angleDegrees: Float = 52.0f,
    val spacingPx: Float = 80.0f
) {
    init {
        require(angleDegrees in 10.0f..170.0f) { "Ângulo de inclinação deve estar entre 10° e 170°" }
        require(spacingPx > 5.0f) { "Espaçamento entre guias de inclinação deve ser maior que 5px" }
    }
}

/**
 * Estilo visual de uma linha-guia individual.
 */
data class GuidelineLineStyle(
    val colorArgb: Long = 0xFF888888,
    val strokeWidthPx: Float = 1.5f,
    val isDashed: Boolean = false,
    val dashIntervals: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GuidelineLineStyle
        if (colorArgb != other.colorArgb) return false
        if (strokeWidthPx != other.strokeWidthPx) return false
        if (isDashed != other.isDashed) return false
        if (dashIntervals != null) {
            if (other.dashIntervals == null) return false
            if (!dashIntervals.contentEquals(other.dashIntervals)) return false
        } else if (other.dashIntervals != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = colorArgb.hashCode()
        result = 31 * result + strokeWidthPx.hashCode()
        result = 31 * result + isDashed.hashCode()
        result = 31 * result + (dashIntervals?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Uma faixa de pauta caligráfica completa (uma "linha de caderno").
 */
data class GuidelineBand(
    val bandIndex: Int,
    val ascenderY: Float,
    val xHeightY: Float,
    val baselineY: Float,
    val descenderY: Float
) {
    val totalHeight: Float
        get() = descenderY - ascenderY

    val xHeight: Float
        get() = baselineY - xHeightY

    val ascenderHeight: Float
        get() = xHeightY - ascenderY

    val descenderHeight: Float
        get() = descenderY - baselineY
}

/**
 * Representação de um segmento diagonal de inclinação (slant).
 */
data class SlantSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

/**
 * Configuração completa de pautas caligráficas para uma página ou canvas.
 */
data class GuidelineConfig(
    val xHeightPx: Float = 50.0f,
    val ratio: GuidelineRatio = GuidelineRatio.Ratio212,
    val slant: SlantConfig? = SlantConfig(52.0f, 80.0f),
    val interlineGapPx: Float = 40.0f,
    val topMarginPx: Float = 60.0f,
    val leftMarginPx: Float = 20.0f,
    val rightMarginPx: Float = 20.0f,
    val baselineStyle: GuidelineLineStyle = GuidelineLineStyle(
        colorArgb = 0xFF2A2A2A,
        strokeWidthPx = 2.0f,
        isDashed = false
    ),
    val xHeightStyle: GuidelineLineStyle = GuidelineLineStyle(
        colorArgb = 0xFF3F51B5,
        strokeWidthPx = 1.5f,
        isDashed = true,
        dashIntervals = floatArrayOf(8.0f, 8.0f)
    ),
    val ascenderStyle: GuidelineLineStyle = GuidelineLineStyle(
        colorArgb = 0xFF9E9E9E,
        strokeWidthPx = 1.0f,
        isDashed = true,
        dashIntervals = floatArrayOf(6.0f, 6.0f)
    ),
    val descenderStyle: GuidelineLineStyle = GuidelineLineStyle(
        colorArgb = 0xFF9E9E9E,
        strokeWidthPx = 1.0f,
        isDashed = true,
        dashIntervals = floatArrayOf(6.0f, 6.0f)
    ),
    val slantStyle: GuidelineLineStyle = GuidelineLineStyle(
        colorArgb = 0xFFB0BEC5,
        strokeWidthPx = 0.8f,
        isDashed = false
    )
) {
    init {
        require(xHeightPx > 5.0f) { "xHeightPx deve ser maior que 5px" }
        require(interlineGapPx >= 0.0f) { "interlineGapPx não pode ser negativo" }
        require(topMarginPx >= 0.0f) { "topMarginPx não pode ser negativo" }
    }

    /**
     * Calcula geometricamente todas as faixas de pauta que cabem na altura total da página.
     */
    fun computeBands(pageHeight: Float): List<GuidelineBand> {
        val bands = mutableListOf<GuidelineBand>()
        val ascenderPx = xHeightPx * ratio.ascenderRatio
        val descenderPx = xHeightPx * ratio.descenderRatio
        val bandHeight = ascenderPx + xHeightPx + descenderPx

        var currentY = topMarginPx
        var index = 0

        while (currentY + bandHeight <= pageHeight) {
            val ascY = currentY
            val xhY = ascY + ascenderPx
            val baseY = xhY + xHeightPx
            val descY = baseY + descenderPx

            bands.add(
                GuidelineBand(
                    bandIndex = index++,
                    ascenderY = ascY,
                    xHeightY = xhY,
                    baselineY = baseY,
                    descenderY = descY
                )
            )

            currentY = descY + interlineGapPx
        }

        return bands
    }

    /**
     * Calcula os segmentos diagonais de inclinação para uma faixa de pauta específica.
     */
    fun computeSlantSegments(pageWidth: Float, band: GuidelineBand): List<SlantSegment> {
        val slantConfig = slant ?: return emptyList()
        val segments = mutableListOf<SlantSegment>()

        // Converte ângulo para radianos (ângulo em relação à horizontal inferior)
        val angleRad = Math.toRadians(slantConfig.angleDegrees.toDouble())
        // dx = deltaY / tan(angleRad). Para Y crescendo para baixo:
        // no topo (ascenderY) x está adiantado em dxTop em relação à baseline
        val deltaY = band.totalHeight
        val tanA = tan(angleRad).toFloat()
        if (tanA == 0.0f) return emptyList()

        val totalDx = deltaY / tanA

        // Geramos linhas partindo da margem esquerda até a margem direita + margem de compensação do dx
        val minX = leftMarginPx - kotlin.math.abs(totalDx)
        val maxX = pageWidth - rightMarginPx + kotlin.math.abs(totalDx)

        var xBase = minX
        while (xBase <= maxX) {
            val startX = xBase + totalDx
            val startY = band.ascenderY
            val endX = xBase
            val endY = band.descenderY

            // Adiciona se ao menos parte do segmento interceptar a largura da página
            if ((startX in 0f..pageWidth) || (endX in 0f..pageWidth)) {
                segments.add(SlantSegment(startX, startY, endX, endY))
            }
            xBase += slantConfig.spacingPx
        }

        return segments
    }

    companion object {
        /** Preset clássico de caligrafia Copperplate (52°, proporção 2:1:2). */
        fun copperplate(xHeightPx: Float = 50f) = GuidelineConfig(
            xHeightPx = xHeightPx,
            ratio = GuidelineRatio.Ratio212,
            slant = SlantConfig(angleDegrees = 52.0f, spacingPx = 80.0f)
        )

        /** Preset escolar comum sem inclinação (proporção 1:1:1). */
        fun school(xHeightPx: Float = 45f) = GuidelineConfig(
            xHeightPx = xHeightPx,
            ratio = GuidelineRatio.Ratio111,
            slant = null
        )

        /** Preset Spencerian (68°, proporção 3:2:3 / 1.5:1:1.5). */
        fun spencerian(xHeightPx: Float = 40f) = GuidelineConfig(
            xHeightPx = xHeightPx,
            ratio = GuidelineRatio.Ratio323,
            slant = SlantConfig(angleDegrees = 68.0f, spacingPx = 70.0f)
        )

        /** Gera configuração de pautas a partir de um [ScribeStyle] (R05). */
        fun fromStyle(style: ScribeStyle, xHeightPx: Float = 50f): GuidelineConfig {
            val slant = if (style.defaultSlantAngle in 10f..170f && style.defaultSlantAngle != 90.0f) {
                SlantConfig(angleDegrees = style.defaultSlantAngle, spacingPx = 80.0f)
            } else {
                null
            }
            return GuidelineConfig(
                xHeightPx = xHeightPx,
                ratio = style.recommendedRatio,
                slant = slant
            )
        }
    }
}
