package com.scribe.caligrafia.teacher.repository

import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight

/**
 * Interface do repositório local do Professor IA (SCR-704).
 */
interface TeacherRepository {
    suspend fun getLatestDiagnostic(): BiomechanicalDiagnostic
    suspend fun saveDiagnostic(diagnostic: BiomechanicalDiagnostic)
    suspend fun getLatestPrescription(): PrescribedPracticeSession
    suspend fun savePrescription(prescription: PrescribedPracticeSession)
    suspend fun getRecentInsights(): List<TeacherInsight>
    suspend fun saveInsights(insights: List<TeacherInsight>)
    suspend fun markPrescriptionCompleted(prescriptionId: String)
}
