package com.scribe.caligrafia.teacher.repository

import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalTeacherRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: LocalTeacherRepository

    @Before
    fun setup() {
        val baseDir = tempFolder.newFolder("scribe_test_teacher")
        repository = LocalTeacherRepository(baseDir)
    }

    @Test
    fun `getLatestDiagnostic returns fallback diagnostic when no prior data exists`() = runBlocking {
        val diag = repository.getLatestDiagnostic()

        assertNotNull(diag)
        assertEquals(4, diag.dimensions.size)
        assertTrue(diag.overallScore in 0f..100f)
    }

    @Test
    fun `saveDiagnostic and getLatestDiagnostic persists and reloads correctly`() = runBlocking {
        val initial = repository.getLatestDiagnostic()
        val modified = initial.copy(
            id = "diag_saved_custom",
            overallScore = 91.5f,
            primaryWeakness = BiomechanicalDimension.SLANT_STABILITY
        )

        repository.saveDiagnostic(modified)
        val loaded = repository.getLatestDiagnostic()

        assertEquals("diag_saved_custom", loaded.id)
        assertEquals(91.5f, loaded.overallScore, 0.01f)
        assertEquals(BiomechanicalDimension.SLANT_STABILITY, loaded.primaryWeakness)
    }

    @Test
    fun `savePrescription and markPrescriptionCompleted updates completion state`() = runBlocking {
        val presc = repository.getLatestPrescription()
        assertFalse(presc.isCompleted)

        repository.markPrescriptionCompleted(presc.id)
        val updated = repository.getLatestPrescription()

        assertTrue(updated.isCompleted)
    }

    @Test
    fun `saveInsights and getRecentInsights preserves all entries`() = runBlocking {
        val insights = repository.getRecentInsights()
        assertTrue(insights.isNotEmpty())

        repository.saveInsights(insights)
        val reloaded = repository.getRecentInsights()

        assertEquals(insights.size, reloaded.size)
    }
}
