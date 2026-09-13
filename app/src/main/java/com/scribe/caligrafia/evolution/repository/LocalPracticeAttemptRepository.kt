package com.scribe.caligrafia.evolution.repository

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.evolution.engine.AttemptComparator
import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Implementação local e atômica do repositório de tentativas para análise evolutiva (SCR-501 a SCR-504).
 */
class LocalPracticeAttemptRepository(
    private val baseDir: File,
    private val fileStrategy: DedicatedFileStrategy = DedicatedFileStrategy(File(baseDir, "attempts"))
) : PracticeAttemptRepository {

    private val attemptsDir = File(baseDir, "attempts")
    private val manifestFile = File(attemptsDir, "attempts_manifest.txt")
    private val lock = Any()

    private val cachedMetadata = mutableListOf<AttemptMeta>()
    private val strokeCache = mutableMapOf<String, List<Stroke>>()
    private var lastKnownModified: Long = 0L
    private var lastKnownLength: Long = -1L

    private fun checkAndReloadIfModifiedExternally() {
        if (!manifestFile.exists()) return
        val mod = manifestFile.lastModified()
        val len = manifestFile.length()
        if (mod != lastKnownModified || len != lastKnownLength) {
            loadManifest()
        }
    }

    private data class AttemptMeta(
        val attemptId: String,
        val targetId: String,
        val targetTitle: String,
        val timestampMs: Long,
        val scorePercent: Int,
        val averageSlantDegrees: Float,
        val durationMs: Long,
        val isBaseline: Boolean,
        val targetSlantDegrees: Float? = null,
        val styleId: String? = null
    )

    init {
        synchronized(lock) {
            if (!attemptsDir.exists()) attemptsDir.mkdirs()
            loadManifest()
        }
    }

    override fun saveAttempt(attempt: PracticeAttemptRecord) {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            val scribeFile = File(attemptsDir, "${attempt.attemptId}.scribe")
            fileStrategy.save(scribeFile, attempt.strokes, attempt.attemptId)

            strokeCache[attempt.attemptId] = attempt.strokes

            val meta = AttemptMeta(
                attemptId = attempt.attemptId,
                targetId = attempt.targetId,
                targetTitle = attempt.targetTitle,
                timestampMs = attempt.timestampMs,
                scorePercent = attempt.scorePercent,
                averageSlantDegrees = attempt.averageSlantDegrees,
                durationMs = attempt.durationMs,
                isBaseline = attempt.isBaseline,
                targetSlantDegrees = attempt.targetSlantDegrees,
                styleId = attempt.styleId
            )

            cachedMetadata.removeAll { it.attemptId == meta.attemptId }
            cachedMetadata.add(0, meta)
            saveManifest()
        }
    }

    override fun getAttemptsForTarget(targetId: String): List<PracticeAttemptRecord> {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            return cachedMetadata
                .filter { it.targetId == targetId }
                .map { loadFullAttempt(it) }
        }
    }

    override fun getAllAttempts(): List<PracticeAttemptRecord> {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            return cachedMetadata.map { loadFullAttempt(it) }
        }
    }

    override fun getBeforeAndAfter(targetId: String): Pair<PracticeAttemptRecord, PracticeAttemptRecord>? {
        synchronized(lock) {
            checkAndReloadIfModifiedExternally()
            val forTarget = cachedMetadata.filter { it.targetId == targetId }
            if (forTarget.size < 2) return null

            val baseline = forTarget.firstOrNull { it.isBaseline } ?: forTarget.minByOrNull { it.timestampMs }!!
            val latest = forTarget.filter { it.attemptId != baseline.attemptId }.maxByOrNull { it.timestampMs } ?: return null

            return Pair(loadFullAttempt(baseline), loadFullAttempt(latest))
        }
    }

    override fun getAllComparisons(): List<BeforeAfterComparison> {
        synchronized(lock) {
            val distinctTargets = cachedMetadata.map { it.targetId }.distinct()
            val comparisons = mutableListOf<BeforeAfterComparison>()

            for (targetId in distinctTargets) {
                val pair = getBeforeAndAfter(targetId)
                if (pair != null) {
                    comparisons.add(AttemptComparator.compare(pair.first, pair.second))
                }
            }
            return comparisons
        }
    }

    private fun loadFullAttempt(meta: AttemptMeta): PracticeAttemptRecord {
        val strokes = strokeCache.getOrPut(meta.attemptId) {
            val file = File(attemptsDir, "${meta.attemptId}.scribe")
            if (file.exists()) {
                try {
                    fileStrategy.load(file)
                } catch (_: Throwable) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        return PracticeAttemptRecord(
            attemptId = meta.attemptId,
            targetId = meta.targetId,
            targetTitle = meta.targetTitle,
            timestampMs = meta.timestampMs,
            strokes = strokes,
            scorePercent = meta.scorePercent,
            averageSlantDegrees = meta.averageSlantDegrees,
            durationMs = meta.durationMs,
            isBaseline = meta.isBaseline,
            targetSlantDegrees = meta.targetSlantDegrees,
            styleId = meta.styleId
        )
    }

    private fun loadManifest() {
        if (!manifestFile.exists()) {
            cachedMetadata.clear()
            lastKnownModified = 0L
            lastKnownLength = -1L
            return
        }

        lastKnownModified = manifestFile.lastModified()
        lastKnownLength = manifestFile.length()
        cachedMetadata.clear()
        try {
            val lines = manifestFile.readLines(StandardCharsets.UTF_8)
            for (line in lines) {
                val parts = line.split("|")
                if (parts.size >= 8) {
                    val targetSlant = parts.getOrNull(8)?.toFloatOrNull()
                    val styleId = parts.getOrNull(9)?.takeIf { it.isNotBlank() }
                    cachedMetadata.add(
                        AttemptMeta(
                            attemptId = parts[0],
                            targetId = parts[1],
                            targetTitle = parts[2],
                            timestampMs = parts[3].toLongOrNull() ?: 0L,
                            scorePercent = parts[4].toIntOrNull() ?: 0,
                            averageSlantDegrees = parts[5].toFloatOrNull() ?: 52f,
                            durationMs = parts[6].toLongOrNull() ?: 0L,
                            isBaseline = parts[7].toBoolean(),
                            targetSlantDegrees = targetSlant,
                            styleId = styleId
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    private fun saveManifest() {
        val temp = File(attemptsDir, "attempts_manifest.txt.tmp")
        try {
            FileOutputStream(temp).use { fos ->
                val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)
                for (meta in cachedMetadata) {
                    writer.write("${meta.attemptId}|${meta.targetId}|${meta.targetTitle}|${meta.timestampMs}|${meta.scorePercent}|${meta.averageSlantDegrees}|${meta.durationMs}|${meta.isBaseline}|${meta.targetSlantDegrees ?: ""}|${meta.styleId ?: ""}\n")
                }
                writer.flush()
                fos.flush()
                try { fos.fd.sync() } catch (_: Throwable) {}
            }
            Files.move(temp.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            lastKnownModified = manifestFile.lastModified()
            lastKnownLength = manifestFile.length()
        } catch (e: Throwable) {
            if (temp.exists()) temp.delete()
            throw e
        }
    }

    private fun seedInitialSamples() {
        val now = System.currentTimeMillis()
        val twoWeeksAgo = now - (14L * 24 * 3600 * 1000L)

        // 1. Amostra: basic_slant
        val slantBeforeStrokes = createSampleSlantStrokes(twoWeeksAgo, startX = 200f, startY = 150f, angleDeg = 61.5f, length = 280f, wobble = true)
        val slantAfterStrokes = createSampleSlantStrokes(now, startX = 200f, startY = 150f, angleDeg = 52.2f, length = 280f, wobble = false)

        val slantBefore = PracticeAttemptRecord(
            attemptId = "demo_slant_before",
            targetId = "basic_slant",
            targetTitle = "Traço Inclinado (Slant)",
            timestampMs = twoWeeksAgo,
            strokes = slantBeforeStrokes,
            scorePercent = 63,
            averageSlantDegrees = 61.5f,
            durationMs = 3800L,
            isBaseline = true
        )
        val slantAfter = PracticeAttemptRecord(
            attemptId = "demo_slant_after",
            targetId = "basic_slant",
            targetTitle = "Traço Inclinado (Slant)",
            timestampMs = now,
            strokes = slantAfterStrokes,
            scorePercent = 91,
            averageSlantDegrees = 52.2f,
            durationMs = 2100L,
            isBaseline = false
        )
        saveAttempt(slantBefore)
        saveAttempt(slantAfter)

        // 2. Amostra: underturn
        val underturnBeforeStrokes = createSampleUnderturnStrokes(twoWeeksAgo, startX = 180f, startY = 180f, sharpTurn = true)
        val underturnAfterStrokes = createSampleUnderturnStrokes(now, startX = 180f, startY = 180f, sharpTurn = false)

        val underturnBefore = PracticeAttemptRecord(
            attemptId = "demo_underturn_before",
            targetId = "underturn",
            targetTitle = "Curva Inferior (Underturn)",
            timestampMs = twoWeeksAgo,
            strokes = underturnBeforeStrokes,
            scorePercent = 66,
            averageSlantDegrees = 59.0f,
            durationMs = 4200L,
            isBaseline = true
        )
        val underturnAfter = PracticeAttemptRecord(
            attemptId = "demo_underturn_after",
            targetId = "underturn",
            targetTitle = "Curva Inferior (Underturn)",
            timestampMs = now,
            strokes = underturnAfterStrokes,
            scorePercent = 89,
            averageSlantDegrees = 52.5f,
            durationMs = 2400L,
            isBaseline = false
        )
        saveAttempt(underturnBefore)
        saveAttempt(underturnAfter)
    }

    private fun createSampleSlantStrokes(baseTime: Long, startX: Float, startY: Float, angleDeg: Float, length: Float, wobble: Boolean): List<Stroke> {
        val rad = Math.toRadians(angleDeg.toDouble())
        val deltaX = (Math.cos(rad) * length).toFloat()
        val deltaY = (Math.sin(rad) * length).toFloat()

        val points = mutableListOf<StrokePoint>()
        val count = 20
        for (i in 0..count) {
            val fraction = i / count.toFloat()
            val wobbleOffset = if (wobble) kotlin.math.sin(fraction * Math.PI.toFloat() * 4f) * 6f else 0f
            points.add(
                StrokePoint(
                    x = startX + (deltaX * fraction) + wobbleOffset,
                    y = startY + (deltaY * fraction),
                    tMs = baseTime + (i * 100L),
                    pressure = 0.4f + (fraction * 0.3f)
                )
            )
        }
        return listOf(
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = points,
                startedAtMs = baseTime,
                endedAtMs = baseTime + (count * 100L),
                color = android.graphics.Color.BLACK,
                baseWidthPx = 5.0f
            )
        )
    }

    private fun createSampleUnderturnStrokes(baseTime: Long, startX: Float, startY: Float, sharpTurn: Boolean): List<Stroke> {
        val points = mutableListOf<StrokePoint>()
        val count = 24
        for (i in 0..count) {
            val t = i / count.toFloat()
            val x = startX + (t * 140f)
            val y = if (sharpTurn) {
                if (t < 0.6f) startY + (t / 0.6f) * 120f else (startY + 120f) - ((t - 0.6f) / 0.4f) * 70f
            } else {
                startY + 120f * (1f - 4f * (t - 0.5f) * (t - 0.5f).coerceAtLeast(0f))
            }
            points.add(
                StrokePoint(
                    x = x,
                    y = y,
                    tMs = baseTime + (i * 80L),
                    pressure = if (t < 0.6f) 0.65f else 0.35f
                )
            )
        }
        return listOf(
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = points,
                startedAtMs = baseTime,
                endedAtMs = baseTime + (count * 80L),
                color = android.graphics.Color.BLACK,
                baseWidthPx = 5.0f
            )
        )
    }
}
