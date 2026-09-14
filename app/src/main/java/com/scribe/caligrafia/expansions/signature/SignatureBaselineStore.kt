package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Persistência transacional da referência de assinatura.
 *
 * A referência é gravada em um diretório versionado contendo raw strokes + metadados. Somente
 * depois de ambos os arquivos estarem íntegros o ponteiro `baseline_current.txt` é substituído.
 * Assim, uma falha intermediária nunca transforma um conjunto parcial na referência ativa.
 */
class SignatureBaselineStore(private val signaturesDir: File) {

    private val versionsDir = File(signaturesDir, "baseline_versions")
    private val persistence = DedicatedFileStrategy(signaturesDir)
    private val pointerFile = File(signaturesDir, "baseline_current.txt")

    init {
        signaturesDir.mkdirs()
        versionsDir.mkdirs()
    }

    fun save(attempt: SignatureAttempt): SignatureAttempt {
        require(attempt.strokes.isNotEmpty()) { "Referência sem traços" }

        val stagingDir = File(versionsDir, ".staging_${attempt.id}_${System.nanoTime()}")
        val finalDir = File(versionsDir, attempt.id)
        require(stagingDir.mkdirs()) { "Não foi possível preparar a gravação da referência" }

        try {
            val rawFile = File(stagingDir, "baseline.scribe")
            val metadataFile = File(stagingDir, "metadata.txt")

            persistence.save(rawFile, attempt.strokes, attempt.id)
            writeMetadata(metadataFile, attempt)

            // Leitura de verificação antes de publicar o ponteiro.
            val verifiedStrokes = persistence.load(rawFile)
            require(verifiedStrokes.size == attempt.strokes.size) {
                "Falha de integridade ao verificar os traços da referência"
            }
            val verifiedAttempt = readMetadata(metadataFile, verifiedStrokes)
            require(verifiedAttempt.id == attempt.id) { "Metadados da referência não correspondem aos traços" }

            if (finalDir.exists()) finalDir.deleteRecursively()
            moveDirectory(stagingDir, finalDir)
            writePointerAtomically(attempt.id)
            return verifiedAttempt
        } catch (t: Throwable) {
            stagingDir.deleteRecursively()
            throw t
        }
    }

    fun load(): SignatureAttempt? {
        val currentId = pointerFile.takeIf { it.exists() }
            ?.readText(Charsets.UTF_8)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (currentId != null) {
            val versionDir = File(versionsDir, currentId)
            val rawFile = File(versionDir, "baseline.scribe")
            val metadataFile = File(versionDir, "metadata.txt")
            if (rawFile.exists() && metadataFile.exists()) {
                val strokes = persistence.load(rawFile)
                return readMetadata(metadataFile, strokes)
            }
        }

        // Compatibilidade com instalações anteriores à F5.
        val legacyRaw = File(signaturesDir, "baseline.scribe")
        val legacyMeta = File(signaturesDir, "baseline_meta.txt")
        if (legacyRaw.exists() && legacyMeta.exists()) {
            val strokes = persistence.load(legacyRaw)
            return readLegacyMetadata(legacyMeta, strokes)
        }

        return null
    }

    private fun writeMetadata(file: File, attempt: SignatureAttempt) {
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(
                    listOf(
                        "v1",
                        attempt.id,
                        attempt.timestampMs.toString(),
                        attempt.durationMs.toString(),
                        attempt.minX.toString(),
                        attempt.minY.toString(),
                        attempt.maxX.toString(),
                        attempt.maxY.toString(),
                        attempt.baselineAngleDeg.toString()
                    ).joinToString("|")
                )
                writer.flush()
                fos.fd.sync()
            }
        }
    }

    private fun readMetadata(file: File, strokes: List<com.scribe.caligrafia.core.model.Stroke>): SignatureAttempt {
        val parts = file.readText(Charsets.UTF_8).trim().split('|')
        require(parts.size == 9 && parts[0] == "v1") { "Metadados de referência inválidos" }

        return SignatureAttempt(
            id = parts[1],
            timestampMs = parts[2].toLongOrNull() ?: error("timestamp inválido"),
            strokes = strokes,
            durationMs = parts[3].toLongOrNull() ?: error("duração inválida"),
            minX = parts[4].toFloatOrNull() ?: error("minX inválido"),
            minY = parts[5].toFloatOrNull() ?: error("minY inválido"),
            maxX = parts[6].toFloatOrNull() ?: error("maxX inválido"),
            maxY = parts[7].toFloatOrNull() ?: error("maxY inválido"),
            baselineAngleDeg = parts[8].toFloatOrNull() ?: error("ângulo inválido")
        )
    }

    private fun readLegacyMetadata(file: File, strokes: List<com.scribe.caligrafia.core.model.Stroke>): SignatureAttempt {
        val parts = file.readText(Charsets.UTF_8).trim().split('|')
        require(parts.size >= 6) { "Metadados legados de referência inválidos" }
        return SignatureAttempt(
            id = parts[0],
            strokes = strokes,
            durationMs = parts[1].toLongOrNull() ?: error("duração inválida"),
            minX = parts[2].toFloatOrNull() ?: error("minX inválido"),
            minY = parts[3].toFloatOrNull() ?: error("minY inválido"),
            maxX = parts[4].toFloatOrNull() ?: error("maxX inválido"),
            maxY = parts[5].toFloatOrNull() ?: error("maxY inválido")
        )
    }

    private fun writePointerAtomically(id: String) {
        val temp = File.createTempFile("baseline_current_", ".tmp", signaturesDir)
        try {
            FileOutputStream(temp).use { fos ->
                fos.write(id.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            try {
                Files.move(
                    temp.toPath(),
                    pointerFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(temp.toPath(), pointerFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (t: Throwable) {
            temp.delete()
            throw t
        }
    }

    private fun moveDirectory(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            if (!source.renameTo(target)) {
                source.copyRecursively(target, overwrite = true)
                source.deleteRecursively()
            }
        }
    }
}
