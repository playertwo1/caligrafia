package com.scribe.caligrafia.expansions.backup

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.expansions.signature.SignatureAttempt
import com.scribe.caligrafia.expansions.signature.SignatureBaselineStore
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ScribeBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sourceBaseDir: File
    private lateinit var targetBaseDir: File
    private lateinit var backupManager: ScribeBackupManager

    @Before
    fun setup() {
        sourceBaseDir = tempFolder.newFolder("source_app_data")
        targetBaseDir = tempFolder.newFolder("target_app_data")
        backupManager = ScribeBackupManager(sourceBaseDir)
    }

    @Test
    fun backupSerializer_serializesAndDeserializesExpandedInventory() {
        val manifest = BackupManifest(
            formatVersion = "1.0",
            appVersion = "0.8.5",
            appVersionCode = 15,
            createdAtMs = 1700000000000L,
            deviceInfo = "Android",
            notebookCount = 3,
            pageCount = 12,
            personalGlyphCount = 26,
            lessonHistoryCount = 5,
            spacedRepetitionCount = 4,
            practiceAttemptCount = 18,
            passageCopyCount = 2,
            personalStyleCount = 3,
            importedFontCount = 1,
            signatureReferenceCount = 1,
            hasTeacherDiagnostic = true,
            hasPreferencesSnapshot = true
        )

        val json = BackupSerializer.serializeManifest(manifest)
        val restored = BackupSerializer.deserializeManifest(json)

        assertEquals(manifest, restored)
    }

    @Test
    fun exportInspectAndImport_mixedCanonicalSet_roundTripsAllManagedEntities() {
        createMixedCanonicalDataset(sourceBaseDir)

        val baos = ByteArrayOutputStream()
        val summary = backupManager.exportBackup(baos)

        assertEquals(1, summary.manifest.notebookCount)
        assertEquals(1, summary.manifest.pageCount)
        assertEquals(1, summary.manifest.personalGlyphCount)
        assertEquals(1, summary.manifest.lessonHistoryCount)
        assertEquals(1, summary.manifest.spacedRepetitionCount)
        assertEquals(1, summary.manifest.practiceAttemptCount)
        assertEquals(1, summary.manifest.passageCopyCount)
        assertEquals(1, summary.manifest.personalStyleCount)
        assertEquals(1, summary.manifest.importedFontCount)
        assertEquals(1, summary.manifest.signatureReferenceCount)
        assertTrue(summary.manifest.hasTeacherDiagnostic)
        assertTrue(summary.manifest.hasPreferencesSnapshot)

        val inspection = ScribeBackupManager(targetBaseDir).inspectBackup(ByteArrayInputStream(baos.toByteArray()))
        assertTrue(inspection.isValid)
        assertEquals(summary.manifest, inspection.manifest)
        assertTrue(inspection.entryCount > 1)
        assertTrue(inspection.totalUncompressedBytes > 0)

        // Acervo existente que NÃO existe no pacote. Restore F5 deve substituí-lo, não mesclar.
        val obsolete = File(targetBaseDir, "notebooks/obsolete/pages/obsolete.scribe")
        writeScribe(obsolete, sampleStroke("obsolete"))
        assertTrue(obsolete.exists())

        val result = ScribeBackupManager(targetBaseDir)
            .importBackup(ByteArrayInputStream(baos.toByteArray()))

        assertTrue(result.errorMessage, result.isSuccess)
        assertEquals(1, result.restoredNotebooks)
        assertEquals(1, result.restoredGlyphs)
        assertEquals(1, result.restoredLessons)
        assertEquals(1, result.restoredAttempts)
        assertEquals(1, result.restoredPassageCopies)
        assertEquals(1, result.restoredStyles)
        assertEquals(1, result.restoredFonts)
        assertEquals(1, result.restoredSignatureReferences)
        assertTrue(result.restoredTeacherData)

        assertFalse("item removido não pode sobreviver por merge", obsolete.exists())
        assertTrue(File(targetBaseDir, "notebooks/caderno_1/pages/page_1.scribe").isFile)
        assertTrue(File(targetBaseDir, "personal_alphabet/strokes/var_a_1.scribe").isFile)
        assertTrue(File(targetBaseDir, "attempts/attempt_1.scribe").isFile)
        assertTrue(File(targetBaseDir, "passage_copies/text_copies/copy_1_p0.scribe").isFile)
        assertTrue(File(targetBaseDir, "signatures/baseline_current.txt").isFile)
        assertTrue(File(targetBaseDir, "preferences_snapshot.txt").isFile)
    }

    @Test
    fun exportBackup_producesZipWithCentralDirectoryAndReopens() {
        val page = File(sourceBaseDir, "notebooks/caderno_1/pages/page_1.scribe")
        writeScribe(page, sampleStroke("page"))

        val backupFile = tempFolder.newFile("backup_test.scribepack")
        backupFile.outputStream().use { backupManager.exportBackup(it) }

        ZipFile(backupFile).use { zip ->
            val entries = zip.entries().toList()
            assertTrue(entries.isNotEmpty())
            assertTrue(entries.any { it.name == "manifest.json" })
            assertTrue(entries.any { it.name.endsWith("page_1.scribe") })
        }
    }

    @Test
    fun importInvalidPackage_rejectsBeforeMutation_andPreservesExistingBytes() {
        val original = File(targetBaseDir, "notebooks/original/pages/p1.scribe")
        writeScribe(original, sampleStroke("original"))
        val before = original.readBytes()

        val result = ScribeBackupManager(targetBaseDir)
            .importBackup(ByteArrayInputStream("NOT_A_ZIP".toByteArray()))

        assertFalse(result.isSuccess)
        assertTrue(original.isFile)
        assertTrue(before.contentEquals(original.readBytes()))
    }

    @Test
    fun inspectPackage_withInvalidScribeMagic_isRejected() {
        val manifest = BackupManifest(notebookCount = 1, pageCount = 1)
        val bytes = zipBytes(
            "manifest.json" to BackupSerializer.serializeManifest(manifest).toByteArray(),
            "notebooks/n1/pages/p1.scribe" to "FAKE_SCRIBE".toByteArray()
        )

        val inspection = ScribeBackupManager(targetBaseDir)
            .inspectBackup(ByteArrayInputStream(bytes))

        assertFalse(inspection.isValid)
        assertTrue(inspection.errorMessage?.contains("Magic bytes") == true || inspection.errorMessage?.contains("truncado") == true)
    }

    @Test
    fun importBackup_withZipSlipPath_isRejectedBeforeMutation() {
        val original = File(targetBaseDir, "notebooks/original/pages/p1.scribe")
        writeScribe(original, sampleStroke("original"))
        val before = original.readBytes()

        val bytes = zipBytes(
            "manifest.json" to BackupSerializer.serializeManifest(BackupManifest()).toByteArray(),
            "../malicious.txt" to "MALICIOUS".toByteArray()
        )

        val result = ScribeBackupManager(targetBaseDir)
            .importBackup(ByteArrayInputStream(bytes))

        assertFalse(result.isSuccess)
        assertTrue(before.contentEquals(original.readBytes()))
        assertFalse(File(targetBaseDir.parentFile, "malicious.txt").exists())
    }

    @Test
    fun interruptedRestore_markerAndRollback_areRecoveredOnNextManagerStart() {
        val original = File(targetBaseDir, "notebooks/original/pages/p1.scribe")
        writeScribe(original, sampleStroke("original"))

        val rollback = File(targetBaseDir, ".restore_rollback")
        val rollbackFile = File(rollback, "notebooks/original/pages/p1.scribe")
        rollbackFile.parentFile.mkdirs()
        original.copyTo(rollbackFile, overwrite = true)
        val expectedDigest = managedDigest(rollback)

        // Simula processo morto depois de alterar o conjunto ativo e antes do commit.
        File(targetBaseDir, "notebooks").deleteRecursively()
        val partial = File(targetBaseDir, "notebooks/partial/pages/partial.scribe")
        writeScribe(partial, sampleStroke("partial"))
        File(targetBaseDir, ".restore_in_progress").writeText("v1|$expectedDigest")

        ScribeBackupManager(targetBaseDir) // init executa recuperação persistente

        assertTrue(File(targetBaseDir, "notebooks/original/pages/p1.scribe").isFile)
        assertFalse(partial.exists())
        assertFalse(File(targetBaseDir, ".restore_in_progress").exists())
        assertFalse(rollback.exists())
        assertEquals(expectedDigest, managedDigest(targetBaseDir))
    }

    @Test
    fun backupSerializer_rejectsMalformedGrammarAndIncompatibleVersions() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"1.0\",\"x\":}"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\":}"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\":1,}"))
        assertNull(BackupSerializer.parseJsonObject("{\"arr\":[1,2,]}"))
        assertNull(BackupSerializer.parseJsonObject("{\"n\":12.}"))
        assertNull(BackupSerializer.parseJsonObject("{\"n\":12e}"))
        assertNull(BackupSerializer.parseJsonObject("{\"s\":\"bad\u0000char\"}"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\":1} trailing"))
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"99.0\"}"))
    }

    @Test
    fun old10Manifest_missingNewFields_remainsReadableWithSafeDefaults() {
        val old = """
            {
              "formatVersion":"1.0",
              "appVersion":"0.8.0",
              "appVersionCode":10,
              "createdAtMs":1,
              "deviceInfo":"Android",
              "notebookCount":0,
              "pageCount":0,
              "personalGlyphCount":0,
              "lessonHistoryCount":0,
              "practiceAttemptCount":0,
              "hasTeacherDiagnostic":false
            }
        """.trimIndent()

        val parsed = BackupSerializer.deserializeManifest(old)
        assertNotNull(parsed)
        assertEquals(0, parsed?.spacedRepetitionCount)
        assertEquals(0, parsed?.passageCopyCount)
        assertEquals(0, parsed?.signatureReferenceCount)
        assertFalse(parsed?.hasPreferencesSnapshot ?: true)
    }

    private fun createMixedCanonicalDataset(root: File) {
        writeScribe(
            File(root, "notebooks/caderno_1/pages/page_1.scribe"),
            sampleStroke("page_1")
        )

        File(root, "personal_alphabet").mkdirs()
        File(root, "personal_alphabet/personal_alphabet_manifest.json").writeText(
            """{"id":"alphabet","glyphs":[{"id":"a","variants":[{"id":"var_a_1"}]}]}"""
        )
        writeScribe(
            File(root, "personal_alphabet/strokes/var_a_1.scribe"),
            sampleStroke("glyph_a")
        )

        writeScribe(
            File(root, "attempts/attempt_1.scribe"),
            sampleStroke("attempt_1")
        )

        File(root, "learning_history.json").writeText(
            """
            {
              "sessions":[{
                "sessionId":"s1","lessonId":"lesson_1","lessonTitle":"Lição 1",
                "timestampMs":1000,"durationMinutes":5,"actualDurationSeconds":300,
                "attemptsCount":1,"averageScorePercent":80
              }],
              "spacedRepetition":[{
                "targetId":"a","repetitionCount":1,"lastScorePercent":80,
                "lastPracticedTimestampMs":1000,"intervalDays":1,"nextReviewTimestampMs":2000
              }]
            }
            """.trimIndent()
        )

        File(root, "teacher").mkdirs()
        File(root, "teacher/diagnostic.json").writeText("{\"status\":\"ok\"}")

        File(root, "custom_fonts").mkdirs()
        File(root, "custom_fonts/minha_fonte.ttf").writeBytes(byteArrayOf(0, 1, 2, 3))

        File(root, "passage_copies/text_copies").mkdirs()
        File(root, "passage_copies/text_copies/manifest.json").writeText(
            """{"records":[{"id":"copy_1","pageCount":1}]}"""
        )
        writeScribe(
            File(root, "passage_copies/text_copies/copy_1_p0.scribe"),
            sampleStroke("copy_1")
        )

        File(root, "personal_styles.json").writeText(
            """{"styles":[{"id":"style_1","name":"Meu estilo"}]}"""
        )

        File(root, "preferences_snapshot.txt").writeText(
            """
            pressure_curve=LINEAR
            is_left_handed=false
            is_high_contrast=false
            show_guide_numbers=true
            daily_goal_minutes=15
            """.trimIndent()
        )

        val signatureStore = SignatureBaselineStore(File(root, "signatures"))
        val stroke = sampleStroke("signature")
        signatureStore.save(
            SignatureAttempt(
                id = "baseline_1",
                timestampMs = 1000L,
                strokes = listOf(stroke),
                durationMs = 100L,
                minX = -10f,
                minY = 5f,
                maxX = 90f,
                maxY = 45f
            )
        )
    }

    private fun writeScribe(file: File, stroke: Stroke) {
        file.parentFile?.mkdirs()
        DedicatedFileStrategy(file.parentFile ?: file.parentFile!!)
            .save(file, listOf(stroke), file.nameWithoutExtension)
    }

    private fun sampleStroke(id: String): Stroke {
        return Stroke(
            id = id,
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(-10f, 5f, 1000L, pressure = 0f, tiltRad = 0f, orientationRad = 0f),
                StrokePoint(20f, 25f, 1050L, pressure = 0.5f, tiltRad = 0.1f, orientationRad = 0.2f),
                StrokePoint(90f, 45f, 1100L, pressure = 1f, tiltRad = 0f, orientationRad = 0f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1100L,
            isCancelled = false,
            color = 0xFF102030.toInt(),
            baseWidthPx = 4f
        )
    }

    private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun managedDigest(root: File): String {
        val managedNames = listOf(
            "notebooks", "personal_alphabet", "attempts", "teacher", "custom_fonts", "signatures", "passage_copies",
            "alphabet", "learning", "practice_attempts", "fonts",
            "learning_history.json", "active_session.json", "personal_styles.json", "preferences_snapshot.txt"
        )
        val digest = MessageDigest.getInstance("SHA-256")
        val files = mutableListOf<Pair<String, File>>()
        for (name in managedNames) {
            val item = File(root, name)
            if (!item.exists()) continue
            if (item.isFile) files += name to item
            else item.walkTopDown().filter { it.isFile }.forEach { file ->
                files += file.relativeTo(root).path.replace('\\', '/') to file
            }
        }
        files.sortedBy { it.first }.forEach { (path, file) ->
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(file.readBytes())
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
