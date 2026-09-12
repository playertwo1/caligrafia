package com.scribe.caligrafia.audit

import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import com.scribe.caligrafia.notebook.export.PageExporter
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Suíte de testes formais de aceitação que comprovam a correção de todos os achados
 * reproduzidos na Auditoria do Codex para o Scribe v0.2.0+.
 */
class AuditFixAcceptanceTest {

    private fun stroke(
        points: List<StrokePoint>,
        color: Int = 0xff123456.toInt(),
        baseWidthPx: Float = 8f,
        tool: ToolType = ToolType.STYLUS
    ) = Stroke("probe", tool, points, 100, 200, color = color, baseWidthPx = baseWidthPx)

    /**
     * A11: Replay em frame intermediário DEVE preservar cor e espessura originais do traço ativo.
     */
    @Test
    fun replayPreservesStyleDuringActiveStroke() {
        val s = stroke(listOf(StrokePoint(0f, 0f, 100), StrokePoint(10f, 10f, 200)))
        val engine = StrokeReplayEngine(listOf(s))
        val active = engine.computeFrameAt(50).activeStroke

        assertNotNull("Traço ativo deve existir no instante intermediário 50ms", active)
        assertEquals("Cor deve ser preservada no frame parcial", 0xff123456.toInt(), active!!.color)
        assertEquals("Espessura deve ser preservada no frame parcial", 8f, active.baseWidthPx)
    }

    /**
     * A07: Sensores físicos reportando 0.0f (ex: pena exatamente perpendicular à tela, ou pressão mínima)
     * NÃO devem ser transformados indevidamente em null.
     */
    @Test
    fun zeroSensorValuesArePreserved() {
        val m = StrokeCapturePipeline::class.java.getDeclaredMethod(
            "createPoint",
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType
        )
        m.isAccessible = true
        val p = m.invoke(StrokeCapturePipeline(), 1f, 2f, 100L, 0f, 0f, 0f) as StrokePoint

        assertEquals("Pressão 0.0f deve ser preservada", 0f, p.pressure)
        assertEquals("Tilt 0.0f deve ser preservado", 0f, p.tiltRad)
        assertEquals("Orientation 0.0f deve ser preservada", 0f, p.orientationRad)
    }

    /**
     * A05: Borracha deve conectar o último ponto do evento anterior ao lote atual,
     * apagando traços interceptados entre dois MotionEvents (DOWN em 0,0 e MOVE em 100,0).
     */
    @Test
    fun eraserBridgesAcrossEventsAndErasesCrossingStroke() {
        val repo = InMemoryStrokeRepository()
        // Traço vertical de (50, -50) até (50, 50)
        repo.addStroke(stroke(listOf(StrokePoint(50f, -50f, 100), StrokePoint(50f, 50f, 200))))
        assertEquals(1, repo.count)

        val pipeline = StrokeCapturePipeline(onEraserPointsAdded = {
            repo.eraseStrokesIntersecting(it, 5f)
        })

        // Evento 1: Encosta a borracha em (0, 0)
        pipeline.onPointerDown(0, ToolType.ERASER, StrokePoint(0f, 0f, 100))
        // Evento 2: Move a borracha até (100, 0) cruzando a reta vertical em x=50
        pipeline.onPointerMove(0, listOf(StrokePoint(100f, 0f, 200)))

        // O traço vertical que cruza em x=50 DEVE ser removido
        assertEquals("Traço interceptado deve ser apagado pela continuidade de segmento", 0, repo.count)
    }

    /**
     * A04: Borracha é um comando de exclusão e NUNCA deve ser adicionada como traço de tinta
     * ao completar ou gerar prévia ativa.
     */
    @Test
    fun eraserNeverDeliversCompletedStrokeOrPreview() {
        var completedStrokeCalled = false
        val pipeline = StrokeCapturePipeline(onStrokeCompleted = {
            completedStrokeCalled = true
        })

        pipeline.onPointerDown(0, ToolType.ERASER, StrokePoint(0f, 0f, 100))
        assertNull("Borracha não deve gerar prévia de traço de tinta", pipeline.getActiveStrokePreview())

        pipeline.onPointerUp(0, listOf(StrokePoint(50f, 50f, 150)), 150)
        assertFalse("Borracha nunca deve invocar onStrokeCompleted", completedStrokeCalled)
    }

