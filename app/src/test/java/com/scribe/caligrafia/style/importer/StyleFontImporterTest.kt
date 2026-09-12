package com.scribe.caligrafia.style.importer

import com.scribe.caligrafia.style.model.StyleCategory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class StyleFontImporterTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("scribe_font_test_").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun isValidFontFile_detectsTrueTypeAndOpenTypeMagicBytes() {
        // 1. TrueType válido: 0x00, 0x01, 0x00, 0x00 seguido de tabelas
        val ttfFile = File(tempDir, "sample.ttf").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        }
        assertTrue("Deve reconhecer arquivo TrueType válido", StyleFontImporter.isValidFontFile(ttfFile))

        // 2. OpenType válido: 'OTTO'
        val otfFile = File(tempDir, "sample.otf").apply {
            writeBytes(byteArrayOf('O'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte(), 0, 0, 0, 0, 0, 0, 0, 0))
        }
        assertTrue("Deve reconhecer arquivo OpenType válido", StyleFontImporter.isValidFontFile(otfFile))

        // 3. Arquivo aleatório / inválido
        val txtFile = File(tempDir, "not_a_font.txt").apply {
            writeText("Este arquivo é texto simples e não contém cabeçalho de fonte.")
        }
        assertFalse("Não deve aceitar arquivo de texto", StyleFontImporter.isValidFontFile(txtFile))

        // 4. Arquivo vazio / muito curto
        val emptyFile = File(tempDir, "empty.ttf").apply { writeBytes(byteArrayOf(0x00, 0x01)) }
        assertFalse("Não deve aceitar arquivo truncado", StyleFontImporter.isValidFontFile(emptyFile))
    }

    @Test
    fun importFont_createsValidCustomStyle_and_rejectsCorruptFiles() {
        val ttfFile = File(tempDir, "Minha_Fonte_Elegante.ttf").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        }

        val style = StyleFontImporter.importFont(ttfFile, customName = "Minha Fonte Elegante", slantAngle = 55.0f)
        assertEquals("font_minha_fonte_elegante", style.id)
        assertEquals("Minha Fonte Elegante", style.name)
        assertEquals(StyleCategory.CUSTOM_FONT, style.category)
        assertEquals(55.0f, style.defaultSlantAngle, 0.001f)
        assertTrue(style.isCustom)
        assertEquals(ttfFile.absolutePath, style.customFontPath)

        // Tentativa com arquivo inválido deve lançar IOException
        val corruptFile = File(tempDir, "corrupted.ttf").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        assertThrows(IOException::class.java) {
            StyleFontImporter.importFont(corruptFile)
        }
    }
}
