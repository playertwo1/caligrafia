package com.scribe.caligrafia.expansions.backup

import com.scribe.caligrafia.learning.history.LearningHistorySerializer
import com.scribe.caligrafia.preferences.ScribePreferencesStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Gerenciador local-first de pacotes Scribe (.scribepack).
 *
 * F5: exporta inventário real, valida pacote antes do commit, restaura como SUBSTITUIÇÃO do conjunto
 * gerenciado (nunca merge silencioso) e mantém rollback persistente para falha/interrupção.
 */
class ScribeBackupManager(private val baseDir: File) {

    private val canonicalDirs = listOf(
        "notebooks",
        "personal_alphabet",
        "attempts",
        "teacher",
        "custom_fonts",
        "signatures",
        "passage_copies"
    )

    private val legacyDirs = listOf(
        "alphabet",
        "learning",
        "practice_attempts",
        "fonts"
    )

    private val rootFiles = listOf(
        "learning_history.json",
        "active_session.json",
        "personal_styles.json",
        "preferences_snapshot.txt"
    )

    private val managedNames = canonicalDirs + legacyDirs + rootFiles
    private val restoreMarker = File(baseDir, ".restore_in_progress")
    private val stableRollbackDir = File(baseDir, ".restore_rollback")

    init {
        baseDir.mkdirs()
        recoverInterruptedRestoreIfNeeded()
    }

    fun exportBackup(outputStream: OutputStream): BackupSummary = synchronized(restoreLock) {
        val inventory = inventory(baseDir)
        val manifest = inventory.toManifest()
        val zipOut = ZipOutputStream(outputStream.buffered())
        var totalFiles = 0
        var totalBytes = 0L

        fun putEntry(name: String, bytes: ByteArray) {
            zipOut.putNextEntry(ZipEntry(name))
            zipOut.write(bytes)
            zipOut.closeEntry()
            totalFiles++
            totalBytes += bytes.size
        }

        val manifestBytes = BackupSerializer.serializeManifest(manifest).toByteArray(Charsets.UTF_8)
        putEntry("manifest.json", manifestBytes)

        val exportedDirs = mutableSetOf<String>()
        for (dirName in canonicalDirs) {
            val folder = File(baseDir, dirName)
            if (folder.isDirectory) {
                addDirectoryToZip(zipOut, folder) { bytes ->
                    totalFiles++
                    totalBytes += bytes
                }
                exportedDirs += dirName
            }
        }

        val legacyToCanonical = mapOf(
            "alphabet" to "personal_alphabet",
            "practice_attempts" to "attempts",
            "fonts" to "custom_fonts",
            "learning" to "learning"
        )
        for (dirName in legacyDirs) {
            val canonical = legacyToCanonical[dirName]
            if (canonical != null && canonical in exportedDirs) continue
            val folder = File(baseDir, dirName)
            if (folder.isDirectory) {
                addDirectoryToZip(zipOut, folder) { bytes ->
                    totalFiles++
                    totalBytes += bytes
                }
            }
        }

        for (fileName in rootFiles) {
            val file = File(baseDir, fileName)
            if (file.isFile) {
                zipOut.putNextEntry(ZipEntry(fileName))
                val bytes = file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
                totalFiles++
                totalBytes += bytes
            }
        }

        zipOut.finish()
        zipOut.flush()

        BackupSummary(
            manifest = manifest,
            totalBytes = totalBytes,
            fileCount = totalFiles
        )
    }

    /** Somente valida; não altera nenhum arquivo ativo. */
    fun inspectBackup(inputStream: InputStream): BackupInspectionResult {
        return try {
            val validation = validateZip(inputStream, extractTo = null)
            BackupInspectionResult(
                isValid = true,
                manifest = validation.manifest,
                entryCount = validation.entryCount,
                totalUncompressedBytes = validation.totalBytes
            )
        } catch (e: Throwable) {
            BackupInspectionResult(isValid = false, errorMessage = e.message ?: "Pacote inválido")
        }
    }

