package com.scribe.caligrafia.evolution.repository

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Testes unitários do repositório local de tentativas caligráficas (SCR-501 a SCR-504).
 */
class LocalPracticeAttemptRepositoryTest {

    @Test
    fun init_startsEmptyWithoutSyntheticSeeds() {
        val tempDir = Files.createTempDirectory("scribe_attempts_test").toFile()
        try {
            val repository = LocalPracticeAttemptRepository(tempDir)

            val all = repository.getAllAttempts()
            assertTrue("Um perfil novo deve iniciar com zero tentativas sintéticas (R03)", all.isEmpty())

            val slantAttempts = repository.getAttemptsForTarget("basic_slant")
            assertTrue("Não deve conter tentativas para alvos antes do treino real", slantAttempts.isEmpty())

            val comparison = repository.getBeforeAndAfter("basic_slant")
            org.junit.Assert.assertNull("Não deve haver comparativo antes de tentativas reais", comparison)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun saveAttempt_persistsAndReloadsAcrossInstances() {
        val tempDir = Files.createTempDirectory("scribe_attempts_save_test").toFile()
        try {
            val repository = LocalPracticeAttemptRepository(tempDir)

            val stroke = Stroke(
                id = "s_new",
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(10f, 20f, 1000L, 0.5f),
                    StrokePoint(15f, 25f, 1050L, 0.6f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1050L
            )

            val customAttempt = PracticeAttemptRecord(
                attemptId = "custom_01",
                targetId = "letter_custom",
                targetTitle = "Letra Especial",
                timestampMs = System.currentTimeMillis(),
                strokes = listOf(stroke),
                scorePercent = 95,
                averageSlantDegrees = 52.0f,
                durationMs = 1200L,
                isBaseline = false
            )

            repository.saveAttempt(customAttempt)

            // Recarrega em uma nova instância do repositório
            val reloadedRepo = LocalPracticeAttemptRepository(tempDir)
            val loaded = reloadedRepo.getAttemptsForTarget("letter_custom")
            assertEquals(1, loaded.size)
            assertEquals("Letra Especial", loaded.first().targetTitle)
            assertEquals(95, loaded.first().scorePercent)
            assertEquals(1, loaded.first().strokes.size)
            assertEquals(2, loaded.first().strokes.first().points.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
