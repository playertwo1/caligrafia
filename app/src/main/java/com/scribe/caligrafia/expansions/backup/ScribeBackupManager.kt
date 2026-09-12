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
        "alphabet",
        "learning",
        "practice_attempts",
        "teacher",
        "fonts"
    )

    /**
     * Exporta todos os dados do Scribe para um OutputStream compactado em ZIP (.scribepack).
     */
    fun exportBackup(outputStream: OutputStream): BackupSummary {
        val zipOut = ZipOutputStream(outputStream.buffered())

        var totalFiles = 0
        var totalBytes = 0L

        var notebookCount = 0
        var pageCount = 0
        var glyphCount = 0
        var lessonCount = 0
        var attemptCount = 0
        var hasTeacher = false

        // 1. Contagem prévia de entidades para o manifesto
        val notebooksDir = File(baseDir, "notebooks")
        if (notebooksDir.exists()) {
            val nFiles = notebooksDir.listFiles() ?: emptyArray()
            notebookCount = nFiles.count { it.isDirectory }
            pageCount = nFiles.flatMap { it.listFiles()?.toList() ?: emptyList() }
                .count { it.extension == "scribe" }
        }

        val alphabetDir = File(baseDir, "alphabet")
        if (alphabetDir.exists()) {
            glyphCount = alphabetDir.listFiles()?.count { it.extension == "scribe" } ?: 0
        }

        val learningDir = File(baseDir, "learning")
        if (learningDir.exists()) {
            lessonCount = if (File(learningDir, "learning_history.json").exists()) 1 else 0
        }

        val attemptsDir = File(baseDir, "practice_attempts")
        if (attemptsDir.exists()) {
            attemptCount = attemptsDir.listFiles()?.count { it.extension == "scribe" } ?: 0
        }

        val teacherDir = File(baseDir, "teacher")
        if (teacherDir.exists()) {
            hasTeacher = File(teacherDir, "diagnostic_latest.json").exists()
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
                    if (file.isFile && !file.name.endsWith(".tmp")) {
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

        zipOut.flush()
        zipOut.finish()

        return BackupSummary(
            manifest = manifest,
            totalBytes = totalBytes,
            fileCount = totalFiles
        )
    }

    /**
     * Importa e restaura os dados a partir de um InputStream (.scribepack).
     * Aplica proteção estrita contra Zip-Slip e restauração atômica via diretório temporário.
     */
    fun importBackup(inputStream: InputStream): BackupImportResult {
        val tempRestoreDir = File(baseDir, "temp_restore_${System.currentTimeMillis()}")
        tempRestoreDir.mkdirs()

        var manifest: BackupManifest? = null

        try {
            val zipIn = ZipInputStream(inputStream.buffered())
            var entry: ZipEntry? = zipIn.nextEntry

            while (entry != null) {
                val entryName = entry.name

                // Proteção contra Zip Slip Vulnerability
                val destinationFile = File(tempRestoreDir, entryName).canonicalFile
                if (!destinationFile.path.startsWith(tempRestoreDir.canonicalPath)) {
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

            if (manifest == null) {
                tempRestoreDir.deleteRecursively()
                return BackupImportResult(
                    isSuccess = false,
                    errorMessage = "Manifesto do backup ausente ou corrompido."
                )
            }

            // 4. Move atomicamente os diretórios para a pasta base
            var restoredNotebooks = 0
            var restoredGlyphs = 0
            var restoredLessons = 0
            var restoredAttempts = 0
            var restoredTeacher = false

            for (dirName in supportedDirs) {
                val extractedDir = File(tempRestoreDir, dirName)
                if (extractedDir.exists() && extractedDir.isDirectory) {
                    val targetDir = File(baseDir, dirName)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }

                    extractedDir.walkTopDown().forEach { srcFile ->
                        if (srcFile.isFile) {
                            val rel = srcFile.relativeTo(extractedDir).path
                            val dest = File(targetDir, rel)
                            dest.parentFile?.mkdirs()
                            Files.copy(
                                srcFile.toPath(),
                                dest.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    }

                    when (dirName) {
                        "notebooks" -> restoredNotebooks = extractedDir.listFiles()?.count { it.isDirectory } ?: 0
                        "alphabet" -> restoredGlyphs = extractedDir.listFiles()?.count { it.extension == "scribe" } ?: 0
                        "learning" -> restoredLessons = if (File(extractedDir, "learning_history.json").exists()) 1 else 0
                        "practice_attempts" -> restoredAttempts = extractedDir.listFiles()?.count { it.extension == "scribe" } ?: 0
                        "teacher" -> restoredTeacher = File(extractedDir, "diagnostic_latest.json").exists()
                    }
                }
            }

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
        } catch (e: Throwable) {
            tempRestoreDir.deleteRecursively()
            return BackupImportResult(
                isSuccess = false,
                errorMessage = "Falha ao restaurar backup: ${e.message}"
            )
        }
    }
}
