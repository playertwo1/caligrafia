package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.MaturityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachingCurriculumGeneratorTest {

    private val generator = CoachingCurriculumGenerator()

    private fun createDiagnosticWithWeakness(
        weakness: BiomechanicalDimension,
        level: MaturityLevel = MaturityLevel.PRACTITIONER
    ): BiomechanicalDiagnostic {
        val dims = BiomechanicalDimension.values().associateWith { dim ->
            val score = if (dim == weakness) 45f else 85f
            DimensionEvaluation(
                dimension = dim,
                score = score,
                observedValue = score,
                targetValue = 100f,
                status = if (dim == weakness) EvaluationStatus.NEEDS_ATTENTION else EvaluationStatus.EXCELLENT,
                shortDiagnosis = "Diagnóstico para $dim"
            )
        }
        return BiomechanicalDiagnostic(
            overallScore = 72f,
            maturityLevel = level,
            dimensions = dims,
            primaryWeakness = weakness,
            primaryStrength = BiomechanicalDimension.GUIDELINE_CONTAINMENT,
            totalStrokesAnalyzed = 50,
            totalAttemptsAnalyzed = 10
        )
    }

    @Test
    fun `prescribes slant practice when weakness is SLANT_STABILITY`() {
        val diag = createDiagnosticWithWeakness(BiomechanicalDimension.SLANT_STABILITY)
        val presc = generator.generatePrescription(diag)

        assertNotNull(presc)
        assertEquals(BiomechanicalDimension.SLANT_STABILITY, presc.targetDimension)
        assertEquals("basic_slant", presc.warmupExerciseId)
        assertTrue(presc.title.contains("Alinhamento", ignoreCase = true))
        assertFalse(presc.isCompleted)
    }

    @Test
    fun `prescribes containment practice when weakness is GUIDELINE_CONTAINMENT`() {
        val diag = createDiagnosticWithWeakness(BiomechanicalDimension.GUIDELINE_CONTAINMENT)
        val presc = generator.generatePrescription(diag)

        assertNotNull(presc)
        assertEquals(BiomechanicalDimension.GUIDELINE_CONTAINMENT, presc.targetDimension)
        assertTrue(presc.warmupExerciseId == "underturn" || presc.warmupExerciseId == "overturn")
        assertTrue(presc.title.contains("Contenção", ignoreCase = true))
    }

    @Test
    fun `prescribes cadence practice when weakness is RHYTHM_AND_CADENCE`() {
        val diag = createDiagnosticWithWeakness(BiomechanicalDimension.RHYTHM_AND_CADENCE)
        val presc = generator.generatePrescription(diag)

        assertNotNull(presc)
        assertEquals(BiomechanicalDimension.RHYTHM_AND_CADENCE, presc.targetDimension)
        assertEquals("compound_curve", presc.warmupExerciseId)
        assertEquals(15, presc.recommendedMinutes)
    }

    @Test
    fun `prescribes pressure modulation practice when weakness is PRESSURE_CONTROL`() {
        val diag = createDiagnosticWithWeakness(BiomechanicalDimension.PRESSURE_CONTROL)
        val presc = generator.generatePrescription(diag)

        assertNotNull(presc)
        assertEquals(BiomechanicalDimension.PRESSURE_CONTROL, presc.targetDimension)
        assertTrue(presc.title.contains("Pressão", ignoreCase = true) || presc.title.contains("Modulação", ignoreCase = true))
    }

    @Test
    fun `prescribes mastery challenge when user reaches MASTER_OF_STROKE`() {
        val diag = createDiagnosticWithWeakness(
            weakness = BiomechanicalDimension.SLANT_STABILITY,
            level = MaturityLevel.MASTER_OF_STROKE
        )
        val presc = generator.generatePrescription(diag)

        assertNotNull(presc)
        assertTrue(presc.title.contains("Mestria", ignoreCase = true))
        assertEquals(0.10f, presc.recommendedGhostLevel, 0.01f) // Marca d'água mínima
    }
}
