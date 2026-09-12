package com.scribe.caligrafia.teacher.engine

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.MaturityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachingFeedbackEngineTest {

    private val feedbackEngine = CoachingFeedbackEngine()

    private fun createSampleDiagnostic(): BiomechanicalDiagnostic {
        val dims = BiomechanicalDimension.values().associateWith { dim ->
            val score = if (dim == BiomechanicalDimension.PRESSURE_CONTROL) 40f else 88f
            DimensionEvaluation(
                dimension = dim,
                score = score,
                observedValue = score,
                targetValue = 100f,
                status = if (dim == BiomechanicalDimension.PRESSURE_CONTROL) EvaluationStatus.NEEDS_ATTENTION else EvaluationStatus.EXCELLENT,
                shortDiagnosis = "Diagnóstico $dim"
            )
        }
        return BiomechanicalDiagnostic(
            overallScore = 76f,
            maturityLevel = MaturityLevel.DEVELOPING_CALLIGRAPHER,
            dimensions = dims,
            primaryWeakness = BiomechanicalDimension.PRESSURE_CONTROL,
            primaryStrength = BiomechanicalDimension.SLANT_STABILITY,
            totalStrokesAnalyzed = 45,
            totalAttemptsAnalyzed = 8
        )
    }

    @Test
    fun `generateInsights outputs all pedagogical insight categories`() {
        val diag = createSampleDiagnostic()
        val insights = feedbackEngine.generateInsights(diag)

        assertTrue(insights.size >= 4)
        assertTrue(insights.any { it.type == InsightType.PRAISE })
        assertTrue(insights.any { it.type == InsightType.CORRECTION })
        assertTrue(insights.any { it.type == InsightType.ERGONOMIC_TIP })
        assertTrue(insights.any { it.type == InsightType.CHALLENGE })
    }

    @Test
    fun `insights contain non-blank titles, messages and actionable guidance`() {
        val diag = createSampleDiagnostic()
        val insights = feedbackEngine.generateInsights(diag)

        for (ins in insights) {
            assertTrue(ins.title.isNotBlank())
            assertTrue(ins.message.isNotBlank())
            assertNotNull(ins.type)
        }
    }

    @Test
    fun `ergonomic tip is tailored to pressure control weakness`() {
        val diag = createSampleDiagnostic()
        val insights = feedbackEngine.generateInsights(diag)
        val ergo = insights.firstOrNull { it.type == InsightType.ERGONOMIC_TIP }

        assertNotNull(ergo)
        assertTrue(ergo?.message?.contains("S Pen", ignoreCase = true) == true)
        assertEquals(BiomechanicalDimension.PRESSURE_CONTROL, ergo?.relatedDimension)
    }
}
