package com.scribe.caligrafia.teacher.repository

import com.scribe.caligrafia.teacher.engine.CoachingCurriculumGenerator
import com.scribe.caligrafia.teacher.engine.CoachingFeedbackEngine
import com.scribe.caligrafia.teacher.engine.MotorDiagnosticEngine
import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Implementação local do repositório do Professor IA com salvamento atômico seguro (SCR-704).
 *
 * Utiliza o protocolo de contenção .tmp + fos.fd.sync() + ATOMIC_MOVE.
 */
class LocalTeacherRepository(
    private val baseDir: File,
    private val diagnosticEngine: MotorDiagnosticEngine = MotorDiagnosticEngine(),
    private val curriculumGenerator: CoachingCurriculumGenerator = CoachingCurriculumGenerator(),
    private val feedbackEngine: CoachingFeedbackEngine = CoachingFeedbackEngine()
) : TeacherRepository {

    private val teacherDir = File(baseDir, "teacher").apply { if (!exists()) mkdirs() }
    private val diagnosticFile = File(teacherDir, "diagnostic.json")
    private val prescriptionFile = File(teacherDir, "prescription.json")
    private val insightsFile = File(teacherDir, "insights.json")

    override suspend fun getLatestDiagnostic(): BiomechanicalDiagnostic = withContext(Dispatchers.IO) {
        if (!diagnosticFile.exists()) {
            val initial = diagnosticEngine.diagnoseAttempts(emptyList())
            saveDiagnostic(initial)
            return@withContext initial
        }

        try {
            val json = diagnosticFile.readText()
            TeacherSerializer.deserializeDiagnostic(json) ?: run {
                val fallback = diagnosticEngine.diagnoseAttempts(emptyList())
                saveDiagnostic(fallback)
                fallback
            }
        } catch (_: Exception) {
            diagnosticEngine.diagnoseAttempts(emptyList())
        }
    }

    override suspend fun saveDiagnostic(diagnostic: BiomechanicalDiagnostic) = withContext(Dispatchers.IO) {
        val json = TeacherSerializer.serializeDiagnostic(diagnostic)
        writeAtomic(diagnosticFile, json)
    }

    override suspend fun getLatestPrescription(): PrescribedPracticeSession = withContext(Dispatchers.IO) {
        if (!prescriptionFile.exists()) {
            val diag = getLatestDiagnostic()
            val initial = curriculumGenerator.generatePrescription(diag)
            savePrescription(initial)
            return@withContext initial
        }

        try {
            val json = prescriptionFile.readText()
            TeacherSerializer.deserializePrescription(json) ?: run {
                val diag = getLatestDiagnostic()
                val fallback = curriculumGenerator.generatePrescription(diag)
                savePrescription(fallback)
                fallback
            }
        } catch (_: Exception) {
            val diag = getLatestDiagnostic()
            curriculumGenerator.generatePrescription(diag)
        }
    }

    override suspend fun savePrescription(prescription: PrescribedPracticeSession) = withContext(Dispatchers.IO) {
        val json = TeacherSerializer.serializePrescription(prescription)
        writeAtomic(prescriptionFile, json)
    }

    override suspend fun getRecentInsights(): List<TeacherInsight> = withContext(Dispatchers.IO) {
        if (!insightsFile.exists()) {
            val diag = getLatestDiagnostic()
            val initial = feedbackEngine.generateInsights(diag)
            saveInsights(initial)
            return@withContext initial
        }

        try {
            val json = insightsFile.readText()
            val list = TeacherSerializer.deserializeInsights(json)
            if (list.isNotEmpty()) list
            else {
                val diag = getLatestDiagnostic()
                val fallback = feedbackEngine.generateInsights(diag)
                saveInsights(fallback)
                fallback
            }
        } catch (_: Exception) {
            val diag = getLatestDiagnostic()
            feedbackEngine.generateInsights(diag)
        }
    }

    override suspend fun saveInsights(insights: List<TeacherInsight>) = withContext(Dispatchers.IO) {
        val json = TeacherSerializer.serializeInsights(insights)
        writeAtomic(insightsFile, json)
    }

    override suspend fun markPrescriptionCompleted(prescriptionId: String) = withContext(Dispatchers.IO) {
        val current = getLatestPrescription()
        if (current.id == prescriptionId) {
            val updated = current.copy(isCompleted = true)
            savePrescription(updated)
        }
    }

    private fun writeAtomic(targetFile: File, content: String) {
        val tmpFile = File(teacherDir, "${targetFile.name}.tmp")
        try {
            FileOutputStream(tmpFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            try {
                Files.move(
                    tmpFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: IOException) {
                // Fallback para sistemas de arquivos sem suporte a ATOMIC_MOVE
                Files.move(
                    tmpFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: Exception) {
            if (tmpFile.exists()) tmpFile.delete()
            throw e
        }
    }
}
