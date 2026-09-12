package com.scribe.caligrafia.teacher.repository

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.MaturityLevel
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherSerializerTest {

    @Test
    fun `serialize and deserialize diagnostic roundtrip`() {
        val dims = mapOf(
            BiomechanicalDimension.SLANT_STABILITY to DimensionEvaluation(
                dimension = BiomechanicalDimension.SLANT_STABILITY,
                score = 88.5f,
                observedValue = 52.1f,
                targetValue = 52.0f,
                status = EvaluationStatus.EXCELLENT,
                shortDiagnosis = "Inclinação exemplar a 52.1°."
            )
        )
        val original = BiomechanicalDiagnostic(
            id = "diag_test_123",
            timestampMs = 1726000000000L,
            overallScore = 84.5f,
            maturityLevel = MaturityLevel.DEVELOPING_CALLIGRAPHER,
            dimensions = dims,
            primaryWeakness = BiomechanicalDimension.PRESSURE_CONTROL,
            primaryStrength = BiomechanicalDimension.SLANT_STABILITY,
            totalStrokesAnalyzed = 42,
            totalAttemptsAnalyzed = 7
        )

        val json = TeacherSerializer.serializeDiagnostic(original)
        val deserialized = TeacherSerializer.deserializeDiagnostic(json)

        assertNotNull(deserialized)
        assertEquals(original.id, deserialized?.id)
        assertEquals(original.overallScore, deserialized?.overallScore ?: 0f, 0.01f)
        assertEquals(original.maturityLevel, deserialized?.maturityLevel)
        assertEquals(original.primaryWeakness, deserialized?.primaryWeakness)
        assertEquals(original.primaryStrength, deserialized?.primaryStrength)
        assertEquals(original.totalStrokesAnalyzed, deserialized?.totalStrokesAnalyzed)
        assertEquals(1, deserialized?.dimensions?.size)
        assertEquals(88.5f, deserialized?.dimensions?.get(BiomechanicalDimension.SLANT_STABILITY)?.score ?: 0f, 0.01f)
    }

    @Test
    fun `serialize and deserialize prescription roundtrip`() {
        val original = PrescribedPracticeSession(
            id = "presc_test_456",
            timestampMs = 1726000000000L,
            title = "Treino de Alinhamento",
            rationale = "Razão pedagógica clara.",
            targetDimension = BiomechanicalDimension.SLANT_STABILITY,
            recommendedMinutes = 10,
            warmupExerciseId = "basic_slant",
            focusExerciseId = "t",
            recommendedGhostLevel = 0.70f,
            targetGoalDescription = "Meta de paralelismo estrito.",
            isCompleted = false
        )

        val json = TeacherSerializer.serializePrescription(original)
        val deserialized = TeacherSerializer.deserializePrescription(json)

        assertNotNull(deserialized)
        assertEquals(original.id, deserialized?.id)
        assertEquals(original.title, deserialized?.title)
        assertEquals(original.rationale, deserialized?.rationale)
        assertEquals(original.targetDimension, deserialized?.targetDimension)
        assertEquals(original.recommendedMinutes, deserialized?.recommendedMinutes)
        assertEquals(original.recommendedGhostLevel, deserialized?.recommendedGhostLevel ?: 0f, 0.01f)
        assertEquals(original.isCompleted, deserialized?.isCompleted)
    }

    @Test
    fun `serialize and deserialize insights roundtrip`() {
        val insights = listOf(
            TeacherInsight(
                id = "ins_1",
                type = InsightType.PRAISE,
                title = "Excelente ritmo",
                message = "Seu traçado flui com naturalidade.",
                relatedDimension = BiomechanicalDimension.RHYTHM_AND_CADENCE,
                metricDelta = "+15%"
            ),
            TeacherInsight(
                id = "ins_2",
                type = InsightType.ERGONOMIC_TIP,
                title = "Apoio de palma",
                message = "Descanse a mão na tela.",
                relatedDimension = null,
                metricDelta = null
            )
        )

        val json = TeacherSerializer.serializeInsights(insights)
        val deserialized = TeacherSerializer.deserializeInsights(json)

        assertEquals(2, deserialized.size)
        assertEquals(InsightType.PRAISE, deserialized[0].type)
        assertEquals("Excelente ritmo", deserialized[0].title)
        assertEquals("+15%", deserialized[0].metricDelta)
        assertEquals(InsightType.ERGONOMIC_TIP, deserialized[1].type)
    }
}
