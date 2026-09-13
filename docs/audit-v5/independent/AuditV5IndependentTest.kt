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
class AuditV5IndependentTest {
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
    @Test fun malformedJsonGrammarRejected() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"1.0\", GARBAGE}"))
    }
    @Test fun malformedManifestCannotOverwrite() = isolated { dir ->
        val active = File(dir, "teacher/diagnostic.json").apply { parentFile?.mkdirs(); writeText("ORIGINAL") }
        ScribeBackupManager(dir).importBackup(ByteArrayInputStream(pack(
            "manifest.json" to "{\"formatVersion\":\"1.0\", GARBAGE}",
            "teacher/diagnostic.json" to "REPLACED")))
        assertEquals("ORIGINAL", active.readText())
    }
    @Test fun backupIncludesPersistedSignature() = isolated { dir ->
        File(dir, "signatures/baseline.scribe").apply { parentFile?.mkdirs(); writeText("raw") }
        File(dir, "signatures/baseline_meta.txt").writeText("metadata")
        val out = ByteArrayOutputStream()
        ScribeBackupManager(dir).exportBackup(out)
        assertTrue(names(out.toByteArray()).contains("signatures/baseline.scribe"))
    }
    @Test fun sameSizeSameMtimeAttemptReplacementReloads() = isolated { dir ->
        val repo = LocalPracticeAttemptRepository(dir)
        repo.saveAttempt(attempt("a"))
        val manifest = File(dir, "attempts/attempts_manifest.txt")
        val mtime = manifest.lastModified()
        manifest.writeText(manifest.readText().replace("uncial", "italic"))
        check(manifest.setLastModified(mtime))
        assertEquals("italic", repo.getAllAttempts().single().styleId)
    }
    @Test fun historyReplacementDoesNotRetainOldVersion() = isolated { dir ->
        val repo = LocalLearningHistoryRepository(dir)
        repo.recordSession(session("a"))
        val history = File(dir, "learning_history.json")
        history.writeText(history.readText().replace("Lesson", "Updated lesson"))
        assertEquals("Updated lesson", repo.getProgressSummary().recentSessions.single().lessonTitle)
    }
    @Test fun reversedClosedPathCannotClaimIdenticalMuscularDynamics() {
        val a = listOf(StrokePoint(0f,0f,0), StrokePoint(100f,0f,250), StrokePoint(100f,100f,500), StrokePoint(0f,100f,750), StrokePoint(0f,0f,1000))
        val b = a.reversed().mapIndexed { i,p -> p.copy(tMs = i * 250L) }
        val report = SignatureConsistencyEngine.evaluateConsistency(
            SignatureConsistencyEngine.computeMetrics(listOf(stroke(a))),
            SignatureConsistencyEngine.computeMetrics(listOf(stroke(b))))
        assertFalse(report.feedbackDetails.contains("dinâmica muscular praticamente idênticos"))
    }
}
