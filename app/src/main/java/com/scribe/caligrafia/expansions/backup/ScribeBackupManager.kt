package com.scribe.caligrafia.expansions.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Gerenciador de exportação e restauração atômica de pacotes de dados Scribe (.scribepack).
 * 100% offline, local-first e sem qualquer dependência de nuvem.
 */
class ScribeBackupManager(private val baseDir: File) {

    private val supportedDirs = listOf(
        "notebooks",
        "personal_alphabet",
        "attempts",
        "teacher",
        "custom_fonts",
        // Legados mantidos para compatibilidade retroativa com pacotes antigos
        "alphabet",
        "learning",
        "practice_attempts",
        "fonts"
    )

    /**
     * Exporta todos os dados do Scribe para um OutputStream compactado em ZIP (.scribepack).
     * Garante o descarregamento de todos os buffers intermediários e a integridade do diretório central do ZIP (S01).
     */
    fun exportBackup(outputStream: OutputStream): BackupSummary {
        val bufferedOut = outputStream.buffered()
        val zipOut = ZipOutputStream(bufferedOut)

        var totalFiles = 0
        var totalBytes = 0L

        var notebookCount = 0
        var pageCount = 0
        var glyphCount = 0
        var lessonCount = 0
        var attemptCount = 0
        var hasTeacher = false

        // 1. Contagem prévia exata nos caminhos canônicos e legados (S02)
        val notebooksDir = File(baseDir, "notebooks")
        if (notebooksDir.exists()) {
            val nFiles = notebooksDir.listFiles() ?: emptyArray()
            notebookCount = nFiles.count { it.isDirectory }
            pageCount = notebooksDir.walkTopDown().count { it.isFile && it.extension == "scribe" }
        }

        val alphabetDir = File(baseDir, "personal_alphabet").takeIf { it.exists() } ?: File(baseDir, "alphabet")
        if (alphabetDir.exists()) {
            glyphCount = alphabetDir.walkTopDown().count { it.isFile && it.extension == "scribe" }
        }

        val learningHistoryFile = File(baseDir, "learning_history.json").takeIf { it.exists() }
            ?: File(baseDir, "learning/learning_history.json")
        if (learningHistoryFile.exists()) {
            lessonCount = 1
        }

        val attemptsDir = File(baseDir, "attempts").takeIf { it.exists() } ?: File(baseDir, "practice_attempts")
        if (attemptsDir.exists()) {
            attemptCount = attemptsDir.walkTopDown().count { it.isFile && it.extension == "scribe" }
        }

        val teacherDir = File(baseDir, "teacher")
        if (teacherDir.exists()) {
            hasTeacher = File(teacherDir, "diagnostic.json").exists() || File(teacherDir, "diagnostic_latest.json").exists()
        }

        val manifest = BackupManifest(
            formatVersion = "1.0",
            appVersion = "0.8.0",
            appVersionCode = 10,
            createdAtMs = System.currentTimeMillis(),
            deviceInfo = "Samsung Galaxy S25 Ultra",
            notebookCount = notebookCount,
            pageCount = pageCount,
            personalGlyphCount = glyphCount,
            lessonHistoryCount = lessonCount,
            practiceAttemptCount = attemptCount,
            hasTeacherDiagnostic = hasTeacher
        )

        // 2. Grava manifest.json como primeira entrada do ZIP
        val manifestJson = BackupSerializer.serializeManifest(manifest)
        val manifestBytes = manifestJson.toByteArray(Charsets.UTF_8)
        val manifestEntry = ZipEntry("manifest.json")
        zipOut.putNextEntry(manifestEntry)
        zipOut.write(manifestBytes)
        zipOut.closeEntry()
        totalFiles++
        totalBytes += manifestBytes.size

        // 3. Compacta os diretórios suportados
        for (dirName in supportedDirs) {
            val folder = File(baseDir, dirName)
            if (folder.exists() && folder.isDirectory) {
                folder.walkTopDown().forEach { file ->
                    if (file.isFile && !file.name.endsWith(".tmp") && !file.name.startsWith(".rollback")) {
                        val relPath = file.relativeTo(baseDir).path.replace('\\', '/')
                        val entry = ZipEntry(relPath)
                        zipOut.putNextEntry(entry)
                        val bytesWritten = file.inputStream().use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                        totalFiles++
                        totalBytes += bytesWritten
                    }
                }
            }
        }

        // 4. Compacta arquivos soltos na raiz (ex: learning_history.json)
        val rootLearningHistory = File(baseDir, "learning_history.json")
        if (rootLearningHistory.exists() && rootLearningHistory.isFile) {
            val entry = ZipEntry("learning_history.json")
            zipOut.putNextEntry(entry)
            val bytesWritten = rootLearningHistory.inputStream().use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeEntry()
            totalFiles++
            totalBytes += bytesWritten
        }

        // S01: Escreve o diretório central do ZIP e descarrega todos os buffers intermediários
        zipOut.finish()
        zipOut.flush()
        bufferedOut.flush()

        return BackupSummary(
            manifest = manifest,
            totalBytes = totalBytes,
            fileCount = totalFiles
        )
    }

