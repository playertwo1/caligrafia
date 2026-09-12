package com.scribe.caligrafia.style.importer

import android.graphics.Typeface
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.style.model.DuctusRule
import com.scribe.caligrafia.style.model.PressureBehavior
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.style.model.StyleCategory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Importador de fontes TrueType (.ttf) e OpenType (.otf) locais para o Scribe (SCR-022).
 *
 * Princípio Arquitetural Inviolável:
 * A fonte importada atua estritamente como gabarito estético e visual para prévia, pautas e modelos de letras.
 * Os traços manuais da S Pen continuam sendo vetores brutos de alta densidade (`Stroke` / `StrokePoint`)
 * e JAMAIS são substituídos por glifos de fonte ou bitmaps estáticos.
 */
object StyleFontImporter {

    /**
     * Valida os Magic Bytes de um arquivo de fonte TTF ou OTF.
     *
     * TrueType: 0x00010000 ou 0x74727565 ('true')
     * OpenType: 0x4F54544F ('OTTO')
     */
    fun isValidFontFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 12) return false

        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                if (read < 4) return false

                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF
                val b2 = header[2].toInt() and 0xFF
                val b3 = header[3].toInt() and 0xFF

                // TrueType: 0x00, 0x01, 0x00, 0x00
                val isTrueType = (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00)
                // TrueType OS X: 'true' (0x74, 0x72, 0x75, 0x65)
                val isTrueTypeAlt = (b0 == 0x74 && b1 == 0x72 && b2 == 0x75 && b3 == 0x65)
                // OpenType: 'OTTO' (0x4F, 0x54, 0x54, 0x4F)
                val isOpenType = (b0 == 0x4F && b1 == 0x54 && b2 == 0x54 && b3 == 0x4F)

                isTrueType || isTrueTypeAlt || isOpenType
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Importa um arquivo de fonte local e constrói a definição de estilo derivada [ScribeStyle].
     *
     * @param file Arquivo .ttf ou .otf local válido.
     * @param customName Nome de exibição opcional (se omitido, usa o nome do arquivo).
     * @param slantAngle Inclinação estimada do estilo visual (padrão 52° ou 68° para itálicos/cursivos).
     * @return O [ScribeStyle] associado ao gabarito visual importado.
     * @throws IOException Se o arquivo não for acessível ou tiver formato inválido.
     */
    fun importFont(
        file: File,
        customName: String? = null,
        slantAngle: Float = 60.0f
    ): ScribeStyle {
        if (!isValidFontFile(file)) {
            throw IOException("Arquivo de fonte inválido ou corrompido: ${file.name}. Formatos suportados: .ttf e .otf")
        }

        val styleName = customName ?: file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
        val styleId = "font_" + file.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9_]"), "_")

        return ScribeStyle(
            id = styleId,
            name = styleName,
            description = "Estilo importado a partir da fonte ${file.name}. Utilizado como gabarito estético e visual para prática caligráfica.",
            category = StyleCategory.CUSTOM_FONT,
            recommendedRatio = GuidelineRatio.Ratio212,
            defaultSlantAngle = slantAngle,
            recommendedStrokeWidthPx = 3.5f,
            contrastRatio = 1.5f,
            sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
            ductusRules = listOf(
                DuctusRule(
                    ruleIndex = 1,
                    title = "Reprodução de Gabarito Visual",
                    instruction = "Pratique sobrepondo os traços vetoriais da S Pen ao desenho de referência da fonte importada.",
                    pressureBehavior = PressureBehavior.UNIFORM
                )
            ),
            customFontPath = file.absolutePath,
            isCustom = true
        )
    }

    /**
     * Carrega com segurança o [Typeface] do Android para um estilo com fonte customizada,
     * com fallback automático para [Typeface.DEFAULT] se o arquivo tiver sido movido ou excluído.
     */
    fun loadTypefaceSafe(style: ScribeStyle): Typeface {
        val path = style.customFontPath ?: return Typeface.DEFAULT
        val fontFile = File(path)
        if (!fontFile.exists() || !fontFile.canRead()) {
            return Typeface.DEFAULT
        }

        return try {
            Typeface.createFromFile(fontFile) ?: Typeface.DEFAULT
        } catch (_: Throwable) {
            Typeface.DEFAULT
        }
    }
}
