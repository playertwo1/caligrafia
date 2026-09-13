package com.scribe.caligrafia.learning.history

import com.scribe.caligrafia.learning.review.SpacedRepetitionItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Repositório local de histórico de aprendizado com persistência atômica segura (SCR-404).
 */
class LocalLearningHistoryRepository(
    private val baseDir: File
) {
    private val historyFile = File(baseDir, "learning_history.json")
    private val lock = Any()

    private var cachedSessions = mutableListOf<CompletedSessionRecord>()
    private var cachedRepetitionItems = mutableMapOf<String, SpacedRepetitionItem>()
    private var lastKnownModified: Long = 0L
    private var lastKnownLength: Long = -1L
    private var lastKnownHash: Int = 0

    init {
        synchronized(lock) {
            loadFromDisk()
        }
    }

    /**
     * Registra a conclusão de uma sessão de treino e atualiza os itens de repetição espaçada.
     */
    fun recordSession(
        session: CompletedSessionRecord,
        updatedItem: SpacedRepetitionItem? = null
    ) {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            cachedSessions.removeAll { it.sessionId == session.sessionId }
            cachedSessions.add(0, session)
            if (updatedItem != null) {
                cachedRepetitionItems[updatedItem.targetId] = updatedItem
            }
            saveToDisk()
        }
    }

    /**
     * Retorna os itens de repetição espaçada que estão pendentes de revisão.
     */
    fun getRepetitionItems(): List<SpacedRepetitionItem> {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            return cachedRepetitionItems.values.toList()
        }
    }

    /**
     * Retorna o resumo completo de progresso do usuário para alimentar a tela de Evolução.
     */
    fun getProgressSummary(): LearningProgressSummary {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            val totalSeconds = cachedSessions.sumOf { it.actualDurationSeconds }
            val totalMinutes = (totalSeconds / 60)
            val totalSessions = cachedSessions.size
            val uniqueLessons = cachedSessions.map { it.lessonId }.distinct().size

            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            val daysActive = cachedSessions
                .filter { it.timestampMs >= thirtyDaysAgo }
                .map { it.timestampMs / (24 * 60 * 60 * 1000L) }
                .distinct()
                .size

            return LearningProgressSummary(
                totalMinutesPracticed = totalMinutes,
                totalSessionsCompleted = totalSessions,
                uniqueLessonsPracticed = uniqueLessons,
                daysActiveLast30Days = daysActive,
                spacedRepetitionItems = cachedRepetitionItems.toMap(),
                recentSessions = cachedSessions.toList()
            )
        }
    }

    private fun checkAndReloadIfModifiedExternally() {
        if (!historyFile.exists()) return
        val currentMod = historyFile.lastModified()
        val currentLen = historyFile.length()
        val currentHash = try { historyFile.readBytes().contentHashCode() } catch (_: Throwable) { 0 }
        if (currentMod != lastKnownModified || currentLen != lastKnownLength || currentHash != lastKnownHash) {
            loadFromDisk()
        }
    }

    private fun loadFromDisk() {
        if (!historyFile.exists()) return

        try {
            lastKnownModified = historyFile.lastModified()
            lastKnownLength = historyFile.length()
            val bytes = try { historyFile.readBytes() } catch (_: Throwable) { ByteArray(0) }
            lastKnownHash = bytes.contentHashCode()
            val jsonText = bytes.toString(StandardCharsets.UTF_8)
            val (sessions, repetitions) = LearningHistorySerializer.deserialize(jsonText)
            val mergedSessions = (sessions + cachedSessions).distinctBy { it.sessionId }
                .sortedByDescending { it.timestampMs }
            cachedSessions.clear()
            cachedSessions.addAll(mergedSessions)
            cachedRepetitionItems.clear()
            repetitions.forEach { item ->
                cachedRepetitionItems[item.targetId] = item
            }
        } catch (_: Throwable) {
            // Em caso de corrupção de arquivo, mantém cache em memória limpo
        }
    }

    private fun saveToDisk() {
        val jsonString = LearningHistorySerializer.serialize(
            sessions = cachedSessions,
            repetitions = cachedRepetitionItems.values.toList()
        )

        // Gravação atômica defensiva (conforme A02 da auditoria)
        baseDir.mkdirs()
        val tempFile = File(baseDir, "learning_history.json.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)
                writer.write(jsonString)
                writer.flush()
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }
            Files.move(
                tempFile.toPath(),
                historyFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            lastKnownModified = historyFile.lastModified()
            lastKnownLength = historyFile.length()
            lastKnownHash = try { historyFile.readBytes().contentHashCode() } catch (_: Throwable) { 0 }
        } catch (e: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }
}
