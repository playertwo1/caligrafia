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
class AuditV5RecheckTest {
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
    @Test fun missingJsonValueCannotOverwriteData() = isolated { dir ->
        val active = File(dir,"teacher/diagnostic.json").apply { parentFile?.mkdirs(); writeText("ORIGINAL") }
        ScribeBackupManager(dir).importBackup(ByteArrayInputStream(pack(
            "manifest.json" to "{\"formatVersion\":\"1.0\",\"x\":}", "teacher/diagnostic.json" to "REPLACED")))
        assertEquals("ORIGINAL",active.readText())
    }
    @Test fun replacedStrokeMustInvalidateReaderCache() = isolated { dir ->
        val reader = LocalPracticeAttemptRepository(dir)
        reader.saveAttempt(attempt("a"))
        reader.getAllAttempts()
        val replacement = attempt("a").copy(strokes = listOf(stroke(listOf(StrokePoint(0f,0f,0),StrokePoint(200f,200f,1000)))),styleId="changed")
        LocalPracticeAttemptRepository(dir).saveAttempt(replacement)
        assertEquals(200f,reader.getAllAttempts().single().strokes.single().points.last().x,0f)
    }
    @Test fun signatureFilesSurviveBackupRestore() = isolated { source -> isolated { target ->
        File(source,"signatures/baseline.scribe").apply { parentFile?.mkdirs(); writeText("RAW") }
        File(source,"signatures/baseline_meta.txt").writeText("META")
        val out=ByteArrayOutputStream()
        ScribeBackupManager(source).exportBackup(out)
        assertTrue(ScribeBackupManager(target).importBackup(ByteArrayInputStream(out.toByteArray())).isSuccess)
        assertEquals("RAW",File(target,"signatures/baseline.scribe").readText())
        assertEquals("META",File(target,"signatures/baseline_meta.txt").readText())
    } }
}
