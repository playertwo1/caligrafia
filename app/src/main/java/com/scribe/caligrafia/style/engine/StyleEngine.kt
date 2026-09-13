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
    private val customFontsDir: File? = null,
    private val storageDir: File? = customFontsDir?.parentFile ?: customFontsDir
) {
    private val customStyles = mutableMapOf<String, ScribeStyle>()
    private val personalStylesFile: File? = storageDir?.let { File(it, "personal_styles.json") }

    init {
        scanCustomFonts()
        loadPersonalStyles()
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

        // 2. Verifica nos estilos customizados importados ou pessoais
        val custom = customStyles[styleId]
        if (custom != null) {
            if (custom.category == com.scribe.caligrafia.style.model.StyleCategory.PERSONAL) {
                return custom
            }
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
     * F3.22 & F3.23: Importa fonte a partir de um fluxo de entrada (ex: seletor de documentos Android),
     * copiando o arquivo de forma atômica para o diretório interno do aplicativo (custom_fonts/),
     * garantindo validação de Magic Bytes e persistência offline sem perdas.
     */
    fun importCustomFontStream(
        inputStream: java.io.InputStream,
        originalFileName: String,
        customName: String? = null,
        slantAngle: Float = 60.0f
    ): Result<ScribeStyle> {
        return try {
            val fontsDir = customFontsDir ?: File(storageDir ?: File("."), "custom_fonts")
            if (!fontsDir.exists()) fontsDir.mkdirs()

            val sanitizedName = originalFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(fontsDir, sanitizedName)
            val tempFile = File.createTempFile("font_import_", ".tmp", fontsDir)

            tempFile.outputStream().use { fos ->
                inputStream.copyTo(fos)
                fos.flush()
            }

            if (!StyleFontImporter.isValidFontFile(tempFile)) {
                tempFile.delete()
                return Result.failure(java.io.IOException("Arquivo não é uma fonte TrueType (.ttf) ou OpenType (.otf) válida."))
            }

            java.nio.file.Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            )

            importCustomFont(targetFile, customName, slantAngle)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Registra ou atualiza um estilo customizado (ex: estilo compilado pelo PersonalStyleCompiler).
     */
    fun registerCustomStyle(style: ScribeStyle) {
        customStyles[style.id] = style
        if (style.category == com.scribe.caligrafia.style.model.StyleCategory.PERSONAL) {
            savePersonalStyles()
        }
    }

    /**
     * Carrega estilos pessoais compilados a partir de arquivo persistido em disco.
     */
    fun loadPersonalStyles(file: File? = personalStylesFile) {
        val candidateFiles = if (file != null) {
            listOf(file)
        } else {
            listOfNotNull(
                personalStylesFile,
                storageDir?.let { File(it, "personal_styles.json") },
                customFontsDir?.let { File(it, "personal_styles.json") },
                customFontsDir?.parentFile?.let { File(it, "personal_styles.json") }
            ).distinct()
        }

        for (target in candidateFiles) {
            if (!target.exists() || !target.isFile) continue
            try {
                val content = target.readText(Charsets.UTF_8)
                val styles = PersonalStyleSerializer.deserialize(content)
                for (s in styles) {
                    customStyles[s.id] = s
                }
            } catch (_: Throwable) {
                // Ignora se o arquivo estiver corrompido
            }
        }
    }

    /**
     * Salva estilos pessoais compilados em disco.
     */
    fun savePersonalStyles(file: File? = personalStylesFile) {
        val target = file ?: return
        val personalStyles = customStyles.values.filter { it.category == com.scribe.caligrafia.style.model.StyleCategory.PERSONAL }
        val parent = target.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        try {
            val json = PersonalStyleSerializer.serialize(personalStyles)
            target.writeText(json, Charsets.UTF_8)
        } catch (_: Throwable) {
            // Falhas de IO não devem quebrar o fluxo
        }
    }

    /**
     * Remove um estilo customizado importado.
     */
    fun removeCustomStyle(styleId: String): Boolean {
        val removed = customStyles.remove(styleId) != null
        if (removed) {
            savePersonalStyles()
        }
        return removed
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
