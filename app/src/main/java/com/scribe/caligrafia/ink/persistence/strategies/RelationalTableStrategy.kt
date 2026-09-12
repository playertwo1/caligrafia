package com.scribe.caligrafia.ink.persistence.strategies

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.StrokePersistenceStrategy
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Estratégia A: Persistência Relacional Normalizada (Tabela Ponto-a-Ponto).
 *
 * Modela a abordagem padrão de banco relacional (SQLite / Room clássico):
 * - Tabela de traços: id, tool, startedAtMs, endedAtMs, isCancelled
 * - Tabela de pontos: strokeId, pointIndex, x, y, tMs, pressure, tiltRad, orientationRad
 *
 * Características:
 * - Altamente normalizada e consultável por SQL.
 * - Desvantagem teórica: alto overhead de I/O e amplificação de armazenamento
 *   devido a foreign keys, metadados por linha e parse de cada coluna para milhares de pontos.
 */
class RelationalTableStrategy(private val baseDir: File) : StrokePersistenceStrategy {

    override val formatName: String = "Tabela Relacional (Ponto-a-Ponto)"
    override val description: String = "Cada ponto é uma linha independente referenciando a chave primária do traço."

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    private fun getStrokesTableFile(sessionId: String): File =
        File(baseDir, "${sessionId}_tbl_strokes.csv")

    private fun getPointsTableFile(sessionId: String): File =
        File(baseDir, "${sessionId}_tbl_points.csv")

    override fun save(sessionId: String, strokes: List<Stroke>): Long {
        val strokesFile = getStrokesTableFile(sessionId)
        val pointsFile = getPointsTableFile(sessionId)

        // 1. Gravar tabela de strokes
        BufferedWriter(FileWriter(strokesFile)).use { writer ->
            writer.write("id,tool,startedAtMs,endedAtMs,isCancelled\n")
            for (s in strokes) {
                writer.write("${s.id},${s.tool.name},${s.startedAtMs},${s.endedAtMs},${s.isCancelled}\n")
            }
        }

        // 2. Gravar tabela de pontos com foreign key para o stroke
        BufferedWriter(FileWriter(pointsFile)).use { writer ->
            writer.write("strokeId,pointIndex,x,y,tMs,pressure,tiltRad,orientationRad\n")
            for (s in strokes) {
                for ((idx, p) in s.points.withIndex()) {
                    val pressStr = p.pressure?.toString() ?: ""
                    val tiltStr = p.tiltRad?.toString() ?: ""
                    val orientStr = p.orientationRad?.toString() ?: ""
                    writer.write("${s.id},$idx,${p.x},${p.y},${p.tMs},$pressStr,$tiltStr,$orientStr\n")
                }
            }
        }

        return strokesFile.length() + pointsFile.length()
    }

    override fun load(sessionId: String): List<Stroke> {
        val strokesFile = getStrokesTableFile(sessionId)
        val pointsFile = getPointsTableFile(sessionId)

        if (!strokesFile.exists() || !pointsFile.exists()) return emptyList()

        // 1. Ler e mapear pontos por strokeId
        val pointsByStrokeId = mutableMapOf<String, MutableList<IndexedPoint>>()
        BufferedReader(FileReader(pointsFile)).use { reader ->
            reader.readLine() // Pula cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(',')
                if (parts.size >= 8) {
                    val strokeId = parts[0]
                    val idx = parts[1].toInt()
                    val x = parts[2].toFloat()
                    val y = parts[3].toFloat()
                    val tMs = parts[4].toLong()
                    val pressure = parts[5].takeIf { it.isNotEmpty() }?.toFloat()
                    val tiltRad = parts[6].takeIf { it.isNotEmpty() }?.toFloat()
                    val orientRad = parts[7].takeIf { it.isNotEmpty() }?.toFloat()

                    val point = StrokePoint(x, y, tMs, pressure, tiltRad, orientRad)
                    pointsByStrokeId.computeIfAbsent(strokeId) { mutableListOf() }
                        .add(IndexedPoint(idx, point))
                }
            }
        }

        // 2. Ler tabela de strokes e reconstruir traços
        val strokes = mutableListOf<Stroke>()
        BufferedReader(FileReader(strokesFile)).use { reader ->
            reader.readLine() // Pula cabeçalho
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(',')
                if (parts.size >= 5) {
                    val id = parts[0]
                    val tool = ToolType.valueOf(parts[1])
                    val startedAtMs = parts[2].toLong()
                    val endedAtMs = parts[3].toLong()
                    val isCancelled = parts[4].toBoolean()

                    val sortedPoints = pointsByStrokeId[id]
                        ?.sortedBy { it.index }
                        ?.map { it.point }
                        ?: emptyList()

                    strokes.add(
                        Stroke(
                            id = id,
                            tool = tool,
                            points = sortedPoints,
                            startedAtMs = startedAtMs,
                            endedAtMs = endedAtMs,
                            isCancelled = isCancelled
                        )
                    )
                }
            }
        }

        return strokes
    }

    override fun getStorageSizeBytes(sessionId: String): Long {
        val s = getStrokesTableFile(sessionId).takeIf { it.exists() }?.length() ?: 0L
        val p = getPointsTableFile(sessionId).takeIf { it.exists() }?.length() ?: 0L
        return s + p
    }

    override fun delete(sessionId: String): Boolean {
        val s = getStrokesTableFile(sessionId).delete()
        val p = getPointsTableFile(sessionId).delete()
        return s || p
    }

    private data class IndexedPoint(val index: Int, val point: StrokePoint)
}
