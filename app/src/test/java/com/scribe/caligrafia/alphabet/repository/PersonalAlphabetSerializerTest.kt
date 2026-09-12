package com.scribe.caligrafia.alphabet.repository

import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalAlphabetSerializerTest {

    @Test
    fun serializeAndDeserialize_preservesAllMetadata() {
        val variant1 = GlyphVariant(
            id = "v1",
            glyphId = "glyph_lower_a",
            version = 1,
            label = "v1",
            score = 88.5f,
            slantAngleDegrees = 52.0f,
            isFavorite = false,
            strokeCount = 2,
            createdAtTimestamp = 1700000000000L
        )
        val variant2 = GlyphVariant(
            id = "v2",
            glyphId = "glyph_lower_a",
            version = 2,
            label = "v2",
            score = 94.0f,
            slantAngleDegrees = 53.0f,
            isFavorite = true,
            strokeCount = 2,
            createdAtTimestamp = 1700000050000L
        )

        val glyphA = PersonalGlyph(
            id = "glyph_lower_a",
            symbol = "a",
            name = "Letra 'a'",
            category = AlphabetCategory.LOWERCASE,
            selectedVariantId = "v2",
            variants = listOf(variant1, variant2),
            bestScore = 94.0f,
            updatedAtTimestamp = 1700000050000L
        )

        val alphabet = PersonalAlphabet(
            id = "test_alphabet",
            name = "Meu Caderno de Letras",
            glyphs = mapOf("glyph_lower_a" to glyphA),
            lastCompiledStyleId = "style_123",
            lastCompiledTimestamp = 1700000100000L
        )

        val json = PersonalAlphabetSerializer.serialize(alphabet)
        assertTrue(json.contains("\"test_alphabet\""))
        assertTrue(json.contains("\"glyph_lower_a\""))

        val restored = PersonalAlphabetSerializer.deserialize(json)
        assertEquals("test_alphabet", restored.id)
        assertEquals("Meu Caderno de Letras", restored.name)
        assertEquals("style_123", restored.lastCompiledStyleId)
        assertEquals(1700000100000L, restored.lastCompiledTimestamp)

        val restoredGlyph = restored.glyphs["glyph_lower_a"]
        assertNotNull(restoredGlyph)
        assertEquals("a", restoredGlyph!!.symbol)
        assertEquals(AlphabetCategory.LOWERCASE, restoredGlyph.category)
        assertEquals("v2", restoredGlyph.selectedVariantId)
        assertEquals(2, restoredGlyph.variants.size)

        val restoredV2 = restoredGlyph.variants.first { it.id == "v2" }
        assertEquals(2, restoredV2.version)
        assertEquals("v2", restoredV2.label)
        assertEquals(94.0f, restoredV2.score, 0.01f)
        assertEquals(53.0f, restoredV2.slantAngleDegrees, 0.01f)
        assertTrue(restoredV2.isFavorite)
    }

    @Test
    fun deserialize_emptyJson_returnsDefaultAlphabet() {
        val json = "{}"
        val restored = PersonalAlphabetSerializer.deserialize(json)
        assertNotNull(restored)
        assertTrue(restored.glyphs.isEmpty())
    }
}
