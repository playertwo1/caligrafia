package com.scribe.caligrafia.style.engine

import com.scribe.caligrafia.style.model.BuiltInStyles
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StyleEngineTest {

    private lateinit var tempDir: File
    private lateinit var engine: StyleEngine

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("scribe_style_engine_test_").toFile()
        engine = StyleEngine(customFontsDir = tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun engineProvidesBuiltInStylesByDefault() {
        val styles = engine.getAvailableStyles()
        assertTrue("Deve listar os 3 estilos embutidos", styles.size >= 3)
        assertNotNull(engine.getStyle("cursiva_escolar"))
        assertNotNull(engine.getStyle("copperplate"))
        assertNotNull(engine.getStyle("spencerian"))
    }

    @Test
    fun getStyle_withUnknownOrNullId_executesGracefulFallback() {
        val fallbackNull = engine.getStyle(null)
        assertEquals(BuiltInStyles.CURSIVA_ESCOLAR.id, fallbackNull.id)

        val fallbackNonExistent = engine.getStyle("estilo_fantasma_inexistente")
        assertEquals(BuiltInStyles.CURSIVA_ESCOLAR.id, fallbackNonExistent.id)
    }

    @Test
    fun importCustomFont_registersStyle_and_handlesMissingFileFallback() {
        val fontFile = File(tempDir, "script_teste.ttf").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0, 8, 0, 0, 0, 0, 0, 0))
        }

        val result = engine.importCustomFont(fontFile, customName = "Script Teste")
        assertTrue("Importação deve ter sucesso", result.isSuccess)
        val style = result.getOrThrow()

        assertEquals("font_script_teste", style.id)
        assertEquals(style, engine.getStyle("font_script_teste"))
        assertTrue("Estilo customizado deve estar na lista", engine.getAvailableStyles().any { it.id == style.id })

        // Se o arquivo de fonte for apagado fisicamente, o engine deve executar fallback gracioso
        fontFile.delete()
        val resolvedAfterDelete = engine.getStyle("font_script_teste")
        assertEquals("Deve retornar fallback para Cursiva Escolar após arquivo ser deletado", BuiltInStyles.CURSIVA_ESCOLAR.id, resolvedAfterDelete.id)
    }

    @Test
    fun removeCustomStyle_unregistersCorrectly() {
        val fontFile = File(tempDir, "custom.otf").apply {
            writeBytes(byteArrayOf('O'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte(), 0, 0, 0, 0, 0, 0, 0, 0))
        }
        val registered = engine.importCustomFont(fontFile).getOrThrow()
        assertTrue(engine.getAvailableStyles().any { it.id == registered.id })

        val removed = engine.removeCustomStyle(registered.id)
        assertTrue(removed)
        assertFalse(engine.getAvailableStyles().any { it.id == registered.id })
    }
}