    /**
     * Importa e restaura os dados a partir de um InputStream (.scribepack).
     * Aplica proteção estrita contra Zip-Slip (S04) e restauração transacional com rollback automático (S03).
     */
    fun importBackup(inputStream: InputStream): BackupImportResult {
        val tempRestoreDir = File(baseDir, "temp_restore_${System.currentTimeMillis()}")
        tempRestoreDir.mkdirs()

        var manifest: BackupManifest? = null

        try {
            val zipIn = ZipInputStream(inputStream.buffered())
            var entry: ZipEntry? = zipIn.nextEntry

            val canonicalBase = tempRestoreDir.canonicalPath
            val canonicalBaseWithSep = if (canonicalBase.endsWith(File.separator)) canonicalBase else canonicalBase + File.separator

            while (entry != null) {
                val entryName = entry.name

                // S04: Proteção rigorosa contra Zip Slip Vulnerability
                val destinationFile = File(tempRestoreDir, entryName).canonicalFile
                if (!destinationFile.canonicalPath.startsWith(canonicalBaseWithSep) && destinationFile.canonicalPath != canonicalBase) {
                    throw SecurityException("Caminho de entrada inválido no arquivo de backup: $entryName")
                }

                if (entry.isDirectory) {
                    destinationFile.mkdirs()
                } else {
                    destinationFile.parentFile?.mkdirs()
                    if (entryName == "manifest.json") {
                        val baos = ByteArrayOutputStream()
                        zipIn.copyTo(baos)
                        val json = baos.toString(Charsets.UTF_8.name())
                        manifest = BackupSerializer.deserializeManifest(json)
                        destinationFile.writeBytes(baos.toByteArray())
                    } else {
                        FileOutputStream(destinationFile).use { fos ->
                            zipIn.copyTo(fos)
                            fos.fd.sync()
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            // S05: Validação do manifesto
            if (manifest == null) {
                tempRestoreDir.deleteRecursively()
                return BackupImportResult(
                    isSuccess = false,
                    errorMessage = "Manifesto do backup ausente ou corrompido."
                )
            }

            // S03: Restauração Atômica e Transacional com Backup de Rollback
            val rollbackDir = File(baseDir, ".rollback_${System.currentTimeMillis()}")
            rollbackDir.mkdirs()

            // Criar cópia de segurança do estado ativo para rollback em caso de falha
            val activeFolders = listOf("notebooks", "personal_alphabet", "attempts", "teacher", "custom_fonts", "learning_history.json")
            for (item in activeFolders) {
                val src = File(baseDir, item)
                if (src.exists()) {
                    val dest = File(rollbackDir, item)
                    if (src.isDirectory) {
                        src.copyRecursively(dest, overwrite = true)
                    } else {
                        src.copyTo(dest, overwrite = true)
                    }
                }
            }

            try {
                // Copia com migração de legados para os caminhos canônicos
                fun copyDirSafely(srcDir: File, targetDir: File) {
                    if (!srcDir.exists()) return
                    if (!targetDir.exists()) targetDir.mkdirs()
                    srcDir.walkTopDown().forEach { srcFile ->
                        if (srcFile.isFile) {
                            val rel = srcFile.relativeTo(srcDir).path
                            val dest = File(targetDir, rel)
                            dest.parentFile?.mkdirs()
                            Files.copy(srcFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }

                // 1. Restaura todos os diretórios suportados preservando seus caminhos relativos
                for (dirName in supportedDirs) {
                    val extracted = File(tempRestoreDir, dirName)
                    if (extracted.exists() && extracted.isDirectory) {
                        copyDirSafely(extracted, File(baseDir, dirName))
                    }
                }

                // 2. Replicação de legados para os caminhos canônicos utilizados pelos repositórios
                val stAlphabet = File(tempRestoreDir, "alphabet")
                if (stAlphabet.exists()) {
                    copyDirSafely(stAlphabet, File(baseDir, "personal_alphabet"))
                }

                val stPractice = File(tempRestoreDir, "practice_attempts")
                if (stPractice.exists()) {
                    copyDirSafely(stPractice, File(baseDir, "attempts"))
                }

                val stFonts = File(tempRestoreDir, "fonts")
                if (stFonts.exists()) {
                    copyDirSafely(stFonts, File(baseDir, "custom_fonts"))
                }

                // 3. Histórico de aprendizado
                val stLearningRoot = File(tempRestoreDir, "learning_history.json")
                val stLearningSub = File(tempRestoreDir, "learning/learning_history.json")
                if (stLearningRoot.exists()) {
                    Files.copy(stLearningRoot.toPath(), File(baseDir, "learning_history.json").toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else if (stLearningSub.exists()) {
                    Files.copy(stLearningSub.toPath(), File(baseDir, "learning_history.json").toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                // Contagens reais pós-restauração
                val restoredNotebooks = File(baseDir, "notebooks").listFiles()?.count { it.isDirectory } ?: 0
                val restoredGlyphs = (File(baseDir, "personal_alphabet").takeIf { it.exists() } ?: File(baseDir, "alphabet"))
                    .walkTopDown().count { it.isFile && it.extension == "scribe" }
                val restoredLessons = if (File(baseDir, "learning_history.json").exists() || File(baseDir, "learning/learning_history.json").exists()) 1 else 0
                val restoredAttempts = (File(baseDir, "attempts").takeIf { it.exists() } ?: File(baseDir, "practice_attempts"))
                    .walkTopDown().count { it.isFile && it.extension == "scribe" }
                val restoredTeacher = File(baseDir, "teacher/diagnostic.json").exists() || File(baseDir, "teacher/diagnostic_latest.json").exists()

                // Sucesso: descarta rollback e staging
                rollbackDir.deleteRecursively()
                tempRestoreDir.deleteRecursively()

                return BackupImportResult(
                    isSuccess = true,
                    manifest = manifest,
                    restoredNotebooks = restoredNotebooks,
                    restoredGlyphs = restoredGlyphs,
                    restoredLessons = restoredLessons,
                    restoredAttempts = restoredAttempts,
                    restoredTeacherData = restoredTeacher
                )
            } catch (copyError: Throwable) {
                // S03: Rollback transacional automático em caso de erro na cópia
                for (item in activeFolders) {
                    val backupItem = File(rollbackDir, item)
                    val activeTarget = File(baseDir, item)
                    if (backupItem.exists()) {
                        if (backupItem.isDirectory) {
                            activeTarget.deleteRecursively()
                            backupItem.copyRecursively(activeTarget, overwrite = true)
                        } else {
                            backupItem.copyTo(activeTarget, overwrite = true)
                        }
                    }
                }
                rollbackDir.deleteRecursively()
                tempRestoreDir.deleteRecursively()
                return BackupImportResult(
                    isSuccess = false,
                    errorMessage = "Falha durante a restauração dos arquivos; dados anteriores preservados: ${copyError.message}"
                )
            }
        } catch (e: Throwable) {
            tempRestoreDir.deleteRecursively()
            return BackupImportResult(
                isSuccess = false,
                errorMessage = "Falha ao restaurar backup: ${e.message}"
            )
        }
    }
}
