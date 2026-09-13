package com.scribe.caligrafia.audit

import android.app.Application
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.engine.CalendarConsistencyHelper
import com.scribe.caligrafia.expansions.backup.*
import com.scribe.caligrafia.expansions.signature.*
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.learning.history.*
import com.scribe.caligrafia.teacher.engine.*
import com.scribe.caligrafia.teacher.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.util.zip.*

/** Independent V4 acceptance expectations. RED means the product violates the criterion.
 * Fixtures are isolated temporary directories; no real user data is touched.
 * Kept outside the permanent test tree to preserve the implementer's baseline.
 */
class AuditV4IndependentTest {
    private fun <T> isolated(block: (File) -> T): T {
        val dir = Files.createTempDirectory("scribe-independent-v4-").toFile()
        return try { block(dir) } finally { dir.deleteRecursively() }
    }
    private fun stroke(points: List<StrokePoint>) = Stroke(
        "v4", ToolType.STYLUS, points, points.first().tMs, points.last().tMs,
        color = 0xff123456.toInt(), baseWidthPx = 3f
    )
    private fun line() = stroke(listOf(StrokePoint(100f, 0f, 0), StrokePoint(0f, 100f, 1000)))
    private fun attempt(id: String) = PracticeAttemptRecord(id, "basic_slant", "Slant", 1000,
        listOf(line()), 90, 90f, 1000, targetSlantDegrees = 90f, styleId = "uncial")
    private fun pack(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip -> entries.forEach { (name, value) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray()); zip.closeEntry()
        } }
        return out.toByteArray()
    }
    private fun names(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { result.add(entry.name); zip.closeEntry(); entry = zip.nextEntry }
        }
        return result
    }
    private fun session(id: String) = CompletedSessionRecord(id, "l", "Lesson", 1000, 1, 60, 1, 90)

    @Test fun s04_rejectsNonJsonManifest() {
        assertNull("Non-JSON must not become a valid manifest", BackupSerializer.deserializeManifest("not JSON"))
    }
    @Test fun s04_rejectsUnsupportedManifestVersion() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"999.0\"}"))
    }
    @Test fun s04_invalidManifestCannotOverwriteActiveData() = isolated { dir ->
        val active = File(dir, "teacher/diagnostic.json").apply { parentFile.mkdirs(); writeText("ORIGINAL") }
        val result = ScribeBackupManager(dir).importBackup(ByteArrayInputStream(pack(
            "manifest.json" to "not JSON", "teacher/diagnostic.json" to "REPLACED")))
        assertEquals("Invalid manifest changed active bytes; success=${result.isSuccess}", "ORIGINAL", active.readText())
    }
    @Test fun s03_rollbackRemovesNewRootsAfterMidCopyFailure() = isolated { dir ->
        // A file where a directory is required forces failure AFTER copying notebooks.
        File(dir, "teacher").writeText("ORIGINAL_FILE")
        val result = ScribeBackupManager(dir).importBackup(ByteArrayInputStream(pack(
            "manifest.json" to BackupSerializer.serializeManifest(BackupManifest()),
            "notebooks/new/pages/p.scribe" to "NEW",
            "teacher/diagnostic.json" to "NEW_DIAGNOSTIC")))
        assertFalse("Fixture must reach a failed import", result.isSuccess)
        assertEquals("ORIGINAL_FILE", File(dir, "teacher").readText())
        assertFalse("Rollback left new notebook data behind: ${result.errorMessage}", File(dir, "notebooks").exists())
    }
    @Test fun s02_backupIncludesStyleEngineRootFile() = isolated { dir ->
        File(dir, "personal_styles.json").writeText("[]")
        val out = ByteArrayOutputStream()
        ScribeBackupManager(dir).exportBackup(out)
        assertTrue("StyleEngine reads this root file", names(out.toByteArray()).contains("personal_styles.json"))
    }
    @Test fun r04_newAlphabetContainsNoSyntheticVariants() = isolated { dir -> runBlocking {
        val repo = LocalPersonalAlphabetRepository(dir)
        assertEquals("New user's a must not already have scored writing", 0, repo.getGlyph("glyph_lower_a")!!.variants.size)
    } }
    @Test fun r03_existingReaderSeesNewAttempt() = isolated { dir ->
        val writer = LocalPracticeAttemptRepository(dir)
        val reader = LocalPracticeAttemptRepository(dir)
        writer.saveAttempt(attempt("new"))
        assertEquals("Teacher/Evolution instances must see guided writes", 1, reader.getAllAttempts().size)
    }
    @Test fun r17_attemptRoundTripPreservesStyleContext() = isolated { dir ->
        LocalPracticeAttemptRepository(dir).saveAttempt(attempt("new"))
        val restored = LocalPracticeAttemptRepository(dir).getAllAttempts().single()
        assertEquals("uncial", restored.styleId)
        assertEquals(90f, restored.targetSlantDegrees)
    }
    @Test fun r06_equalMtimeDoesNotLoseSession() = isolated { dir ->
        val a = LocalLearningHistoryRepository(dir)
        a.recordSession(session("seed"))
        val file = File(dir, "learning_history.json")
        val observedTime = file.lastModified()
        val b = LocalLearningHistoryRepository(dir)
        a.recordSession(session("a"))
        assertTrue("Fixture preserves same observed timestamp", file.setLastModified(observedTime))
        b.recordSession(session("b"))
        assertEquals("seed, a and b must survive", 3, LocalLearningHistoryRepository(dir).getProgressSummary().totalSessionsCompleted)
    }
    @Test fun r07_evolutionInputMustNotTruncateAtTwentySessions() = isolated { dir ->
        val repo = LocalLearningHistoryRepository(dir)
        repeat(21) { repo.recordSession(session("s$it")) }
        val summary = repo.getProgressSummary()
        // This is the exact input used by EvolutionViewModel.loadData.
        val evolution = CalendarConsistencyHelper.computeSummary(summary.recentSessions, 0, 0)
        assertEquals(summary.totalMinutesPracticed, evolution.totalMinutesPracticed)
    }
    @Test fun s08_emptyDiagnosticDoesNotClaimMasteryInPrescription() {
        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(emptyList())
        val prescription = CoachingCurriculumGenerator().generatePrescription(diagnostic)
        assertFalse(prescription.rationale.contains("maturidade caligráfica superior"))
    }
    @Test fun s09_denseAndSparseSlantHaveSameObservation() {
        val sparse = line()
        val dense = stroke((0..100).map { StrokePoint(100f-it, it.toFloat(), it*10L) })
        val engine = MotorDiagnosticEngine()
        val a = engine.diagnoseStrokes(listOf(sparse)).dimensions.getValue(BiomechanicalDimension.SLANT_STABILITY)
        val b = engine.diagnoseStrokes(listOf(dense)).dimensions.getValue(BiomechanicalDimension.SLANT_STABILITY)
        assertEquals("Same straight path and duration", a.observedValue, b.observedValue)
    }
    @Test fun r14_denseReferenceStillMeasuresSlant() {
        val glyph = ReferenceGlyphCatalog.BASIC_SLANT
        val band = GuidelineBand(0, 0f, 100f, 200f, 300f)
        val p = GeometricFeedbackEvaluator.mapToScreen(glyph.strokes.first().points.first(), band, 0f, 80f)
        val q = GeometricFeedbackEvaluator.mapToScreen(glyph.strokes.first().points.last(), band, 0f, 80f)
        val pts = (0..100).map { i -> val t = i/100f; StrokePoint(p.x+(q.x-p.x)*t, p.y+(q.y-p.y)*t, i*10L) }
        val eval = GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts)), glyph, band, 0f, 80f, SlantConfig(52f))
        assertNotNull("Dense straight downstroke must still be measured", eval.slant.measuredAngleDegrees)
    }
    @Test fun s16_oppositeDiagonalsDoNotClaimIdenticalMuscularDynamics() {
        val a = SignatureConsistencyEngine.computeMetrics(listOf(line()))
        val b = SignatureConsistencyEngine.computeMetrics(listOf(stroke(listOf(StrokePoint(0f,0f,0), StrokePoint(100f,100f,1000)))))
        val report = SignatureConsistencyEngine.evaluateConsistency(a,b)
        assertFalse("Score=${report.repeatabilityScore}", report.feedbackDetails.contains("dinâmica muscular praticamente idênticos"))
    }
    @Test fun s15_svgFramesNegativeCoordinates() {
        val svg = SignatureExporter.exportToSvg(listOf(stroke(listOf(StrokePoint(-100f,-100f,0),StrokePoint(-10f,-10f,1000)))),1080,500)
        assertFalse("Fixed positive viewport with unchanged negative coordinates clips whole signature",
            svg.contains("viewBox=\"0 0 1080 500\"") && svg.contains("M -100.00 -100.00"))
    }
    @Test fun s24_manifestEscapesRoundTrip() {
        val value = "Device \"quoted\" \\ path\nUnicode ç"
        val decoded = BackupSerializer.deserializeManifest(BackupSerializer.serializeManifest(BackupManifest(deviceInfo=value)))!!
        assertEquals(value, decoded.deviceInfo)
    }
}
