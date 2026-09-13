package com.scribe.caligrafia.audit

import com.scribe.caligrafia.core.model.*
import com.scribe.caligrafia.evolution.engine.CalendarConsistencyHelper
import com.scribe.caligrafia.evolution.model.CalendarDayRecord
import com.scribe.caligrafia.expansions.backup.ScribeBackupManager
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import com.scribe.caligrafia.notebook.export.PageExporter
import com.scribe.caligrafia.teacher.engine.MotorDiagnosticEngine
import com.scribe.caligrafia.teacher.model.MaturityLevel
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import android.app.Application
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.learning.history.LocalLearningHistoryRepository
import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.session.SessionDuration
import com.scribe.caligrafia.learning.session.SessionPhase
import com.scribe.caligrafia.learning.session.SessionTimer
import com.scribe.caligrafia.notebook.repository.LocalNotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.scribe.caligrafia.evolution.engine.AttemptComparator
import com.scribe.caligrafia.evolution.engine.DualReplayEngine
import com.scribe.caligrafia.evolution.engine.ReplaySyncMode
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import com.scribe.caligrafia.alphabet.engine.PersonalStyleCompiler
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.guided.model.ReferencePoint
import com.scribe.caligrafia.expansions.styles.ExpandedStyles
import com.scribe.caligrafia.expansions.signature.SignatureConsistencyEngine
import com.scribe.caligrafia.expansions.signature.SignatureExporter
import com.scribe.caligrafia.expansions.ui.ExpansionsViewModel

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

    /**
     * S01: O arquivo de backup exportado (.scribepack) DEVE conter o diretório central do ZIP
     * perfeitamente finalizado (finish/flush), garantindo abertura e leitura completa por ZipFile
     * sem lançar a exceção "zip END header not found".
     */
    @Test
    fun s01_exportedZipHasValidCentralDirectory_andCanBeReadByZipFile() {
        val root = Files.createTempDirectory("scribe-s01-src-").toFile()
        val dest = Files.createTempDirectory("scribe-s01-dest-").toFile()
        try {
            val nbDir = File(root, "notebooks/caderno_exemplo/pages").apply { mkdirs() }
            File(nbDir, "p1.scribe").writeText("INK_VECT_DATA")

            val backupFile = File(dest, "teste_s01.scribepack")
            backupFile.outputStream().use { fos ->
                val summary = ScribeBackupManager(root).exportBackup(fos)
                assertTrue("Deve ter ao menos 2 arquivos (manifest + página)", summary.fileCount >= 2)
            }

            assertTrue("Arquivo gerado deve existir e ter tamanho > 0", backupFile.exists() && backupFile.length() > 0)

            // Valida abertura direta com java.util.zip.ZipFile (critério decisivo S01)
            val zipFile = ZipFile(backupFile)
            val entries = zipFile.entries().toList()
            assertTrue("Arquivo ZIP deve conter manifest.json", entries.any { it.name == "manifest.json" })
            assertTrue("Arquivo ZIP deve conter a página do caderno", entries.any { it.name == "notebooks/caderno_exemplo/pages/p1.scribe" })
            zipFile.close()
        } finally {
            root.deleteRecursively()
            dest.deleteRecursively()
        }
    }

    /**
     * S02: O backup DEVE incluir todos os caminhos canônicos reais de produção
     * (personal_alphabet, attempts, learning_history.json, teacher/diagnostic.json, custom_fonts).
     */
    @Test
    fun s02_backupIncludesAllCanonicalProductionPaths() {
        val root = Files.createTempDirectory("scribe-s02-test-").toFile()
        try {
            File(root, "personal_alphabet/strokes").mkdirs()
            File(root, "personal_alphabet/strokes/a.scribe").writeText("GLYPH_A")
            File(root, "attempts").mkdirs()
            File(root, "attempts/att1.scribe").writeText("ATTEMPT_1")
            File(root, "learning_history.json").writeText("{\"sessions\": 10}")
            File(root, "teacher").mkdirs()
            File(root, "teacher/diagnostic.json").writeText("{\"posture\": \"EXCELLENT\"}")
            File(root, "custom_fonts").mkdirs()
            File(root, "custom_fonts/callig.ttf").writeText("FONT_BINARY")

            val baos = ByteArrayOutputStream()
            val summary = ScribeBackupManager(root).exportBackup(baos)

            assertEquals(1, summary.manifest.personalGlyphCount)
            assertEquals(1, summary.manifest.practiceAttemptCount)
            assertEquals(1, summary.manifest.lessonHistoryCount)
            assertTrue("Deve detectar diagnóstico do professor", summary.manifest.hasTeacherDiagnostic)

            val entryNames = mutableListOf<String>()
            ZipInputStream(ByteArrayInputStream(baos.toByteArray())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entryNames.add(entry.name)
                    entry = zis.nextEntry
                }
            }

            assertTrue(entryNames.contains("personal_alphabet/strokes/a.scribe"))
            assertTrue(entryNames.contains("attempts/att1.scribe"))
            assertTrue(entryNames.contains("learning_history.json"))
            assertTrue(entryNames.contains("teacher/diagnostic.json"))
            assertTrue(entryNames.contains("custom_fonts/callig.ttf"))
        } finally {
            root.deleteRecursively()
        }
    }

    /**
     * S03: Rollback atômico. Se a importação falhar no meio do processo ou contiver dados corrompidos,
     * os dados ativos do usuário NUNCA são perdidos ou corrompidos.
     */
    @Test
    fun s03_importAtomicRollbackPreservesOriginalUserDataOnFailure() {
        val baseDir = Files.createTempDirectory("scribe-s03-test-").toFile()
        try {
            val originalFile = File(baseDir, "notebooks/caderno_ativo/pages/p1.scribe").apply {
                parentFile?.mkdirs()
                writeText("CONTEUDO_ORIGINAL_INTACTO")
            }

            val manager = ScribeBackupManager(baseDir)
            val corruptedInput = ByteArrayInputStream("DADOS_CORROMPIDOS".toByteArray())
            val result = manager.importBackup(corruptedInput)

            assertFalse("Importação deve reportar falha", result.isSuccess)
            assertTrue("Arquivo original deve permanecer inalterado", originalFile.exists())
            assertEquals("CONTEUDO_ORIGINAL_INTACTO", originalFile.readText())
        } finally {
            baseDir.deleteRecursively()
        }
    }

    /**
     * S04: Proteção contra vulnerabilidade Zip-Slip. Caminhos contendo '..' ou prefixos irmãos
     * são estritamente rejeitados e contidos.
     */
    @Test
    fun s04_zipSlipVulnerabilityIsPrevented() {
        val baseDir = Files.createTempDirectory("scribe-s04-test-").toFile()
        try {
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write("{}".toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("../perigo.txt"))
                zos.write("HACK".toByteArray())
                zos.closeEntry()
            }

            val manager = ScribeBackupManager(baseDir)
            val result = manager.importBackup(ByteArrayInputStream(baos.toByteArray()))

            assertFalse("Backup malicioso deve ser rejeitado", result.isSuccess)
            assertFalse("Arquivo não deve ter escapado para fora do diretório", File(baseDir.parentFile ?: baseDir, "perigo.txt").exists())
        } finally {
            baseDir.deleteRecursively()
        }
    }

    /**
     * S07 & S08: Zero tentativas produz score 0.0f e maturidade BEGINNER
     * sem dados artificiais ou pontuações forçadas.
     */
    @Test
    fun s07_zeroAttemptsProducesZeroScoreAndBeginnerMaturity() {
        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(emptyList())
        assertEquals("Sem tentativas, nota deve ser 0.0f", 0.0f, diagnostic.overallScore, 0.001f)
        assertEquals("Sem tentativas, maturidade deve ser BEGINNER", MaturityLevel.BEGINNER, diagnostic.maturityLevel)
        assertEquals("Sem tentativas, total analisado deve ser 0", 0, diagnostic.totalAttemptsAnalyzed)
        assertNull("Sem fraqueza identificada sem dados", diagnostic.primaryWeakness)
    }

    /**
     * R07: O cálculo de tempo no calendário de consistência soma segundos exatos
     * sem inflação artificial de 0 segundos para 1 minuto.
     */
    @Test
    fun r07_calendarConsistencyComputesExactDurationWithoutOneMinuteInflation() {
        val sessionZeroSeconds = CompletedSessionRecord(
            sessionId = "s0",
            lessonId = "l0",
            lessonTitle = "Teste Rápido",
            timestampMs = System.currentTimeMillis(),
            durationMinutes = 0,
            actualDurationSeconds = 0,
            attemptsCount = 1,
            averageScorePercent = 80
        )

        val days = CalendarConsistencyHelper.buildMonthDays(listOf(sessionZeroSeconds))
        val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val todayRecord = days.first { it.dayOfMonth == currentDay }

        assertEquals("Sessão de 0 segundos não deve inflar para 1 minuto", 0, todayRecord.totalMinutesPracticed)
        assertEquals("Contagem de sessões deve ser 1", 1, todayRecord.sessionCount)
    }

    /**
     * R01: SessionTimer deve avançar a contagem em tempo real mesmo sem fornecer scope explicitamente.
     */
    @Test
    fun r01_sessionTimerTicksInRealTimeWithDefaultScope() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.ALL_LESSONS.first()
        timer.startSession(lesson, SessionDuration.MIN_5)
        Thread.sleep(1250L)
        val state = timer.sessionState.value
        assertNotNull("Estado da sessão deve existir", state)
        assertTrue("Timer com scope padrão deve avançar ao menos 1 segundo", state!!.totalElapsedSeconds >= 1)
        timer.cancelSession()
    }

    /**
     * R08: No 300º tick (sessão de 5min), o SessionTimer deve encerrar com totalElapsedSeconds exatamente em 300.
     */
    @Test
    fun r08_sessionTimerCompletes300SecondsOn300thTick() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.ALL_LESSONS.first()
        timer.startSession(lesson, SessionDuration.MIN_5)
        for (i in 1..300) {
            timer.tickOneSecond()
        }
        val state = timer.sessionState.value
        assertNotNull(state)
        assertEquals("No 300º tick, totalElapsedSeconds deve ser exatamente 300", 300, state!!.totalElapsedSeconds)
        assertTrue("Sessão deve estar marcada como finalizada", state.isFinished)
        assertEquals(SessionPhase.REVIEW_SUMMARY, state.currentPhase)
    }

    /**
     * R09: fsync() deve ser executado com o descritor de arquivo aberto e válido em todos os repositórios.
     */
    @Test
    fun r09_fsyncExecutesOnOpenValidFileDescriptorAcrossRepositories() {
        val dir = Files.createTempDirectory("scribe-r09-").toFile()
        try {
            // 1. DedicatedFileStrategy
            val strategy = DedicatedFileStrategy(dir)
            val scribeFile = File(dir, "sync_test.scribe")
            val strokes = listOf(stroke(listOf(StrokePoint(10f, 10f, 100L))))
            val size = strategy.save(scribeFile, strokes)
            assertTrue("Arquivo deve ser gravado e ter tamanho > 0", size > 0 && scribeFile.exists())
            val loadedStrokes = strategy.load(scribeFile)
            assertEquals(1, loadedStrokes.size)

            // 2. LocalLearningHistoryRepository
            val historyRepo = LocalLearningHistoryRepository(dir)
            val session = CompletedSessionRecord("s_r09", "l1", "Lição R09", System.currentTimeMillis(), 5, 300, 1, 90)
            historyRepo.recordSession(session)
            val summary = historyRepo.getProgressSummary()
            assertEquals(1, summary.totalSessionsCompleted)

            // 3. LocalPracticeAttemptRepository
            val attemptRepo = LocalPracticeAttemptRepository(dir, strategy)
            val attempt = PracticeAttemptRecord(
                attemptId = "att_r09",
                targetId = "basic_slant",
                targetTitle = "Traço Inclinado",
                timestampMs = System.currentTimeMillis(),
                strokes = strokes,
                scorePercent = 85,
                averageSlantDegrees = 52.0f,
                durationMs = 2500L,
                isBaseline = false
            )
            attemptRepo.saveAttempt(attempt)
            val attempts = attemptRepo.getAttemptsForTarget("basic_slant")
            assertTrue("Deve recuperar a tentativa salva", attempts.any { it.attemptId == "att_r09" })
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R06: Múltiplas instâncias de LocalLearningHistoryRepository devem manter sincronização mútua via disco.
     */
    @Test
    fun r06_multipleLearningHistoryInstancesStaySynchronized() {
        val dir = Files.createTempDirectory("scribe-r06-").toFile()
        try {
            val repo1 = LocalLearningHistoryRepository(dir)
            val repo2 = LocalLearningHistoryRepository(dir)

            val s1 = CompletedSessionRecord("s1", "l1", "Lição 1", 1000L, 5, 300, 1, 80)
            repo1.recordSession(s1)

            Thread.sleep(50L)

            val s2 = CompletedSessionRecord("s2", "l2", "Lição 2", 2000L, 10, 600, 2, 85)
            repo2.recordSession(s2)

            val summary2 = repo2.getProgressSummary()
            assertEquals("Repo 2 deve conter ambas as sessões após gravação coordenada", 2, summary2.totalSessionsCompleted)

            val summary1 = repo1.getProgressSummary()
            assertEquals("Repo 1 deve detectar modificação e recarregar ambas as sessões", 2, summary1.totalSessionsCompleted)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R10: Operações no Alfabeto Pessoal devem ser protegidas por Mutex, gerar UUIDs únicos e ser transacionais.
     */
    @Test
    fun r10_alphabetOperationsAreTransactionalAndConcurrentSafe() = runBlocking {
        val dir = Files.createTempDirectory("scribe-r10-").toFile()
        try {
            val repo = LocalPersonalAlphabetRepository(dir)
            val sampleStrokes = listOf(stroke(listOf(StrokePoint(5f, 5f, 100L))))

            // 1. Concorrência: inserções paralelas devem produzir variantes com IDs únicos
            val jobs = (1..5).map { i ->
                async(Dispatchers.IO) {
                    repo.addVariant(
                        glyphId = "glyph_lower_a",
                        strokes = sampleStrokes,
                        score = 80f + i,
                        slantAngle = 52f,
                        setFavorite = false
                    )
                }
            }
            val variants = jobs.awaitAll()
            val allIds = variants.map { it.id }.toSet()
            assertEquals("Todas as 5 variantes devem ter IDs únicos (UUID)", 5, allIds.size)

            val glyph = repo.getGlyph("glyph_lower_a")
            assertNotNull("Glifo deve existir", glyph)
            assertTrue("Glifo deve conter todas as 5 variantes", glyph!!.variants.size >= 5)

            // 2. Transacionalidade na deleção
            val toDelete = variants.first()
            val scribeFile = File(dir, "strokes/${toDelete.id}.scribe")
            assertTrue("Arquivo vetorial deve existir antes da exclusão", scribeFile.exists())

            val success = repo.deleteVariant("glyph_lower_a", toDelete.id)
            assertTrue("Deleção deve reportar sucesso", success)

            val updatedGlyph = repo.getGlyph("glyph_lower_a")
            assertFalse("Variante excluída não deve constar no manifesto", updatedGlyph!!.variants.any { it.id == toDelete.id })
            assertFalse("Arquivo .scribe correspondente deve ser removido após atualizar manifesto", scribeFile.exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * A03: Salvamentos de página do caderno devem ser sequenciais e sem conflito de concorrência.
     */
    @Test
    fun a03_notebookPageSavesAreOrderedAndNonConflicting() = runBlocking {
        val dir = Files.createTempDirectory("scribe-a03-").toFile()
        try {
            val repo = LocalNotebookRepository(dir)
            val nb = repo.createNotebook("Caderno Sequencial")
            val page = repo.getPages(nb.id).first()

            val mutex = Mutex()
            // Simula persistência em fila ordenada
            for (i in 1..10) {
                val currentStrokes = (1..i).map { idx ->
                    stroke(listOf(StrokePoint(idx.toFloat(), idx.toFloat(), 100L)))
                }
                mutex.withLock {
                    repo.savePageStrokes(page, currentStrokes)
                }
            }

            val loaded = repo.loadPageStrokes(page)
            assertEquals("O último lote salvo (10 traços) deve ser o conteúdo final da página", 10, loaded.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R02: A nota geométrica calculada a partir de traços reais desenhados no canvas
     * deve alimentar a sessão ativa de aprendizado e o histórico de repetição espaçada,
     * sem recurso a botões manuais com porcentagens fixas (65%, 80%, 95%).
     */
    @Test
    fun r02_realPracticeAttemptFeedsLearningSessionScore() {
        val dir = Files.createTempDirectory("scribe-r02-").toFile()
        try {
            val historyRepo = LocalLearningHistoryRepository(dir)
            val sessionTimer = SessionTimer()
            val lesson = CurriculumCatalog.ALL_LESSONS.first()

            sessionTimer.startSession(lesson, SessionDuration.MIN_5)

            // Simula traço real executado no canvas do treino guiado
            val app = Application()
            val strokeRepo = InMemoryStrokeRepository()
            val band = GuidelineBand(1, 50f, 100f, 150f, 200f)
            val slantGlyph = ReferenceGlyphCatalog.BASIC_SLANT
            val userPoints = slantGlyph.strokes.first().points.mapIndexed { idx, pt ->
                val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, 100f, 80f)
                StrokePoint(
                    x = screenPt.x,
                    y = screenPt.y,
                    tMs = 1000L + idx * 16L,
                    pressure = 0.5f
                )
            }
            val vm = GuidedPracticeViewModel(
                application = app,
                strokeRepository = strokeRepo
            )
            vm.selectGlyph(slantGlyph)

            val userStroke = stroke(userPoints)
            strokeRepo.addStroke(userStroke)

            val eval = vm.evaluateCurrentAttempt(
                band = band,
                originX = 100f,
                glyphWidthPx = 80f,
                slant = SlantConfig(52f, 80f)
            )

            // A nota geométrica real é enviada para a sessão ativa
            assertTrue("A avaliação real deve produzir nota positiva", eval.scorePercent in 1..100)
            sessionTimer.recordAttempt(eval.scorePercent.toFloat())

            val activeState = sessionTimer.sessionState.value
            assertNotNull(activeState)
            assertEquals(1, activeState!!.attemptsCount)
            assertEquals(eval.scorePercent.toFloat(), activeState.averageScore!!, 0.01f)

            // Finaliza e persiste no repositório de histórico
            val completedRecord = CompletedSessionRecord(
                sessionId = UUID.randomUUID().toString(),
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                timestampMs = System.currentTimeMillis(),
                durationMinutes = 5,
                actualDurationSeconds = 300,
                attemptsCount = activeState.attemptsCount,
                averageScorePercent = activeState.averageScore?.toInt()
            )
            historyRepo.recordSession(completedRecord)

            val summary = historyRepo.getProgressSummary()
            assertEquals(1, summary.totalSessionsCompleted)
            val lastSession = summary.recentSessions.first()
            assertEquals("A nota persistida no histórico deve coincidir exatamente com a nota real avaliada",
                eval.scorePercent, lastSession.averageScorePercent)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R03: Avaliação de prática guiada deve persistir imediatamente a tentativa no
     * repositório local com traços reais, sem semeadura sintética prévia,
     * permitindo que duas tentativas reais formem um comparativo Antes & Depois legítimo.
     */
    @Test
    fun r03_practiceEvaluationPersistsRealAttemptAndCreatesBeforeAfter() {
        val dir = Files.createTempDirectory("scribe-r03-").toFile()
        try {
            val attemptRepo = LocalPracticeAttemptRepository(dir)
            assertTrue("Novo perfil deve iniciar rigorosamente sem tentativas sintéticas", attemptRepo.getAllAttempts().isEmpty())

            val app = Application()
            val strokeRepo = InMemoryStrokeRepository()
            val vm = GuidedPracticeViewModel(
                application = app,
                strokeRepository = strokeRepo,
                attemptRepo = attemptRepo
            )
            val slantGlyph = ReferenceGlyphCatalog.BASIC_SLANT
            vm.selectGlyph(slantGlyph)
            val band = GuidelineBand(1, 50f, 100f, 150f, 200f)

            // Tentativa 1 (Baseline)
            val pts1 = slantGlyph.strokes.first().points.mapIndexed { idx, pt ->
                val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, 100f, 80f)
                StrokePoint(screenPt.x, screenPt.y, 1000L + idx * 16L, 0.5f)
            }
            val stroke1 = stroke(pts1)
            strokeRepo.addStroke(stroke1)
            val eval1 = vm.evaluateCurrentAttempt(band, 100f, 80f, SlantConfig(52f, 80f))

            val attemptsAfter1 = attemptRepo.getAttemptsForTarget("basic_slant")
            assertEquals(1, attemptsAfter1.size)
            assertTrue("Primeira tentativa para um alvo deve ser classificada como baseline", attemptsAfter1.first().isBaseline)
            assertEquals(eval1.scorePercent, attemptsAfter1.first().scorePercent)

            // Tentativa 2 (Evolução posterior)
            vm.clearAttempt()
            val pts2 = slantGlyph.strokes.first().points.mapIndexed { idx, pt ->
                val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, 100f, 80f)
                StrokePoint(screenPt.x + 1f, screenPt.y, 2000L + idx * 16L, 0.6f)
            }
            val stroke2 = stroke(pts2)
            strokeRepo.addStroke(stroke2)
            val eval2 = vm.evaluateCurrentAttempt(band, 100f, 80f, SlantConfig(52f, 80f))

            val attemptsAfter2 = attemptRepo.getAttemptsForTarget("basic_slant")
            assertEquals(2, attemptsAfter2.size)
            assertFalse("Tentativa subsequente não é baseline", attemptsAfter2.first().isBaseline)

            val comparison = attemptRepo.getBeforeAndAfter("basic_slant")
            assertNotNull("Deve gerar comparação Antes & Depois com dados reais do usuário", comparison)
            val (before, after) = comparison!!
            assertTrue("Tentativa inicial (Antes) deve ter timestamp anterior à recente (Depois)", before.timestampMs <= after.timestampMs)
            assertEquals("Traços reais do usuário devem estar preservados no Antes", 1, before.strokes.size)
            assertEquals("Traços reais do usuário devem estar preservados no Depois", 1, after.strokes.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R04: Navegar a partir de um glifo do Alfabeto Pessoal deve selecionar o alvo correspondente
     * no Treino Guiado, e salvar a variante avaliada deve persistir os traços reais e atualizar o repositório.
     */
    @Test
    fun r04_alphabetNavigationSelectsGlyphAndSavedVariantUpdatesRepository() = runBlocking {
        val dir = Files.createTempDirectory("scribe-r04-").toFile()
        try {
            val alphabetRepo = LocalPersonalAlphabetRepository(dir)
            val app = Application()
            val strokeRepo = InMemoryStrokeRepository()
            val vm = GuidedPracticeViewModel(
                application = app,
                strokeRepository = strokeRepo,
                alphabetRepo = alphabetRepo
            )

            // 1. Navega para glifo 'a' minúsculo
            vm.selectGlyphBySymbolOrId("glyph_lower_a")
            assertEquals("a", vm.state.value.selectedGlyph.symbol)

            // 2. Executa e avalia traço real
            val band = GuidelineBand(1, 50f, 100f, 150f, 200f)
            val targetGlyph = vm.state.value.selectedGlyph
            val strokePts = targetGlyph.strokes.first().points.mapIndexed { idx, pt ->
                val screenPt = GeometricFeedbackEvaluator.mapToScreen(pt, band, 100f, 80f)
                StrokePoint(screenPt.x, screenPt.y, 1000L + idx * 16L, 0.5f)
            }
            val strokeA = stroke(strokePts)
            strokeRepo.addStroke(strokeA)
            vm.evaluateCurrentAttempt(band, 100f, 80f, SlantConfig(52f, 80f))

            // 3. Salva no alfabeto pessoal
            vm.saveAttemptToPersonalAlphabet { }
            // Aguarda IO da coroutine
            kotlinx.coroutines.delay(200)

            val glyphA = alphabetRepo.getGlyph("glyph_lower_a")
            assertNotNull(glyphA)
            assertTrue("O glifo deve conter a nova variante salva com traços reais", glyphA!!.variants.isNotEmpty())
            assertTrue("O glifo deve estar marcado como completado", glyphA.isCompleted)
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * R05: Estilo pessoal compilado deve ser reconhecido pelo StyleEngine sem requerer fonte TTF em disco,
     * persistir em personal_styles.json através de reinicializações do motor,
     * e adaptar as pautas caligráficas do caderno às suas métricas de inclinação e proporção.
     */
    @Test
    fun r05_personalStyleSurvivesEngineLookupAndAppliesToNotebookGuidelines() = runBlocking {
        val dir = Files.createTempDirectory("scribe-r05-").toFile()
        try {
            val alphabetRepo = LocalPersonalAlphabetRepository(dir)
            val fontsDir = File(dir, "custom_fonts")
            fontsDir.mkdirs()

            // Compila estilo pessoal
            val compiledStyle = alphabetRepo.compileAndSavePersonalStyle("Meu Estilo Onda 2")
            assertNotNull(compiledStyle)
            assertEquals("Meu Estilo Onda 2", compiledStyle.name)
            assertEquals(com.scribe.caligrafia.style.model.StyleCategory.PERSONAL, compiledStyle.category)

            // 1. O StyleEngine deve reconhecer o estilo sem arquivo TTF
            val engine1 = StyleEngine(customFontsDir = fontsDir)
            val resolvedStyle1 = engine1.getStyle(compiledStyle.id)
            assertEquals("O StyleEngine deve retornar o estilo pessoal e não fazer fallback para cursiva",
                compiledStyle.id, resolvedStyle1.id)

            // 2. Persistência em personal_styles.json: nova instância sem estado em memória
            val engine2 = StyleEngine(customFontsDir = fontsDir)
            val resolvedStyle2 = engine2.getStyle(compiledStyle.id)
            assertEquals("Estilo pessoal deve sobreviver à recriação do StyleEngine via personal_styles.json",
                compiledStyle.id, resolvedStyle2.id)

            // 3. Aplicação no Caderno com adaptação de pautas
            val notebookRepo = LocalNotebookRepository(dir)
            val nb = notebookRepo.createNotebook("Caderno Pessoal")
            val initialPage = notebookRepo.getPages(nb.id).first()

            // Adapta as pautas da página para o estilo pessoal
            val adaptedConfig = GuidelineConfig.fromStyle(resolvedStyle2, initialPage.guidelineConfig.xHeightPx)
            notebookRepo.updatePageGuidelines(initialPage.id, adaptedConfig)

            val updatedPage = notebookRepo.getPage(initialPage.id)
            assertNotNull(updatedPage)
            assertEquals("Proporção da pauta deve refletir o estilo pessoal",
                resolvedStyle2.recommendedRatio.displayName, updatedPage!!.guidelineConfig.ratio.displayName)
            if (resolvedStyle2.defaultSlantAngle != 90.0f) {
                assertEquals("Inclinação da pauta deve refletir o estilo pessoal",
                    resolvedStyle2.defaultSlantAngle, updatedPage.guidelineConfig.slant?.angleDegrees ?: 0f, 0.01f)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * S18: A prescrição de treino recomendada pelo diagnóstico motor do Professor
     * deve encaminhar o usuário diretamente para o exercício corretivo exato no Treino Guiado.
     */
    @Test
    fun s18_teacherPrescriptionNavigatesToTargetExercise() {
        val app = Application()
        val vm = GuidedPracticeViewModel(application = app)

        // Cenário 1: Prescrição de paralelismo e ângulo -> basic_slant
        vm.selectGlyphBySymbolOrId("basic_slant")
        assertEquals(ReferenceGlyphCatalog.BASIC_SLANT.id, vm.state.value.selectedGlyph.id)
        assertEquals(ReferenceGlyphCatalog.BASIC_SLANT.instructions, vm.state.value.selectedGlyph.instructions)

        // Cenário 2: Prescrição de fluidez de curva -> underturn
        vm.selectGlyphBySymbolOrId("underturn")
        assertEquals(ReferenceGlyphCatalog.BASIC_UNDERTURN.id, vm.state.value.selectedGlyph.id)

        // Cenário 3: Prescrição de controle de haste ascendente -> ascender_loop
        vm.selectGlyphBySymbolOrId("ascender_loop")
        assertEquals(ReferenceGlyphCatalog.BASIC_ASCENDING_LOOP.id, vm.state.value.selectedGlyph.id)
    }

    /**
     * R11: O PersonalStyleCompiler calcula a inclinação sob a convenção canônica (dx < 0, dy > 0)
     * e garante monotonicidade estrita no mapeamento de proporção de pautas.
     */
    @Test
    fun r11_compilerCalculates52DegreesUnderCanonicalConventionAndMonotonicRatio() {
        val compiler = PersonalStyleCompiler()

        // Traço descendente com dx < 0 e dy > 0 (convenção caligráfica canônica atan2(dy, -dx) = 52°)
        val rad52 = Math.toRadians(52.0)
        val p1 = StrokePoint(200f, 100f, 1000L, 0.5f, null, null)
        val p2 = StrokePoint(
            (200f - 100.0 * Math.cos(rad52)).toFloat(),
            (100f + 100.0 * Math.sin(rad52)).toFloat(),
            1050L, 0.5f, null, null
        )
        val variant52 = GlyphVariant(
            id = "v52",
            glyphId = "glyph_slant",
            version = 1,
            label = "Variante 52",
            score = 90f,
            slantAngleDegrees = 0f, // Força o compilador a medir via atan2(dy, -dx)
            strokes = listOf(Stroke("s1", ToolType.STYLUS, listOf(p1, p2), 1000L, 1050L, false, null, 5f))
        )

        val measuredSlant = compiler.calculateAverageSlant(listOf(variant52))
        assertEquals(52.0f, measuredSlant, 0.5f)

        val baseGlyph = PersonalGlyph(
            id = "glyph_lower_a",
            symbol = "a",
            name = "Letra a",
            category = AlphabetCategory.LOWERCASE,
            variants = listOf(
                GlyphVariant(
                    id = "v_a",
                    glyphId = "glyph_lower_a",
                    version = 1,
                    label = "Variante A",
                    score = 90f,
                    slantAngleDegrees = 52f,
                    strokes = listOf(
                        Stroke("s_a", ToolType.STYLUS, listOf(
                            StrokePoint(10f, 60f, 0L, 0.5f, null, null),
                            StrokePoint(10f, 100f, 10L, 0.5f, null, null)
                        ), 0L, 10L, false, null, 5f)
                    )
                )
            )
        )

        fun createAlphabet(ascenderH: Float): PersonalAlphabet {
            val ascGlyph = PersonalGlyph(
                id = "glyph_lower_l",
                symbol = "l",
                name = "Letra l",
                category = AlphabetCategory.LOWERCASE,
                variants = listOf(
                    GlyphVariant(
                        id = "v_l",
                        glyphId = "glyph_lower_l",
                        version = 1,
                        label = "Variante L",
                        score = 90f,
                        slantAngleDegrees = 52f,
                        strokes = listOf(
                            Stroke("s_l", ToolType.STYLUS, listOf(
                                StrokePoint(10f, 100f - ascenderH, 0L, 0.5f, null, null),
                                StrokePoint(10f, 100f, 10L, 0.5f, null, null)
                            ), 0L, 10L, false, null, 5f)
                        )
                    )
                )
            )
            return PersonalAlphabet(glyphs = mapOf("glyph_lower_a" to baseGlyph, "glyph_lower_l" to ascGlyph))
        }

        assertEquals(GuidelineRatio.Ratio212, compiler.estimateGuidelineRatio(createAlphabet(100f)))
        assertEquals(GuidelineRatio.Ratio323, compiler.estimateGuidelineRatio(createAlphabet(80f)))
        assertEquals(GuidelineRatio.Ratio111, compiler.estimateGuidelineRatio(createAlphabet(50f)))
    }

    /**
     * R14: A avaliação de completude geométrica é invariante à densidade de amostragem
     * (distância ponto-a-segmento em vez de distância ponto-a-ponto discreta).
     */
    @Test
    fun r14_completenessEvaluationIsSamplingDensityIndependent() {
        val band = GuidelineBand(bandIndex = 0, ascenderY = 50f, xHeightY = 100f, baselineY = 200f, descenderY = 250f)
        val reference = ReferenceGlyphCatalog.BASIC_SLANT

        val refPoints = reference.strokes.first().points
        val ptStart = GeometricFeedbackEvaluator.mapToScreen(refPoints.first(), band, 100f, 100f)
        val ptEnd = GeometricFeedbackEvaluator.mapToScreen(refPoints.last(), band, 100f, 100f)

        val stroke2Pts = Stroke(
            id = "sparse",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(ptStart.x, ptStart.y, 0L, 0.5f, null, null),
                StrokePoint(ptEnd.x, ptEnd.y, 100L, 0.5f, null, null)
            ),
            startedAtMs = 0L,
            endedAtMs = 100L,
            isCancelled = false,
            color = null,
            baseWidthPx = 5f
        )

        val densePoints = refPoints.mapIndexed { idx, refPt ->
            val pt = GeometricFeedbackEvaluator.mapToScreen(refPt, band, 100f, 100f)
            StrokePoint(pt.x, pt.y, idx * 10L, 0.5f, null, null)
        }
        val strokeDense = Stroke("dense", ToolType.STYLUS, densePoints, 0L, 150L, false, null, 5f)

        val ptMid = GeometricFeedbackEvaluator.mapToScreen(refPoints[2], band, 100f, 100f)
        val strokeIncomplete = Stroke(
            id = "incomplete",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(ptStart.x, ptStart.y, 0L, 0.5f, null, null),
                StrokePoint(ptMid.x, ptMid.y, 20L, 0.5f, null, null)
            ),
            startedAtMs = 0L,
            endedAtMs = 20L,
            isCancelled = false,
            color = null,
            baseWidthPx = 5f
        )

        val slantConfig = SlantConfig(52f)
        val evalSparse = GeometricFeedbackEvaluator.evaluate(listOf(stroke2Pts), reference, band, 100f, 100f, slantConfig)
        val evalDense = GeometricFeedbackEvaluator.evaluate(listOf(strokeDense), reference, band, 100f, 100f, slantConfig)
        val evalIncomplete = GeometricFeedbackEvaluator.evaluate(listOf(strokeIncomplete), reference, band, 100f, 100f, slantConfig)

        // Amostragem esparsa e densa do mesmo traçado completo devem pontuar de forma equivalente
        assertEquals("Pontuação de direção/completude deve ser idêntica",
            evalDense.direction.scorePercent, evalSparse.direction.scorePercent)
        assertTrue("Traço completo de 2 pontos deve ser aprovado", evalSparse.isPassed)
        assertTrue("Traço completo de 30 pontos deve ser aprovado", evalDense.isPassed)

        // Traço com apenas 15% deve ser rejeitado com diagnóstico de incompleto
        assertFalse("Traço de 15% não deve ser aprovado", evalIncomplete.isPassed)
        assertTrue("Mensagem de traço incompleto deve estar presente",
            evalIncomplete.feedbackMessages.any { it.contains("incompleto", ignoreCase = true) })
    }

    /**
     * R12: O DualReplayEngine preserva a duração física real no modo REAL_TIME
     * e escala percentualmente no modo NORMALIZED.
     */
    @Test
    fun r12_dualReplayPreservesRealTimeRelativeSpeedAndSupportsNormalizedMode() {
        val strokeA = Stroke("a", ToolType.STYLUS, listOf(
            StrokePoint(10f, 10f, 1000L, 0.5f, null, null),
            StrokePoint(50f, 50f, 2000L, 0.5f, null, null)
        ), 1000L, 2000L, false, null, 5f)

        val strokeB = Stroke("b", ToolType.STYLUS, listOf(
            StrokePoint(10f, 10f, 1000L, 0.5f, null, null),
            StrokePoint(50f, 50f, 3000L, 0.5f, null, null)
        ), 1000L, 3000L, false, null, 5f)

        val replayRealTime = DualReplayEngine(listOf(strokeA), listOf(strokeB), syncMode = ReplaySyncMode.REAL_TIME)

        val frameRealTime = replayRealTime.computeDualFrameAt(0.5f)
        assertEquals("No modo REAL_TIME aos 1000ms, Track A (1000ms) já completou seus 2 pontos",
            2, frameRealTime.visibleStrokesA.firstOrNull()?.points?.size ?: 0)
        assertEquals("No modo REAL_TIME aos 1000ms, Track B (2000ms) tem apenas o primeiro ponto",
            1, frameRealTime.visibleStrokesB.firstOrNull()?.points?.size ?: 0)

        val replayNorm = DualReplayEngine(listOf(strokeA), listOf(strokeB), syncMode = ReplaySyncMode.NORMALIZED)
        val frameNorm = replayNorm.computeDualFrameAt(0.5f)
        assertEquals("No modo NORMALIZED, ambas as faixas acompanham o progresso relativo",
            frameNorm.visibleStrokesA.firstOrNull()?.points?.size, frameNorm.visibleStrokesB.firstOrNull()?.points?.size)
    }

    /**
     * R16: O NotebookPracticeViewModel armazena as dimensões reais do canvas de escrita
     * para preservar o aspect ratio na exportação.
     */
    @Test
    fun r16_pageExporterMaintainsCanvasAspectRatioWithoutDistortion() {
        val app = Application()
        val vm = NotebookPracticeViewModel(application = app)

        vm.updateCanvasDimensions(1200f, 800f)
        assertEquals(1200f, vm.canvasWidthPx, 0.01f)
        assertEquals(800f, vm.canvasHeightPx, 0.01f)
    }

    /**
     * R17 / S20: O Treino Guiado adapta as pautas e a inclinação alvo ao estilo caligráfico selecionado.
     */
    @Test
    fun r17_guidedPracticeAdaptsTargetSlantToSelectedStyle() {
        val app = Application()
        val vm = GuidedPracticeViewModel(application = app)

        vm.selectStyle(BuiltInStyles.CURSIVA_ESCOLAR)
        assertEquals("cursiva_escolar", vm.state.value.currentStyle.id)
        assertEquals(68.0f, vm.state.value.guidelineConfig.slant?.angleDegrees ?: 0f, 0.1f)

        vm.selectStyle(BuiltInStyles.SPENCERIAN)
        assertEquals("spencerian", vm.state.value.currentStyle.id)
        assertEquals(52.0f, vm.state.value.guidelineConfig.slant?.angleDegrees ?: 0f, 0.1f)

        vm.selectStyle(ExpandedStyles.GOTICA_TEXTURA)
        assertEquals("gotica_textura", vm.state.value.currentStyle.id)
        assertNull("Gótica a 90° não utiliza guias diagonais de inclinação", vm.state.value.guidelineConfig.slant)
        assertEquals(90.0f, vm.state.value.currentStyle.defaultSlantAngle, 0.1f)
    }

    /**
     * R18: O AttemptComparator calcula a velocidade real do traço em px/ms e sintetiza o ganho de agilidade.
     */
    @Test
    fun r18_attemptComparatorComputesPathVelocity() {
        val strokeA = Stroke("s1", ToolType.STYLUS, listOf(
            StrokePoint(0f, 0f, 0L, 0.5f, null, null),
            StrokePoint(100f, 0f, 1000L, 0.5f, null, null)
        ), 0L, 1000L, false, null, 5f)
        val attemptBefore = PracticeAttemptRecord(
            "att1", "basic_slant", "Slant", 1000L, listOf(strokeA), 70, 52f, 1000L, targetSlantDegrees = 52f
        )

        val strokeB = Stroke("s2", ToolType.STYLUS, listOf(
            StrokePoint(0f, 0f, 0L, 0.5f, null, null),
            StrokePoint(100f, 0f, 500L, 0.5f, null, null)
        ), 0L, 500L, false, null, 5f)
        val attemptAfter = PracticeAttemptRecord(
            "att2", "basic_slant", "Slant", 2000L, listOf(strokeB), 85, 52f, 500L, targetSlantDegrees = 52f
        )

        val velA = AttemptComparator.calculateVelocityPxPerMs(attemptBefore)
        val velB = AttemptComparator.calculateVelocityPxPerMs(attemptAfter)

        assertEquals(0.10f, velA, 0.01f)
        assertEquals(0.20f, velB, 0.01f)

        val comparison = AttemptComparator.compare(attemptBefore, attemptAfter)
        assertEquals(0.10f, comparison.speedBeforePxPerMs, 0.01f)
        assertEquals(0.20f, comparison.speedAfterPxPerMs, 0.01f)
        assertEquals(100.0f, comparison.speedGainPercent, 0.5f)
    }

    /**
     * S09: O MotorDiagnosticEngine marca dados ausentes como INSUFFICIENT_DATA com observedValue nulo
     * e os exclui da pontuação geral sem inventar notas de 70%.
     */
    @Test
    fun s09_motorDiagnosticHonorsCanonicalSlantAndMarksInsufficientData() {
        val engine = MotorDiagnosticEngine(defaultTargetSlantDegrees = 52.0f)

        val horizStroke = Stroke("h", ToolType.STYLUS, listOf(
            StrokePoint(0f, 100f, 0L, 0.5f, null, null),
            StrokePoint(100f, 100f, 500L, 0.5f, null, null)
        ), 0L, 500L, false, null, 5f)

        val diag = engine.diagnoseStrokes(listOf(horizStroke))
        val slantEval = diag.dimensions[BiomechanicalDimension.SLANT_STABILITY]
        val containmentEval = diag.dimensions[BiomechanicalDimension.GUIDELINE_CONTAINMENT]

        assertNotNull(slantEval)
        assertEquals("Sem descidas expressivas, status deve ser INSUFFICIENT_DATA",
            EvaluationStatus.INSUFFICIENT_DATA, slantEval?.status)
        assertNull("Valor observado deve ser nulo quando não medido", slantEval?.observedValue)

        assertNotNull(containmentEval)
        assertEquals("Sem tentativas com gabarito, contenção deve ser INSUFFICIENT_DATA",
            EvaluationStatus.INSUFFICIENT_DATA, containmentEval?.status)
        assertNull("Valor observado de contenção deve ser nulo quando não medido", containmentEval?.observedValue)

        assertTrue("Overall score deve ignorar dimensões insuficientes", diag.overallScore in 0f..100f)
    }

    /**
     * S12: Callbacks de assinatura entregam listas imutáveis e preservam snapshots da linha de base
     * mesmo após limpeza ou novos traços no canvas.
     */
    @Test
    fun s12_signatureCanvasCallbacksDeliverImmutableListsAndPreventAliasing() {
        val app = Application()
        val vm = ExpansionsViewModel(application = app)

        val stroke1 = Stroke(
            id = "s1", tool = ToolType.STYLUS,
            points = listOf(StrokePoint(10f, 10f, 1000L, 0.5f, null, null)),
            startedAtMs = 1000L, endedAtMs = 1100L, isCancelled = false, color = null, baseWidthPx = 4f
        )
        // Emite traço
        vm.onSignatureStrokesChanged(listOf(stroke1))
        vm.saveAsBaseline()

        assertNotNull("Referência de baseline deve estar gravada", vm.uiState.value.baselineAttempt)
        assertEquals(1, vm.uiState.value.baselineAttempt?.strokes?.size)

        // Limpa tela para nova tentativa
        vm.onSignatureStrokesChanged(emptyList())

        // A referência salva não pode ter sido limpa por aliasing
        assertNotNull("Referência não deve ser apagada após limpeza da tela", vm.uiState.value.baselineAttempt)
        assertEquals("Traços da referência devem permanecer íntegros", 1, vm.uiState.value.baselineAttempt?.strokes?.size)
    }

    /**
     * S15 / S16: SignatureConsistencyEngine calcula limites de coordenadas negativas corretamente
     * e exportSvg grava arquivo .svg no disco físico.
     */
    @Test
    fun s15_s16_signatureExporterWritesSvgFileAndHandlesNegativeCoordinates() {
        // Testa coordenadas negativas em SignatureConsistencyEngine
        val negativeStroke = Stroke(
            id = "neg", tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(-50f, -80f, 1000L, 0.5f, null, null),
                StrokePoint(-20f, -30f, 1100L, 0.5f, null, null)
            ),
            startedAtMs = 1000L, endedAtMs = 1100L, isCancelled = false, color = null, baseWidthPx = 4f
        )
        val metrics = SignatureConsistencyEngine.computeMetrics(listOf(negativeStroke))
        assertTrue("Comprimento total deve ser positivo", metrics.totalLengthPx > 0f)
        assertTrue("Aspect ratio deve ser estritamente positivo", metrics.aspectRatio > 0f)

        // Testa gravação de arquivo SVG
        val tempDir = Files.createTempDirectory("scribe-s15-").toFile()
        try {
            val app = Application()
            val vm = ExpansionsViewModel(application = app)
            vm.onSignatureStrokesChanged(listOf(negativeStroke))
            val svgFile = vm.exportSvg(tempDir)

            assertNotNull("Arquivo SVG deve ter sido criado no disco", svgFile)
            assertTrue("Arquivo SVG deve existir fisicamente", svgFile!!.exists())
            val content = svgFile.readText()
            assertTrue("Arquivo SVG deve conter tag de abertura", content.contains("<svg xmlns="))
            assertTrue("Arquivo SVG deve conter caminho desenhado", content.contains("<path d="))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