    /**
     * A02: Gravação atômica. Se uma falha ocorrer durante o salvamento, a versão anterior
     * do arquivo DEVE permanecer íntegra e legível.
     */
    @Test
    fun failedSavePreservesPreviousValidFile() {
        val dir = Files.createTempDirectory("scribe-atomic-test-").toFile()
        try {
            val strategy = DedicatedFileStrategy(dir)
            val file = dir.resolve("page.scribe")
            val s = stroke(listOf(StrokePoint(0f, 0f, 100)))

            // 1. Salva versão válida inicial
            strategy.save(file, listOf(s))
            val originalBytes = file.readBytes()
            assertTrue("Arquivo original deve existir", file.exists() && originalBytes.isNotEmpty())

            // 2. Força falha de serialização com ID que estoura writeUTF
            val failure = runCatching {
                strategy.save(file, listOf(s.copy(id = "x".repeat(70000))))
            }.exceptionOrNull()

            assertNotNull("Deve ocorrer falha controlada", failure)
            // 3. O arquivo original DEVE permanecer intacto e recuperável
            assertArrayEquals("Conteúdo original não pode ser corrompido ou truncado", originalBytes, file.readBytes())
            val restored = strategy.load(file)
            assertEquals("Traços da versão anterior devem ser 100% legíveis", 1, restored.size)
            assertEquals("probe", restored[0].id)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * A12 & A13:
     * - Exercício incompleto (apenas 2 pontos de 16) NÃO deve passar (isPassed = false).
     * - Ângulo de referência do BASIC_SLANT deve estar alinhado à pauta de 52 graus.
     */
    @Test
    fun incompleteAttemptFailsAndReferenceSlantMatches52Degrees() {
        val g = ReferenceGlyphCatalog.BASIC_SLANT
        val band = GuidelineBand(0, 0f, 100f, 200f, 300f)
        val pts = g.strokes.first().points.mapIndexed { i, p ->
            val q = GeometricFeedbackEvaluator.mapToScreen(p, band, 0f, 80f)
            StrokePoint(q.x, q.y, 100L + i)
        }

        val full = GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts)), g, band, 0f, 80f, SlantConfig(52f))
        val tiny = GeometricFeedbackEvaluator.evaluate(listOf(stroke(pts.take(2))), g, band, 0f, 80f, SlantConfig(52f))

        // 1. Traço completo deve passar com ângulo medido calibrado em 52°
        assertTrue("Traço completo deve ser aprovado", full.isPassed)
        assertNotNull("Ângulo medido deve existir", full.slant.measuredAngleDegrees)
        val measuredAngle = full.slant.measuredAngleDegrees!!
        assertEquals("Ângulo do modelo BASIC_SLANT deve ser exatamente 52°", 52.0f, measuredAngle, 1.5f)

        // 2. Traço incompleto de 2 pontos (12% da letra) NÃO deve ser aprovado
        assertFalse("Tentativa ínfima/incompleta NÃO deve ser aprovada", tiny.isPassed)
        assertTrue("Nota de traço incompleto deve ser baixa (< 40%)", tiny.scorePercent < 40)
        assertTrue("Deve conter aviso pedagógico de traço incompleto", tiny.feedbackMessages.any { it.contains("incompleto") })
    }

    /**
     * A09: Em ambiente JVM sem Skia/gráficos, o exportador deve lançar IOException clara
     * e NUNCA retornar falso sucesso escrevendo um stub inválido de 8 bytes.
     */
    @Test
    fun exportDoesNotReturnFakeEightByteFile() {
        val dir = Files.createTempDirectory("scribe-export-audit-").toFile()
        try {
            val page = NotebookPage(id = "p", notebookId = "n")
            val targetFile = dir.resolve("p.png")

            val result = runCatching {
                PageExporter.exportToPng(page, emptyList(), targetFile)
            }

            if (result.isFailure) {
                assertTrue("Deve lançar IOException explicativa", result.exceptionOrNull() is IOException)
                assertFalse("Arquivo de 8 bytes falso não deve ter sido criado", targetFile.exists() && targetFile.length() == 8L)
            } else {
                val file = result.getOrThrow()
                assertTrue("Se sucesso com runtime gráfico, arquivo deve ser imagem real", file.length() > 8L)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
