package com.scribe.caligrafia.ink.persistence.benchmark

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.PersistenceResult
import com.scribe.caligrafia.ink.persistence.StrokePersistenceStrategy
import com.scribe.caligrafia.ink.persistence.strategies.CompressedBlobStrategy
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.persistence.strategies.RelationalTableStrategy
import java.io.File
import java.util.UUID
import kotlin.math.sin
import kotlin.system.measureNanoTime

/**
 * Executor de benchmark empírico para o Spike de Persistência (SCR-006).
 *
 * Avalia as 3 estratégias de persistência sob as mesmas condições rigorosas:
 * 1. Medição de latência de gravação (serialização + I/O).
 * 2. Medição de latência de recarga (I/O + desserialização).
 * 3. Pegada física de armazenamento (tamanho em bytes e bytes por ponto).
 * 4. Validação matemática de fidelidade (comparação ponto-a-ponto de coordenadas, timestamps e sensores).
 */
class PersistenceBenchmarkRunner(private val baseDir: File) {

    val strategies: List<StrokePersistenceStrategy> = listOf(
        RelationalTableStrategy(File(baseDir, "relational")),
        CompressedBlobStrategy(File(baseDir, "blob")),
        DedicatedFileStrategy(File(baseDir, "file"))
    )

    /**
     * Executa o benchmark comparativo completo.
     * @param strokeCount Quantidade de traços simulados.
     * @param pointsPerStroke Média de pontos por traço.
     */
    fun runBenchmark(
        strokeCount: Int = 50,
        pointsPerStroke: Int = 60
    ): BenchmarkSuiteReport {
        val sampleStrokes = generateRealisticHandwritingSession(strokeCount, pointsPerStroke)
        val totalPoints = sampleStrokes.sumOf { it.points.size }
        val results = mutableListOf<PersistenceResult>()

        for (strategy in strategies) {
            val sessionId = "bench_${UUID.randomUUID().toString().take(8)}"

            // 1. Medir tempo de gravação
            var writtenBytes = 0L
            val writeNanos = measureNanoTime {
                writtenBytes = strategy.save(sessionId, sampleStrokes)
            }
            val writeMs = writeNanos / 1_000_000.0

            // 2. Medir tempo de leitura
            var loadedStrokes: List<Stroke> = emptyList()
            val readNanos = measureNanoTime {
                loadedStrokes = strategy.load(sessionId)
            }
            val readMs = readNanos / 1_000_000.0

            // 3. Validação de fidelidade matemática absoluta
            val isFidelityOk = verifyFidelity(sampleStrokes, loadedStrokes)

            val sizeBytes = strategy.getStorageSizeBytes(sessionId)

            // 4. Limpeza pós-teste
            strategy.delete(sessionId)

            results.add(
                PersistenceResult(
                    strategyName = strategy.formatName,
                    writeTimeMs = writeMs,
                    readTimeMs = readMs,
                    sizeBytes = sizeBytes,
                    strokeCount = strokeCount,
                    totalPoints = totalPoints,
                    bytesPerPoint = if (totalPoints > 0) sizeBytes.toDouble() / totalPoints else 0.0,
                    isFidelityPreserved = isFidelityOk
                )
            )
        }

        return BenchmarkSuiteReport(
            strokeCount = strokeCount,
            totalPoints = totalPoints,
            results = results
        )
    }

    /**
     * Gera uma sessão sintética determinística e realista de escrita com S Pen:
     * coordenadas curvas, densidade temporal de 120Hz-240Hz, curvas de pressão contínuas e tilt.
     */
    fun generateRealisticHandwritingSession(strokeCount: Int, pointsPerStroke: Int): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        var currentTime = 1_000_000L

        for (s in 0 until strokeCount) {
            val strokeId = "stroke_$s"
            val points = mutableListOf<StrokePoint>()
            val startX = 100f + (s % 5) * 80f
            val startY = 150f + (s / 5) * 100f

            for (p in 0 until pointsPerStroke) {
                val t = p / pointsPerStroke.toFloat()
                val x = startX + (t * 60f) + (sin(t * Math.PI.toFloat() * 2f) * 15f)
                val y = startY + (sin(t * Math.PI.toFloat()) * 40f)
                val time = currentTime + (p * 8L) // Amostragem a ~125Hz

                // Modulação realista de pressão física da S Pen (0.2 a 0.8)
                val pressure = 0.25f + (sin(t * Math.PI.toFloat()) * 0.5f)
                val tilt = 0.35f + (t * 0.1f)
                val orientation = 1.2f

                points.add(
                    StrokePoint(
                        x = x,
                        y = y,
                        tMs = time,
                        pressure = pressure,
                        tiltRad = tilt,
                        orientationRad = orientation
                    )
                )
            }

            val startedAt = points.first().tMs
            val endedAt = points.last().tMs
            currentTime = endedAt + 120L // Pausa entre traços

            strokes.add(
                Stroke(
                    id = strokeId,
                    tool = if (s == strokeCount - 1) ToolType.ERASER else ToolType.STYLUS,
                    points = points,
                    startedAtMs = startedAt,
                    endedAtMs = endedAt,
                    isCancelled = false
                )
            )
        }
        return strokes
    }

    private fun verifyFidelity(original: List<Stroke>, loaded: List<Stroke>): Boolean {
        if (original.size != loaded.size) return false

        for (i in original.indices) {
            val origS = original[i]
            val loadS = loaded[i]

            if (origS.id != loadS.id) return false
            if (origS.tool != loadS.tool) return false
            if (origS.startedAtMs != loadS.startedAtMs) return false
            if (origS.endedAtMs != loadS.endedAtMs) return false
            if (origS.isCancelled != loadS.isCancelled) return false
            if (origS.points.size != loadS.points.size) return false

            for (j in origS.points.indices) {
                val op = origS.points[j]
                val lp = loadS.points[j]

                if (op.x != lp.x || op.y != lp.y) return false
                if (op.tMs != lp.tMs) return false
                if (op.pressure != lp.pressure) return false
                if (op.tiltRad != lp.tiltRad) return false
                if (op.orientationRad != lp.orientationRad) return false
            }
        }
        return true
    }
}

data class BenchmarkSuiteReport(
    val strokeCount: Int,
    val totalPoints: Int,
    val results: List<PersistenceResult>
) {
    fun toMarkdown(): String {
        val sb = StringBuilder()
        sb.appendLine("### Relatório do Spike de Persistência (SCR-006)")
        sb.appendLine("- Carga de teste: $strokeCount traços, $totalPoints pontos vetoriais.")
        sb.appendLine()
        sb.appendLine("| Estratégia | Gravação (ms) | Leitura (ms) | Tamanho Total | Bytes/Ponto | Fidelidade 100% |")
        sb.appendLine("| :--- | :---: | :---: | :---: | :---: | :---: |")
        for (r in results) {
            val sizeKb = String.format("%.2f KB", r.sizeBytes / 1024.0)
            val bpp = String.format("%.1f B", r.bytesPerPoint)
            val wMs = String.format("%.2f ms", r.writeTimeMs)
            val rMs = String.format("%.2f ms", r.readTimeMs)
            val fid = if (r.isFidelityPreserved) "EXATA" else "FALHA"
            sb.appendLine("| ${r.strategyName} | $wMs | $rMs | $sizeKb | $bpp | $fid |")
        }
        return sb.toString()
    }
}
