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
        updatedRepetition: SpacedRepetitionItem? = null
    ) {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            if (cachedSessions.none { it.sessionId == session.sessionId }) {
                cachedSessions.add(0, session) // Mais recente primeiro
            }
            if (updatedRepetition != null) {
                cachedRepetitionItems[updatedRepetition.targetId] = updatedRepetition
            }
            saveToDisk()
        }
    }

    /**
     * Retorna a lista de itens de repetição espaçada registrados.
     */
    fun getRepetitionItems(): List<SpacedRepetitionItem> {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            return cachedRepetitionItems.values.toList()
        }
    }

    /**
     * Retorna o resumo consolidado de progresso caligráfico.
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
                recentSessions = cachedSessions.take(20)
            )
        }
    }

    private fun checkAndReloadIfModifiedExternally() {
        if (historyFile.exists() && historyFile.lastModified() > lastKnownModified) {
            loadFromDisk()
        }
    }

    private fun loadFromDisk() {
        if (!historyFile.exists()) return

        try {
            lastKnownModified = historyFile.lastModified()
            val jsonText = historyFile.readText(StandardCharsets.UTF_8)
            val (sessions, repetitions) = LearningHistorySerializer.deserialize(jsonText)
            cachedSessions.clear()
            cachedSessions.addAll(sessions)
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
        } catch (e: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }
}
