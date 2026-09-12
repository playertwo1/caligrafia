package com.scribe.caligrafia.style.engine

import com.scribe.caligrafia.style.importer.StyleFontImporter
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.style.model.ScribeStyle
import java.io.File

/**
 * Motor de Gerenciamento e Seleção de Estilos Caligráficos do Scribe (SCR-023).
 *
 * Responsabilidades:
 * 1. Manter o registro unificado de estilos nativos e estilos derivados de fontes locais importadas.
 * 2. Garantir fallback gracioso caso um estilo solicitado ou arquivo de fonte customizado seja inacessível.
 * 3. Prover estilos para o Caderno Livre e o Treino Guiado.
 */
class StyleEngine(
    private val customFontsDir: File? = null
) {
    private val customStyles = mutableMapOf<String, ScribeStyle>()

    init {
        scanCustomFonts()
    }

    /**
     * Retorna a lista completa de estilos disponíveis (nativos pré-instalados + expandidos M8 + fontes customizadas).
     */
    fun getAvailableStyles(): List<ScribeStyle> {
        return BuiltInStyles.ALL + com.scribe.caligrafia.expansions.styles.ExpandedStyles.allExpandedStyles + customStyles.values.toList()
    }

    /**
     * Obtém um estilo pelo identificador único.
     * Caso o estilo não exista ou seu arquivo tenha sido corrompido/removido,
     * executa fallback gracioso e seguro para a Cursiva Escolar.
     */
    fun getStyle(styleId: String?): ScribeStyle {
        if (styleId == null) return defaultStyle()

        // 1. Verifica nos estilos pré-instalados e expandidos
        val builtIn = BuiltInStyles.ALL.firstOrNull { it.id == styleId }
        if (builtIn != null) return builtIn

        val expanded = com.scribe.caligrafia.expansions.styles.ExpandedStyles.allExpandedStyles.firstOrNull { it.id == styleId }
        if (expanded != null) return expanded

        // 2. Verifica nos estilos customizados importados
        val custom = customStyles[styleId]
        if (custom != null) {
            val path = custom.customFontPath
            if (path != null && File(path).exists()) {
                return custom
            } else {
                // Arquivo foi excluído do armazenamento: remove do cache e faz fallback
                customStyles.remove(styleId)
            }
        }

        // 3. Fallback gracioso
        return defaultStyle()
    }

    /**
     * Importa e registra uma nova fonte local TTF ou OTF como estilo visual.
     *
     * @param fontFile Arquivo local válido.
     * @param customName Nome amigável de exibição opcional.
     * @param slantAngle Ângulo de inclinação sugerido para o gabarito.
     */
    fun importCustomFont(
        fontFile: File,
        customName: String? = null,
        slantAngle: Float = 60.0f
    ): Result<ScribeStyle> {
        return try {
            val style = StyleFontImporter.importFont(fontFile, customName, slantAngle)
            customStyles[style.id] = style
            Result.success(style)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Registra ou atualiza um estilo customizado (ex: estilo compilado pelo PersonalStyleCompiler).
     */
    fun registerCustomStyle(style: ScribeStyle) {
        customStyles[style.id] = style
    }

    /**
     * Remove um estilo customizado importado.
     */
    fun removeCustomStyle(styleId: String): Boolean {
        return customStyles.remove(styleId) != null
    }

    /**
     * Estilo canônico padrão do aplicativo.
     */
    fun defaultStyle(): ScribeStyle = BuiltInStyles.CURSIVA_ESCOLAR

    /**
     * Varre o diretório de fontes personalizadas caso configurado.
     */
    private fun scanCustomFonts() {
        val dir = customFontsDir ?: return
        if (!dir.exists() || !dir.isDirectory) return

        val fontFiles = dir.listFiles { f ->
            f.isFile && (f.extension.equals("ttf", ignoreCase = true) || f.extension.equals("otf", ignoreCase = true))
        } ?: return

        for (file in fontFiles) {
            if (StyleFontImporter.isValidFontFile(file)) {
                try {
                    val style = StyleFontImporter.importFont(file)
                    customStyles[style.id] = style
                } catch (_: Throwable) {
                    // Ignora arquivos ilegíveis
                }
            }
        }
    }
}