    fun importBackup(inputStream: InputStream): BackupImportResult = synchronized(restoreLock) {
        recoverInterruptedRestoreIfNeeded()

        val stagingDir = File(baseDir, ".restore_staging_${System.currentTimeMillis()}")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        val validation = try {
            validateZip(inputStream, extractTo = stagingDir)
        } catch (e: Throwable) {
            stagingDir.deleteRecursively()
            return@synchronized BackupImportResult(
                isSuccess = false,
                errorMessage = "Pacote rejeitado antes da restauração: ${e.message}"
            )
        }

        val previousDigest = computeManagedDigest(baseDir)
        if (stableRollbackDir.exists()) stableRollbackDir.deleteRecursively()
        stableRollbackDir.mkdirs()

        return@synchronized try {
            copyManagedSet(baseDir, stableRollbackDir)
            val rollbackDigest = computeManagedDigest(stableRollbackDir)
            check(rollbackDigest == previousDigest) { "Cópia de recuperação não corresponde ao acervo anterior" }
            writeRestoreMarker(previousDigest)

            deleteManagedSet(baseDir)
            copyStagedSetToActive(stagingDir)

            val restoredInventory = inventory(baseDir)
            validateRestoredInventory(validation.manifest, restoredInventory)

            stagingDir.deleteRecursively()
            stableRollbackDir.deleteRecursively()
            restoreMarker.delete()

            BackupImportResult(
                isSuccess = true,
                manifest = validation.manifest,
                restoredNotebooks = restoredInventory.notebookCount,
                restoredGlyphs = restoredInventory.personalGlyphCount,
                restoredLessons = restoredInventory.lessonHistoryCount,
                restoredAttempts = restoredInventory.practiceAttemptCount,
                restoredTeacherData = restoredInventory.hasTeacherDiagnostic,
                restoredPassageCopies = restoredInventory.passageCopyCount,
                restoredStyles = restoredInventory.personalStyleCount,
                restoredFonts = restoredInventory.importedFontCount,
                restoredSignatureReferences = restoredInventory.signatureReferenceCount
            )
        } catch (commitError: Throwable) {
            val recoveryVerified = restoreRollbackAndVerify(previousDigest)
            stagingDir.deleteRecursively()
            BackupImportResult(
                isSuccess = false,
                recoveryWasRequired = true,
                errorMessage = if (recoveryVerified) {
                    "Falha durante a restauração; o conjunto anterior foi recuperado e verificado: ${commitError.message}"
                } else {
                    "Falha durante a restauração e a recuperação não pôde ser verificada: ${commitError.message}"
                }
            )
        }
    }

    fun recoverInterruptedRestoreIfNeeded(): Boolean = synchronized(restoreLock) {
        if (!restoreMarker.exists()) return@synchronized false
        val expectedDigest = restoreMarker.readText(Charsets.UTF_8).trim().substringAfter('|', "")
        if (!stableRollbackDir.isDirectory || expectedDigest.isBlank()) return@synchronized false
        restoreRollbackAndVerify(expectedDigest)
    }

    private fun restoreRollbackAndVerify(expectedDigest: String): Boolean {
        return try {
            deleteManagedSet(baseDir)
            copyManagedSet(stableRollbackDir, baseDir)
            val restoredDigest = computeManagedDigest(baseDir)
            val verified = restoredDigest == expectedDigest
            if (verified) {
                stableRollbackDir.deleteRecursively()
                restoreMarker.delete()
            }
            verified
        } catch (_: Throwable) {
            false
        }
    }

    private data class ValidationResult(
        val manifest: BackupManifest,
        val entryCount: Int,
        val totalBytes: Long
    )

