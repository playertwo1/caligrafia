package com.scribe.caligrafia.expansions.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

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
    fun backupSerializer_serializesAndDeserializesCorrectly() {
        val manifest = BackupManifest(
            formatVersion = "1.0",
            appVersion = "0.8.0",
            appVersionCode = 10,
            createdAtMs = 1700000000000L,
            deviceInfo = "Samsung Galaxy S25 Ultra",
            notebookCount = 3,
            pageCount = 12,
            personalGlyphCount = 26,
            lessonHistoryCount = 5,
            practiceAttemptCount = 18,
            hasTeacherDiagnostic = true
        )

        val json = BackupSerializer.serializeManifest(manifest)
        val deserialized = BackupSerializer.deserializeManifest(json)

        assertNotNull(deserialized)
        assertEquals(manifest.formatVersion, deserialized?.formatVersion)
        assertEquals(manifest.appVersion, deserialized?.appVersion)
        assertEquals(manifest.appVersionCode, deserialized?.appVersionCode)
        assertEquals(manifest.notebookCount, deserialized?.notebookCount)
        assertEquals(manifest.pageCount, deserialized?.pageCount)
        assertEquals(manifest.personalGlyphCount, deserialized?.personalGlyphCount)
        assertEquals(manifest.practiceAttemptCount, deserialized?.practiceAttemptCount)
        assertEquals(manifest.hasTeacherDiagnostic, deserialized?.hasTeacherDiagnostic)
    }

    @Test
    fun exportAndImport_restoresAllFilesAndPreservesStructure() {
        // 1. Cria estrutura de dados simulada na pasta de origem
        val notebookDir = File(sourceBaseDir, "notebooks/caderno_1").apply { mkdirs() }
        File(notebookDir, "page_1.scribe").writeText("DADOS_VETORIAIS_PAGINA_1")
        File(notebookDir, "page_2.scribe").writeText("DADOS_VETORIAIS_PAGINA_2")

        val alphabetDir = File(sourceBaseDir, "alphabet").apply { mkdirs() }
        File(alphabetDir, "glyph_a.scribe").writeText("DADOS_GLIFO_A")

        val learningDir = File(sourceBaseDir, "learning").apply { mkdirs() }
        File(learningDir, "learning_history.json").writeText("{\"sessions\": 3}")

        val teacherDir = File(sourceBaseDir, "teacher").apply { mkdirs() }
        File(teacherDir, "diagnostic_latest.json").writeText("{\"maturity\": \"MASTER\"}")

        // 2. Exporta backup para stream em memória
        val baos = ByteArrayOutputStream()
        val summary = backupManager.exportBackup(baos)

        assertTrue(summary.fileCount >= 5) // manifest + 2 pages + 1 glyph + 1 history + 1 teacher
        assertTrue(summary.totalBytes > 0)
        assertEquals(1, summary.manifest.notebookCount)
        assertEquals(2, summary.manifest.pageCount)
        assertEquals(1, summary.manifest.personalGlyphCount)
        assertEquals(1, summary.manifest.lessonHistoryCount)
        assertTrue(summary.manifest.hasTeacherDiagnostic)

        // 3. Importa backup no diretório de destino
        val targetBackupManager = ScribeBackupManager(targetBaseDir)
        val bais = ByteArrayInputStream(baos.toByteArray())
        val importResult = targetBackupManager.importBackup(bais)

        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.restoredNotebooks)
        assertEquals(1, importResult.restoredGlyphs)
        assertEquals(1, importResult.restoredLessons)
        assertTrue(importResult.restoredTeacherData)

        // 4. Verifica integridade física dos arquivos restaurados
        val restoredPage1 = File(targetBaseDir, "notebooks/caderno_1/page_1.scribe")
        assertTrue(restoredPage1.exists())
        assertEquals("DADOS_VETORIAIS_PAGINA_1", restoredPage1.readText())

        val restoredGlyph = File(targetBaseDir, "alphabet/glyph_a.scribe")
        assertTrue(restoredGlyph.exists())
        assertEquals("DADOS_GLIFO_A", restoredGlyph.readText())
    }

    @Test
    fun importCorruptedZip_returnsFailureResult() {
        val corruptedBytes = "ARQUIVO_NAO_EH_UM_ZIP_VALIDO".toByteArray()
        val targetBackupManager = ScribeBackupManager(targetBaseDir)
        val importResult = targetBackupManager.importBackup(ByteArrayInputStream(corruptedBytes))

        assertFalse(importResult.isSuccess)
        assertNotNull(importResult.errorMessage)
    }
}
