package com.scribe.caligrafia.expansions.passage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassageCatalogTest {

    @Test
    fun passageCatalog_containsAllExpectedCategories() {
        val all = PassageCatalog.allPassages
        assertTrue("Catálogo deve possuir ao menos 6 textos clássicos", all.size >= 6)

        val pangrams = PassageCatalog.getByCategory(PassageCategory.PANGRAMS)
        assertTrue(pangrams.isNotEmpty())
        assertTrue(pangrams.any { it.id == "pangram_morcego" })

        val poetry = PassageCatalog.getByCategory(PassageCategory.CLASSIC_POETRY)
        assertTrue(poetry.isNotEmpty())
        assertTrue(poetry.any { it.author == "Luís de Camões" })

        val quotes = PassageCatalog.getByCategory(PassageCategory.FAMOUS_QUOTES)
        assertTrue(quotes.isNotEmpty())
    }

    @Test
    fun passageItem_countsWordsAccurately() {
        val passage = PassageCatalog.getById("pangram_morcego")
        assertNotNull(passage)
        // "O veloz morcego negro das asas de veludo" (8) + "voava sobre a antiga quinta do fazendeiro." (7) = 15
        assertEquals(15, passage?.totalWordCount)
    }

    @Test
    fun passagePacingEngine_optimalCadence_yieldsHighScore() {
        val passage = PassageCatalog.getById("pangram_morcego")!!
        // 15 palavras em aproximadamente 1 minuto (~15 WPM para meta de 16 WPM)
        val durationMs = 60_000L

        val result = PassagePacingEngine.evaluatePacing(passage, durationMs)
        assertEquals(15, result.totalWords)
        assertEquals(15.0f, result.actualWpm, 0.1f)
        assertTrue("Score deve ser alto quando próximo da meta de WPM", result.pacingScore >= 85f)
    }

    @Test
    fun passagePacingEngine_excessiveSpeed_penalizesScore() {
        val passage = PassageCatalog.getById("pangram_morcego")!!
        // 15 palavras em apenas 10 segundos (90 WPM vs meta de 16)
        val durationMs = 10_000L

        val result = PassagePacingEngine.evaluatePacing(passage, durationMs)
        assertTrue("WPM deve refletir escrita apressada", result.actualWpm > 50f)
        assertTrue("Score deve ser penalizado por escrita rápida demais", result.pacingScore < 70f)
        assertTrue(result.diagnosis.contains("Acelerada"))
    }
}
