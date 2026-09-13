package com.scribe.caligrafia.expansions.passage

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Implementação local do repositório de cópia de textos com gravação atômica segura (F4.14, F4.15).
 */
class LocalPassageCopyRepository(
    private val baseDir: File
) : PassageCopyRepository {

    private val copiesDir = File(baseDir, "text_copies").apply { if (!exists()) mkdirs() }
    private val manifestFile = File(copiesDir, "manifest.json")
    private val fileStrategy = DedicatedFileStrategy(copiesDir)
    private val mutex = Mutex()

    private val cachedMetadata = mutableListOf<CopyMeta>()
    private val strokeCache = mutableMapOf<String, Map<Int, List<Stroke>>>()

    private data class CopyMeta(
        val id: String,
        val textId: String,
        val title: String,
        val author: String,
        val textContent: String,
        val styleId: String,
        val timestampMs: Long,
        val durationMs: Long,
        val strokeCount: Int,
        val pageCount: Int,
        val isCompleted: Boolean,
        val actualWpm: Float,
        val targetWpm: Int
    )

    init {
        loadManifest()
    }

    override suspend fun saveRecord(record: PassageCopyRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // 1. Salva os traços de cada página em arquivos .scribe dedicados
            record.strokesByPage.forEach { (pageIndex, strokes) ->
                val scribeFile = File(copiesDir, "${record.id}_p$pageIndex.scribe")
                fileStrategy.save(scribeFile, strokes, "${record.id}_p$pageIndex")
            }

            strokeCache[record.id] = record.strokesByPage

            // 2. Atualiza metadados no manifesto
            val meta = CopyMeta(
                id = record.id,
                textId = record.textId,
                title = record.title,
                author = record.author,
                textContent = record.textContent,
                styleId = record.styleId,
                timestampMs = record.timestampMs,
                durationMs = record.durationMs,
                strokeCount = record.strokeCount,
                pageCount = record.pageCount,
                isCompleted = record.isCompleted,
                actualWpm = record.actualWpm,
                targetWpm = record.targetWpm
            )

            cachedMetadata.removeAll { it.id == meta.id }
            cachedMetadata.add(0, meta)
            saveManifest()
        }
    }

    override suspend fun getAllRecords(): List<PassageCopyRecord> = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadManifest()
            cachedMetadata.map { loadFullRecord(it) }
        }
    }

    override suspend fun getRecordById(id: String): PassageCopyRecord? = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadManifest()
            val meta = cachedMetadata.find { it.id == id } ?: return@withLock null
            loadFullRecord(meta)
        }
    }

    override suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val meta = cachedMetadata.find { it.id == id } ?: return@withLock
            // Remove arquivos .scribe de todas as páginas
            for (p in 0 until meta.pageCount) {
                val f = File(copiesDir, "${id}_p$p.scribe")
                if (f.exists()) f.delete()
            }
            strokeCache.remove(id)
            cachedMetadata.removeAll { it.id == id }
            saveManifest()
        }
    }

    private fun loadFullRecord(meta: CopyMeta): PassageCopyRecord {
        val strokesMap = strokeCache.getOrPut(meta.id) {
            val map = mutableMapOf<Int, List<Stroke>>()
            for (p in 0 until meta.pageCount) {
                val f = File(copiesDir, "${meta.id}_p$p.scribe")
                if (f.exists()) {
                    val pageStrokes = fileStrategy.load(f)
                    map[p] = pageStrokes
                } else {
                    map[p] = emptyList()
                }
            }
            map
        }

        return PassageCopyRecord(
            id = meta.id,
            textId = meta.textId,
            title = meta.title,
            author = meta.author,
            textContent = meta.textContent,
            styleId = meta.styleId,
            timestampMs = meta.timestampMs,
            durationMs = meta.durationMs,
            strokeCount = meta.strokeCount,
            pageCount = meta.pageCount,
            strokesByPage = strokesMap,
            isCompleted = meta.isCompleted,
            actualWpm = meta.actualWpm,
            targetWpm = meta.targetWpm
        )
    }

    private fun loadManifest() {
        if (!manifestFile.exists()) return
        try {
            val json = manifestFile.readText()
            cachedMetadata.clear()
            val block = extractArrayBlock(json, "records") ?: return
            val itemRegex = "\\{[^}]+\\}".toRegex()
            for (match in itemRegex.findAll(block)) {
                val itemStr = match.value
                val id = extractString(itemStr, "id") ?: continue
                val textId = extractString(itemStr, "textId") ?: ""
                val title = extractString(itemStr, "title") ?: ""
                val author = extractString(itemStr, "author") ?: ""
                val textContent = extractString(itemStr, "textContent") ?: ""
                val styleId = extractString(itemStr, "styleId") ?: "cursiva_escolar_br"
                val timestampMs = extractLong(itemStr, "timestampMs") ?: System.currentTimeMillis()
                val durationMs = extractLong(itemStr, "durationMs") ?: 0L
                val strokeCount = extractInt(itemStr, "strokeCount") ?: 0
                val pageCount = extractInt(itemStr, "pageCount") ?: 1
                val isCompleted = extractBoolean(itemStr, "isCompleted") ?: false
                val actualWpm = extractFloat(itemStr, "actualWpm") ?: 0f
                val targetWpm = extractInt(itemStr, "targetWpm") ?: 14

                cachedMetadata.add(
                    CopyMeta(
                        id = id,
                        textId = textId,
                        title = title,
                        author = author,
                        textContent = textContent,
                        styleId = styleId,
                        timestampMs = timestampMs,
                        durationMs = durationMs,
                        strokeCount = strokeCount,
                        pageCount = pageCount,
                        isCompleted = isCompleted,
                        actualWpm = actualWpm,
                        targetWpm = targetWpm
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
    }

    private fun saveManifest() {
        val tempFile = File.createTempFile("copies_manifest_", ".tmp", copiesDir)
        try {
            FileOutputStream(tempFile).use { fos ->
                val writer = OutputStreamWriter(fos, Charsets.UTF_8)
                writer.write("{\n  \"records\": [\n")
                for (i in cachedMetadata.indices) {
                    val m = cachedMetadata[i]
                    writer.write("    {\n")
                    writer.write("      \"id\": \"${m.id}\",\n")
                    writer.write("      \"textId\": \"${escapeJson(m.textId)}\",\n")
                    writer.write("      \"title\": \"${escapeJson(m.title)}\",\n")
                    writer.write("      \"author\": \"${escapeJson(m.author)}\",\n")
                    writer.write("      \"textContent\": \"${escapeJson(m.textContent)}\",\n")
                    writer.write("      \"styleId\": \"${escapeJson(m.styleId)}\",\n")
                    writer.write("      \"timestampMs\": ${m.timestampMs},\n")
                    writer.write("      \"durationMs\": ${m.durationMs},\n")
                    writer.write("      \"strokeCount\": ${m.strokeCount},\n")
                    writer.write("      \"pageCount\": ${m.pageCount},\n")
                    writer.write("      \"isCompleted\": ${m.isCompleted},\n")
                    writer.write("      \"actualWpm\": ${m.actualWpm},\n")
                    writer.write("      \"targetWpm\": ${m.targetWpm}\n")
                    writer.write("    }${if (i < cachedMetadata.size - 1) "," else ""}\n")
                }
                writer.write("  ]\n}")
                writer.flush()
                fos.fd.sync()
            }
            Files.move(tempFile.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractFloat(json: String, key: String): Float? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractArrayBlock(json: String, key: String): String? {
        val startIndex = json.indexOf("\"$key\"")
        if (startIndex == -1) return null
        val arrayStart = json.indexOf('[', startIndex)
        if (arrayStart == -1) return null
        var depth = 0
        for (i in arrayStart until json.length) {
            if (json[i] == '[') depth++
            else if (json[i] == ']') {
                depth--
                if (depth == 0) return json.substring(arrayStart, i + 1)
            }
        }
        return null
    }
}
