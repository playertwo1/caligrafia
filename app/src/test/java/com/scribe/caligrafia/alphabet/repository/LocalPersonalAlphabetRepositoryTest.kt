package com.scribe.caligrafia.alphabet.repository

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.style.model.StyleCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class LocalPersonalAlphabetRepositoryTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun initialization_loadsCanonicalGlyphsAndSeeds() = runBlocking {
        val baseDir = tempFolder.newFolder("alphabet_repo_1")
        val repo = LocalPersonalAlphabetRepository(baseDir, Dispatchers.Unconfined)

        val alphabet = repo.getAlphabet().first()
        // Deve conter 68 glifos canônicos
        assertEquals(68, alphabet.totalGlyphsCount)

        // Glifos semeados devem estar completos
        val glyphA = alphabet.glyphs["glyph_lower_a"]
        assertNotNull(glyphA)
        assertTrue(glyphA!!.isCompleted)
        assertNotNull(glyphA.activeVariant)

        // Deve carregar os traços da semente
        val strokes = repo.getVariantStrokes(glyphA.activeVariant!!.id)
        assertTrue(strokes.isNotEmpty())
    }

    @Test
    fun addVariant_persistsStrokesAndUpdatesGlyph() = runBlocking {
        val baseDir = tempFolder.newFolder("alphabet_repo_2")
        val repo = LocalPersonalAlphabetRepository(baseDir, Dispatchers.Unconfined)

        val testStrokes = listOf(
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(10f, 10f, 1000L, 0.5f, 0f, 0f),
                    StrokePoint(20f, 20f, 1050L, 0.5f, 0f, 0f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1050L,
                baseWidthPx = 5.0f
            )
        )

        val variant = repo.addVariant(
            glyphId = "glyph_digit_0",
            strokes = testStrokes,
            score = 95.0f,
            slantAngle = 60.0f,
            setFavorite = true
        )

        assertNotNull(variant)
        assertEquals("v1", variant.label)
        assertEquals(95.0f, variant.score, 0.1f)
        assertTrue(variant.isFavorite)

        // Verifica no fluxo reativo
        val updatedAlphabet = repo.getAlphabet().first()
        val glyph0 = updatedAlphabet.glyphs["glyph_digit_0"]
        assertNotNull(glyph0)
        assertEquals(1, glyph0!!.variants.size)
        assertEquals(95.0f, glyph0.bestScore, 0.1f)
        assertEquals(variant.id, glyph0.selectedVariantId)

        // Verifica se os traços foram gravados no disco em .scribe
        val loadedStrokes = repo.getVariantStrokes(variant.id)
        assertEquals(1, loadedStrokes.size)
        assertEquals(2, loadedStrokes[0].pointCount)
    }

    @Test
    fun setFavoriteVariant_updatesActiveVariant() = runBlocking {
        val baseDir = tempFolder.newFolder("alphabet_repo_3")
        val repo = LocalPersonalAlphabetRepository(baseDir, Dispatchers.Unconfined)

        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(0f, 0f, 100L, 0.5f, 0f, 0f), StrokePoint(5f, 5f, 150L, 0.5f, 0f, 0f)),
            startedAtMs = 100L,
            endedAtMs = 150L
        )

        val v1 = repo.addVariant("glyph_lower_b", listOf(stroke), 80f, 55f, setFavorite = true)
        val v2 = repo.addVariant("glyph_lower_b", listOf(stroke), 92f, 53f, setFavorite = false)

        val glyphInitial = repo.getGlyph("glyph_lower_b")
        assertEquals(v1.id, glyphInitial?.selectedVariantId)

        val success = repo.setFavoriteVariant("glyph_lower_b", v2.id)
        assertTrue(success)

        val glyphUpdated = repo.getGlyph("glyph_lower_b")
        assertEquals(v2.id, glyphUpdated?.selectedVariantId)
        assertTrue(glyphUpdated!!.variants.first { it.id == v2.id }.isFavorite)
        assertFalse(glyphUpdated.variants.first { it.id == v1.id }.isFavorite)
    }

    @Test
    fun deleteVariant_removesVariantAndCleansUpFile() = runBlocking {
        val baseDir = tempFolder.newFolder("alphabet_repo_4")
        val repo = LocalPersonalAlphabetRepository(baseDir, Dispatchers.Unconfined)

        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(0f, 0f, 100L, 0.5f, 0f, 0f), StrokePoint(5f, 5f, 150L, 0.5f, 0f, 0f)),
            startedAtMs = 100L,
            endedAtMs = 150L
        )

        val v1 = repo.addVariant("glyph_lower_c", listOf(stroke), 85f, 52f, setFavorite = true)
        val v2 = repo.addVariant("glyph_lower_c", listOf(stroke), 90f, 52f, setFavorite = true)

        val deleted = repo.deleteVariant("glyph_lower_c", v2.id)
        assertTrue(deleted)

        val glyph = repo.getGlyph("glyph_lower_c")
        assertEquals(1, glyph?.variants?.size)
        assertEquals(v1.id, glyph?.variants?.first()?.id)

        // Arquivo .scribe da variante excluída não deve existir
        val scribeFile = File(File(baseDir, "strokes"), "${v2.id}.scribe")
        assertFalse(scribeFile.exists())
    }

    @Test
    fun compileAndSavePersonalStyle_registersAndUpdatesManifest() = runBlocking {
        val baseDir = tempFolder.newFolder("alphabet_repo_5")
        val repo = LocalPersonalAlphabetRepository(baseDir, Dispatchers.Unconfined)

        val style = repo.compileAndSavePersonalStyle("Meu Estilo Real")
        assertNotNull(style)
        assertEquals("Meu Estilo Real", style.name)
        assertEquals(StyleCategory.PERSONAL, style.category)

        val updatedAlphabet = repo.getAlphabet().first()
        assertEquals(style.id, updatedAlphabet.lastCompiledStyleId)
        assertNotNull(updatedAlphabet.lastCompiledTimestamp)
    }
}
