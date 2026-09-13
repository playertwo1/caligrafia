package com.scribe.caligrafia.expansions.backup

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

    @Test
    fun exportBackup_producesZipWithValidCentralDirectory_openableByZipFile() {
        // S01: Verifica que o arquivo ZIP final possui o diretório central válido (END header)
        val notebookDir = File(sourceBaseDir, "notebooks/caderno_1/pages").apply { mkdirs() }
        File(notebookDir, "page_1.scribe").writeText("PAGE_DATA_1")

        val backupFile = tempFolder.newFile("backup_test.scribepack")
        backupFile.outputStream().use { fos ->
            backupManager.exportBackup(fos)
        }

        // Deve abrir com java.util.zip.ZipFile sem lançar "zip END header not found"
        val zipFile = java.util.zip.ZipFile(backupFile)
        val entries = zipFile.entries().toList()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries.any { it.name == "manifest.json" })
        zipFile.close()
    }

    @Test
    fun exportAndImport_withRealRepositoryPaths_packsAndRestoresAllCanonicalEntities() {
        // S02: Testa com os caminhos canônicos reais dos repositórios
        // 1. Cadernos com páginas aninhadas
        val pagesDir = File(sourceBaseDir, "notebooks/caderno_real/pages").apply { mkdirs() }
        File(pagesDir, "page_real.scribe").writeText("CADERNO_REAL_PAGE")

        // 2. Alfabeto pessoal canônico
        val strokesDir = File(sourceBaseDir, "personal_alphabet/strokes").apply { mkdirs() }
        File(strokesDir, "glyph_b.scribe").writeText("GLIFO_CANONICO_B")
        File(sourceBaseDir, "personal_alphabet/personal_alphabet_manifest.json").writeText("{\"version\":1}")

        // 3. Tentativas de prática canônicas
        val attemptsDir = File(sourceBaseDir, "attempts").apply { mkdirs() }
        File(attemptsDir, "attempt_1.scribe").writeText("ATTEMPT_STROKES_1")

        // 4. Histórico de aprendizado na raiz
        File(sourceBaseDir, "learning_history.json").writeText("{\"totalSessions\": 5}")

        // 5. Diagnóstico do professor
        val teacherDir = File(sourceBaseDir, "teacher").apply { mkdirs() }
        File(teacherDir, "diagnostic.json").writeText("{\"posture\": \"GOOD\"}")

        // 6. Fontes customizadas
        val fontsDir = File(sourceBaseDir, "custom_fonts").apply { mkdirs() }
        File(fontsDir, "minha_fonte.ttf").writeText("FONT_BYTES")

        val baos = ByteArrayOutputStream()
        val summary = backupManager.exportBackup(baos)

        assertEquals(1, summary.manifest.notebookCount)
        assertEquals(1, summary.manifest.pageCount)
        assertEquals(1, summary.manifest.personalGlyphCount)
        assertEquals(1, summary.manifest.practiceAttemptCount)
        assertEquals(1, summary.manifest.lessonHistoryCount)
        assertTrue(summary.manifest.hasTeacherDiagnostic)

        // Restaura no destino
        val targetBackupManager = ScribeBackupManager(targetBaseDir)
        val importResult = targetBackupManager.importBackup(ByteArrayInputStream(baos.toByteArray()))

        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.restoredNotebooks)
        assertEquals(1, importResult.restoredGlyphs)
        assertEquals(1, importResult.restoredAttempts)
        assertEquals(1, importResult.restoredLessons)
        assertTrue(importResult.restoredTeacherData)

        // Verifica existência nos caminhos canônicos
        assertTrue(File(targetBaseDir, "notebooks/caderno_real/pages/page_real.scribe").exists())
        assertTrue(File(targetBaseDir, "personal_alphabet/strokes/glyph_b.scribe").exists())
        assertTrue(File(targetBaseDir, "attempts/attempt_1.scribe").exists())
        assertTrue(File(targetBaseDir, "learning_history.json").exists())
        assertTrue(File(targetBaseDir, "teacher/diagnostic.json").exists())
        assertTrue(File(targetBaseDir, "custom_fonts/minha_fonte.ttf").exists())
    }

    @Test
    fun importBackup_withZipSlipMaliciousPath_isSafelyRejected() {
        // S04: Testa que caminhos com '../' são rejeitados e não escapam da pasta
        val baos = ByteArrayOutputStream()
        val zos = java.util.zip.ZipOutputStream(baos)

        // Adiciona manifesto
        val manifest = BackupManifest()
        val manifestBytes = BackupSerializer.serializeManifest(manifest).toByteArray()
        zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
        zos.write(manifestBytes)
        zos.closeEntry()

        // Adiciona entrada maliciosa com path traversal
        val maliciousEntry = java.util.zip.ZipEntry("../malicious.txt")
        zos.putNextEntry(maliciousEntry)
        zos.write("MALICIOUS_DATA".toByteArray())
        zos.closeEntry()

        zos.finish()
        zos.flush()

        val targetBackupManager = ScribeBackupManager(targetBaseDir)
        val result = targetBackupManager.importBackup(ByteArrayInputStream(baos.toByteArray()))

        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Caminho de entrada inválido") == true ||
                   result.errorMessage?.contains("SecurityException") == true ||
                   result.errorMessage?.contains("Falha") == true)
    }

    @Test
    fun importBackup_atomicRollbackOnFailure_preservesOriginalData() {
        // S03: Configura dados originais ativos no destino
        val existingNotebookDir = File(targetBaseDir, "notebooks/caderno_original/pages").apply { mkdirs() }
        val originalPage = File(existingNotebookDir, "p1.scribe").apply { writeText("ORIGINAL_PAGE_CONTENT") }
        val originalLearning = File(targetBaseDir, "learning_history.json").apply { writeText("ORIGINAL_HISTORY") }

        // Cria arquivo com manifesto mas faz o targetBaseDir/notebooks ficar somente leitura ou injeta erro simulado
        val targetBackupManager = ScribeBackupManager(targetBaseDir)
        
        // Se passarmos um ZIP truncado ou se falhar durante a extração, os dados originais permanecem
        val invalidZipStream = ByteArrayInputStream("DADOS_TRUNCADOS".toByteArray())
        val result = targetBackupManager.importBackup(invalidZipStream)

        assertFalse(result.isSuccess)
        // Dados originais devem permanecer íntegros
        assertTrue(originalPage.exists())
        assertEquals("ORIGINAL_PAGE_CONTENT", originalPage.readText())
        assertTrue(originalLearning.exists())
        assertEquals("ORIGINAL_HISTORY", originalLearning.readText())
    }

    @Test
    fun backupSerializer_rejectsMissingValue_f08() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"1.0\",\"x\":}"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\":}"))
    }

    @Test
    fun backupSerializer_rejectsTrailingComma_f08() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"1.0\",}"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\":1,}"))
        assertNull(BackupSerializer.parseJsonObject("{\"arr\":[1, 2,]}"))
    }

    @Test
    fun backupSerializer_rejectsInvalidNumbers_f08() {
        assertNull(BackupSerializer.parseJsonObject("{\"n\": 12.}"))
        assertNull(BackupSerializer.parseJsonObject("{\"n\": 12e}"))
        assertNull(BackupSerializer.parseJsonObject("{\"n\": 12e+}"))
    }

    @Test
    fun backupSerializer_rejectsUnescapedControlChars_f08() {
        assertNull(BackupSerializer.parseJsonObject("{\"s\": \"bad\u0000char\"}"))
        assertNull(BackupSerializer.parseJsonObject("{\"s\": \"bad\u0007bell\"}"))
    }

    @Test
    fun backupSerializer_rejectsContentAfterTopLevelObject_f08() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"1.0\"} trailing content"))
        assertNull(BackupSerializer.parseJsonObject("{\"a\": 1} trailing"))
    }

    @Test
    fun backupSerializer_rejectsIncompatibleFormatVersion_f08() {
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"99.0\"}"))
        assertNull(BackupSerializer.deserializeManifest("{\"formatVersion\":\"2.0\"}"))
    }
}