    private fun validateZip(inputStream: InputStream, extractTo: File?): ValidationResult {
        var manifest: BackupManifest? = null
        var entryCount = 0
        var totalBytes = 0L
        val seenNames = mutableSetOf<String>()

        ZipInputStream(inputStream.buffered()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Pacote excede o limite de entradas" }
                val entryName = entry.name
                require(isSafeAndAllowedEntry(entryName)) { "Caminho ou tipo de entrada não permitido: $entryName" }
                require(seenNames.add(entryName)) { "Entrada duplicada no pacote: $entryName" }

                if (entry.isDirectory) {
                    if (extractTo != null) safeDestination(extractTo, entryName).mkdirs()
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                    continue
                }

                val isScribe = entryName.endsWith(".scribe", ignoreCase = true)
                val captureWholePayload = entryName == "manifest.json" ||
                    entryName.endsWith(".json", ignoreCase = true) ||
                    entryName.endsWith("preferences_snapshot.txt")

                val memory = if (captureWholePayload) ByteArrayOutputStream() else null
                val scribePrefix = if (isScribe) ByteArrayOutputStream(SCRIBE_MAGIC.size) else null
                val destination = extractTo?.let { safeDestination(it, entryName) }
                destination?.parentFile?.mkdirs()
                val destinationOut = destination?.let { FileOutputStream(it) }

                var entryBytes = 0L
                try {
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = zipIn.read(buffer)
                        if (read < 0) break
                        entryBytes += read
                        totalBytes += read
                        require(entryBytes <= MAX_ENTRY_BYTES) { "Entrada excede o limite permitido: $entryName" }
                        require(totalBytes <= MAX_TOTAL_BYTES) { "Pacote excede o tamanho total permitido" }
                        destinationOut?.write(buffer, 0, read)

                        if (memory != null) {
                            require(memory.size() + read <= MAX_CAPTURE_BYTES) { "Payload de validação grande demais: $entryName" }
                            memory.write(buffer, 0, read)
                        }

                        if (scribePrefix != null && scribePrefix.size() < SCRIBE_MAGIC.size) {
                            val remaining = SCRIBE_MAGIC.size - scribePrefix.size()
                            scribePrefix.write(buffer, 0, minOf(read, remaining))
                        }
                    }
                    destinationOut?.flush()
                    try { destinationOut?.fd?.sync() } catch (_: Throwable) {}
                } finally {
                    destinationOut?.close()
                }

                val bytes = memory?.toByteArray()
                when {
                    entryName == "manifest.json" -> {
                        val text = bytes?.toString(Charsets.UTF_8) ?: error("Manifesto ilegível")
                        manifest = BackupSerializer.deserializeManifest(text)
                            ?: error("Manifesto ausente, corrompido ou versão incompatível")
                    }
                    isScribe -> {
                        val prefix = scribePrefix?.toByteArray() ?: ByteArray(0)
                        require(prefix.size == SCRIBE_MAGIC.size) { "Arquivo .scribe truncado: $entryName" }
                        require(prefix.contentEquals(SCRIBE_MAGIC)) { "Magic bytes inválidos em $entryName" }
                    }
                    entryName.endsWith(".json", ignoreCase = true) -> {
                        val text = bytes?.toString(Charsets.UTF_8) ?: error("JSON ilegível: $entryName")
                        require(BackupSerializer.parseJsonObject(text) != null) { "JSON inválido em $entryName" }
                    }
                    entryName.endsWith("preferences_snapshot.txt") -> validatePreferencesSnapshot(bytes ?: ByteArray(0))
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        val validatedManifest = manifest ?: error("manifest.json não encontrado")
        return ValidationResult(validatedManifest, entryCount, totalBytes)
    }

    private fun validatePreferencesSnapshot(bytes: ByteArray) {
        require(ScribePreferencesStore.parseBackupSnapshot(bytes.toString(Charsets.UTF_8)) != null) {
            "preferences_snapshot.txt inválido"
        }
    }

    private fun isSafeAndAllowedEntry(name: String): Boolean {
        if (name.isBlank() || name.startsWith('/') || name.startsWith('\\')) return false
        if ('\\' in name) return false
        val segments = name.split('/')
        if (segments.any { it == ".." || it.isBlank() }) return false
        if (name == "manifest.json") return true
        if (name in rootFiles) return true
        val root = segments.first()
        if (root !in canonicalDirs && root !in legacyDirs) return false
        val lower = name.lowercase()
        return lower.endsWith(".json") ||
            lower.endsWith(".scribe") ||
            lower.endsWith(".txt") ||
            lower.endsWith(".meta") ||
            lower.endsWith(".ttf") ||
            lower.endsWith(".otf") ||
            !name.substringAfterLast('/').contains('.')
    }

    private fun safeDestination(root: File, entryName: String): File {
        val destination = File(root, entryName).canonicalFile
        val canonicalRoot = root.canonicalFile
        val prefix = canonicalRoot.path + File.separator
        require(destination.path == canonicalRoot.path || destination.path.startsWith(prefix)) {
            "Zip Slip detectado: $entryName"
        }
        return destination
    }

    private fun copyStagedSetToActive(stagingDir: File) {
        for (dirName in canonicalDirs) {
            val src = File(stagingDir, dirName)
            if (src.isDirectory) copyDirectory(src, File(baseDir, dirName))
        }

        val legacyMappings = mapOf(
            "alphabet" to "personal_alphabet",
            "practice_attempts" to "attempts",
            "fonts" to "custom_fonts"
        )
        for ((legacy, canonical) in legacyMappings) {
            val src = File(stagingDir, legacy)
            if (src.isDirectory && !File(baseDir, canonical).exists()) {
                copyDirectory(src, File(baseDir, canonical))
            }
        }

        val legacyLearning = File(stagingDir, "learning/learning_history.json")
        val rootLearning = File(stagingDir, "learning_history.json")
        if (rootLearning.isFile) rootLearning.copyTo(File(baseDir, "learning_history.json"), overwrite = true)
        else if (legacyLearning.isFile) legacyLearning.copyTo(File(baseDir, "learning_history.json"), overwrite = true)

        for (fileName in rootFiles.filter { it != "learning_history.json" }) {
            val src = File(stagingDir, fileName)
            if (src.isFile) src.copyTo(File(baseDir, fileName), overwrite = true)
        }
    }

    private fun copyManagedSet(sourceRoot: File, targetRoot: File) {
        targetRoot.mkdirs()
        for (name in managedNames) {
            val src = File(sourceRoot, name)
            if (!src.exists()) continue
            val dst = File(targetRoot, name)
            if (src.isDirectory) copyDirectory(src, dst) else {
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
            }
        }
    }

    private fun deleteManagedSet(root: File) {
        for (name in managedNames) {
            val target = File(root, name)
            if (target.exists()) target.deleteRecursively()
        }
    }

    private fun copyDirectory(source: File, target: File) {
        if (!target.exists()) target.mkdirs()
        source.walkTopDown().forEach { file ->
            val rel = file.relativeTo(source).path
            val dst = if (rel.isEmpty()) target else File(target, rel)
            if (file.isDirectory) dst.mkdirs() else {
                dst.parentFile?.mkdirs()
                file.copyTo(dst, overwrite = true)
            }
        }
    }

    private fun addDirectoryToZip(zipOut: ZipOutputStream, folder: File, onFile: (Long) -> Unit) {
        folder.walkTopDown().forEach { file ->
            if (!file.isFile || file.name.endsWith(".tmp") || file.name.startsWith(".rollback")) return@forEach
            val relPath = file.relativeTo(baseDir).path.replace('\\', '/')
            zipOut.putNextEntry(ZipEntry(relPath))
            val bytes = file.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
            onFile(bytes)
        }
    }

    private data class Inventory(
        val notebookCount: Int,
        val pageCount: Int,
        val personalGlyphCount: Int,
        val lessonHistoryCount: Int,
        val spacedRepetitionCount: Int,
        val practiceAttemptCount: Int,
        val passageCopyCount: Int,
        val personalStyleCount: Int,
        val importedFontCount: Int,
        val signatureReferenceCount: Int,
        val hasTeacherDiagnostic: Boolean,
        val hasPreferencesSnapshot: Boolean
    ) {
        fun toManifest() = BackupManifest(
            notebookCount = notebookCount,
            pageCount = pageCount,
            personalGlyphCount = personalGlyphCount,
            lessonHistoryCount = lessonHistoryCount,
            spacedRepetitionCount = spacedRepetitionCount,
            practiceAttemptCount = practiceAttemptCount,
            passageCopyCount = passageCopyCount,
            personalStyleCount = personalStyleCount,
            importedFontCount = importedFontCount,
            signatureReferenceCount = signatureReferenceCount,
            hasTeacherDiagnostic = hasTeacherDiagnostic,
            hasPreferencesSnapshot = hasPreferencesSnapshot
        )
    }

    private fun inventory(root: File): Inventory {
        val notebooksRoot = File(root, "notebooks")
        val notebookCount = countArray(File(notebooksRoot, "manifest.json"), "notebooks")
            .takeIf { it >= 0 }
            ?: (notebooksRoot.listFiles()?.count { it.isDirectory } ?: 0)
        val pageCount = notebooksRoot.takeIf { it.isDirectory }
            ?.walkTopDown()?.count { it.isFile && it.extension.equals("scribe", true) } ?: 0

        val alphabetRoot = File(root, "personal_alphabet").takeIf { it.exists() } ?: File(root, "alphabet")
        val alphabetManifest = listOf(
            File(alphabetRoot, "personal_alphabet_manifest.json"),
            File(alphabetRoot, "manifest.json")
        ).firstOrNull { it.isFile }
        val glyphCount = if (alphabetManifest != null) {
            countArray(alphabetManifest, "glyphs").coerceAtLeast(0)
        } else {
            0
        }

        val learningFile = File(root, "learning_history.json").takeIf { it.isFile }
            ?: File(root, "learning/learning_history.json")
        var sessionCount = 0
        var srsCount = 0
        if (learningFile.isFile) {
            try {
                val pair = LearningHistorySerializer.deserialize(learningFile.readText(Charsets.UTF_8))
                sessionCount = pair.first.size
                srsCount = pair.second.size
            } catch (_: Throwable) {}
        }

        val attemptsRoot = File(root, "attempts").takeIf { it.exists() } ?: File(root, "practice_attempts")
        val attemptManifest = listOf(
            File(attemptsRoot, "manifest.json"),
            File(attemptsRoot, "attempts_manifest.json")
        ).firstOrNull { it.isFile }
        val attemptCount = (if (attemptManifest != null) {
            countArray(attemptManifest, "attempts").takeIf { it >= 0 }
        } else null)
            ?: attemptsRoot.takeIf { it.isDirectory }
                ?.walkTopDown()?.count { it.isFile && it.extension.equals("scribe", true) } ?: 0

        val copyManifest = File(root, "passage_copies/text_copies/manifest.json")
        val passageCopyCount = countArray(copyManifest, "records").coerceAtLeast(0)

        val stylesFile = File(root, "personal_styles.json")
        val personalStyleCount = if (stylesFile.isFile) {
            val text = stylesFile.readText(Charsets.UTF_8)
            val parsed = BackupSerializer.parseJsonObject(text)
            val styles = parsed?.get("styles") as? List<*>
            styles?.size ?: Regex("\\\"id\\\"\\s*:").findAll(text).count()
        } else 0

        val fontsRoot = File(root, "custom_fonts").takeIf { it.exists() } ?: File(root, "fonts")
        val fontCount = fontsRoot.takeIf { it.isDirectory }?.listFiles()?.count {
            it.isFile && (it.extension.equals("ttf", true) || it.extension.equals("otf", true))
        } ?: 0

        val signatures = File(root, "signatures")
        val signatureCount = when {
            File(signatures, "baseline_current.txt").isFile -> 1
            File(signatures, "baseline.scribe").isFile -> 1
            else -> 0
        }

        val teacher = File(root, "teacher")
        val hasTeacher = File(teacher, "diagnostic.json").isFile || File(teacher, "diagnostic_latest.json").isFile

        return Inventory(
            notebookCount = notebookCount,
            pageCount = pageCount,
            personalGlyphCount = glyphCount,
            lessonHistoryCount = sessionCount,
            spacedRepetitionCount = srsCount,
            practiceAttemptCount = attemptCount,
            passageCopyCount = passageCopyCount,
            personalStyleCount = personalStyleCount,
            importedFontCount = fontCount,
            signatureReferenceCount = signatureCount,
            hasTeacherDiagnostic = hasTeacher,
            hasPreferencesSnapshot = File(root, "preferences_snapshot.txt").isFile
        )
    }

    private fun countArray(file: File, key: String): Int {
        if (!file.isFile) return -1
        return try {
            val parsed = BackupSerializer.parseJsonObject(file.readText(Charsets.UTF_8)) ?: return -1
            (parsed[key] as? List<*>)?.size ?: -1
        } catch (_: Throwable) {
            -1
        }
    }

    private fun validateRestoredInventory(manifest: BackupManifest, actual: Inventory) {
        require(actual.notebookCount == manifest.notebookCount) { "Contagem de cadernos divergiu do manifesto" }
        require(actual.pageCount == manifest.pageCount) { "Contagem de páginas divergiu do manifesto" }
        require(actual.personalGlyphCount == manifest.personalGlyphCount) { "Contagem de glifos divergiu do manifesto" }
        require(actual.practiceAttemptCount == manifest.practiceAttemptCount) { "Contagem de tentativas divergiu do manifesto" }
        require(actual.lessonHistoryCount == manifest.lessonHistoryCount) { "Contagem de sessões divergiu do manifesto" }
        require(actual.spacedRepetitionCount == manifest.spacedRepetitionCount) { "Contagem SRS divergiu do manifesto" }
        require(actual.passageCopyCount == manifest.passageCopyCount) { "Contagem de cópias divergiu do manifesto" }
        require(actual.personalStyleCount == manifest.personalStyleCount) { "Contagem de estilos divergiu do manifesto" }
        require(actual.importedFontCount == manifest.importedFontCount) { "Contagem de fontes divergiu do manifesto" }
        require(actual.signatureReferenceCount == manifest.signatureReferenceCount) { "Contagem de referências divergiu do manifesto" }
        require(actual.hasTeacherDiagnostic == manifest.hasTeacherDiagnostic) { "Estado do diagnóstico do Professor divergiu do manifesto" }
        require(actual.hasPreferencesSnapshot == manifest.hasPreferencesSnapshot) { "Estado das preferências divergiu do manifesto" }
    }

    private fun writeRestoreMarker(previousDigest: String) {
        val temp = File.createTempFile("restore_marker_", ".tmp", baseDir)
        FileOutputStream(temp).use { fos ->
            fos.write("v1|$previousDigest".toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }
        if (restoreMarker.exists()) restoreMarker.delete()
        check(temp.renameTo(restoreMarker) || run {
            temp.copyTo(restoreMarker, overwrite = true)
            temp.delete()
            true
        }) { "Não foi possível registrar estado de restauração" }
    }

    private fun computeManagedDigest(root: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val files = mutableListOf<Pair<String, File>>()
        for (name in managedNames) {
            val item = File(root, name)
            if (!item.exists()) continue
            if (item.isFile) {
                files += name to item
            } else {
                item.walkTopDown().filter { it.isFile }.forEach { file ->
                    files += file.relativeTo(root).path.replace('\\', '/') to file
                }
            }
        }
        files.sortedBy { it.first }.forEach { (path, file) ->
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(0)
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val restoreLock = Any()
        private val SCRIBE_MAGIC = "SCRIBE01".toByteArray(Charsets.US_ASCII)
        private const val MAX_ENTRIES = 100_000
        private const val MAX_ENTRY_BYTES = 64L * 1024L * 1024L
        private const val MAX_TOTAL_BYTES = 512L * 1024L * 1024L
        private const val MAX_CAPTURE_BYTES = 16 * 1024 * 1024
    }
}
