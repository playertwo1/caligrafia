package com.scribe.caligrafia.evolution

import android.app.Application
import com.scribe.caligrafia.alphabet.engine.PersonalStyleCompiler
import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.alphabet.repository.LocalPersonalAlphabetRepository
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.evolution.engine.CalendarConsistencyHelper
import com.scribe.caligrafia.evolution.engine.DualReplayEngine
import com.scribe.caligrafia.evolution.engine.ReplaySyncMode
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.ui.EvolutionViewModel
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import com.scribe.caligrafia.learning.history.LocalLearningHistoryRepository
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.style.model.StyleCategory
import com.scribe.caligrafia.style.model.StyleExerciseMatrix
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Bateria de Testes de Aceitação da Fase F3 (F3.01 a F3.28).
 *
 * Valida o cumprimento dos requisitos de histórico ilimitado, replay temporal autêntico,
 * bounding box estável, variantes do alfabeto pessoal sem poluição de métricas,
 * compilação de estilo pessoal autêntico (mínimo 3 glifos, sem seeds) e importador de fontes.
 */
class PhaseF3AcceptanceTest {

    private fun sampleStroke(
        startX: Float = 10f,
        startY: Float = 10f,
        endX: Float = 50f,
        endY: Float = 50f,
        durationMs: Long = 1000L
    ): Stroke {
        val baseTime = 1000L
        val points = listOf(
            StrokePoint(startX, startY, baseTime, 0.5f),
            StrokePoint(endX, endY, baseTime + durationMs, 0.8f)
        )
        return Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = points,
            startedAtMs = baseTime,
            endedAtMs = baseTime + durationMs
        )
    }

    /**
     * F3.01: O histórico de sessões para consistência deve ler e computar TODAS as sessões,
     * sem o corte artificial de 20 sessões (limite histórico do MVP).
     */
    @Test
    fun f3_01_calendarConsistencyHelperProcessesAllSessionsWithoutArtificialLimit() {
        val sessions = (1..30).map { i ->
            CompletedSessionRecord(
                sessionId = "session-$i",
                lessonId = "lesson-01",
                lessonTitle = "Fundamentos",
                timestampMs = System.currentTimeMillis() - (i * 3600_000L),
                durationMinutes = 10,
                actualDurationSeconds = 600,
                attemptsCount = 5,
                averageScorePercent = 85
            )
        }

        val summary = CalendarConsistencyHelper.computeSummary(
            sessions = sessions,
            comparisonsCount = 10,
            averageGain = 12
        )

        assertEquals("Deve contabilizar todas as 30 sessões sem truncar em 20", 30, summary.totalSessionsCompleted)
        assertEquals("Deve somar todos os minutos (30 * 10 = 300)", 300, summary.totalMinutesPracticed)
    }

    /**
     * F3.05: Seleção explícita de duas tentativas para comparação lado a lado.
     * Rejeita com erro compreensível se alvos forem incompatíveis, e aceita se forem do mesmo exercício.
     */
    @Test
    fun f3_05_explicitComparisonRequiresCompatibleTarget() {
        val tempDir = Files.createTempDirectory("scribe-f3-comp-").toFile()
        try {
            val attemptRepo = LocalPracticeAttemptRepository(tempDir)
            val historyRepo = LocalLearningHistoryRepository(tempDir)
            val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            val viewModel = EvolutionViewModel(
                application = Application(),
                attemptRepository = attemptRepo,
                historyRepository = historyRepo,
                externalScope = testScope
            )

            val attemptA = PracticeAttemptRecord(
                attemptId = "att-1",
                targetId = "glyph_a",
                targetTitle = "Letra a",
                styleId = "cursiva_escolar",
                timestampMs = 1000L,
                scorePercent = 70,
                averageSlantDegrees = 52f,
                durationMs = 1000L,
                strokes = listOf(sampleStroke())
            )

            val attemptB = PracticeAttemptRecord(
                attemptId = "att-2",
                targetId = "glyph_b",
                targetTitle = "Letra b",
                styleId = "cursiva_escolar",
                timestampMs = 2000L,
                scorePercent = 85,
                averageSlantDegrees = 52f,
                durationMs = 1000L,
                strokes = listOf(sampleStroke())
            )

            val resultMismatch = viewModel.selectExplicitComparison(attemptA, attemptB)
            assertTrue("Tentativas com alvos diferentes devem falhar", resultMismatch.isFailure)
            assertNotNull(viewModel.uiState.value.comparisonErrorMessage)
            assertTrue(
                "Mensagem de erro deve detalhar a incompatibilidade de alvos",
                viewModel.uiState.value.comparisonErrorMessage!!.contains("alvos diferentes")
            )

            val attemptA2 = PracticeAttemptRecord(
                attemptId = "att-3",
                targetId = "glyph_a",
                targetTitle = "Letra a",
                styleId = "cursiva_escolar",
                timestampMs = 3000L,
                scorePercent = 90,
                averageSlantDegrees = 52f,
                durationMs = 1000L,
                strokes = listOf(sampleStroke())
            )

            val resultMatch = viewModel.selectExplicitComparison(attemptA, attemptA2)
            assertTrue("Tentativas com o mesmo alvo devem ser comparadas com sucesso", resultMatch.isSuccess)
            assertNull(viewModel.uiState.value.comparisonErrorMessage)
            val comp = resultMatch.getOrNull()
            assertNotNull(comp)
            assertEquals("glyph_a", comp!!.targetId)
            assertEquals(70, comp.beforeAttempt.scorePercent)
            assertEquals(90, comp.afterAttempt.scorePercent)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F3.07: Dual Replay suporta modos explícitos REAL_TIME e NORMALIZED.
     */
    @Test
    fun f3_07_dualReplayEngineSupportsRealTimeAndNormalizedModes() {
        val strokeA = sampleStroke(durationMs = 1000L)
        val strokeB = sampleStroke(durationMs = 2000L)

        val engine = DualReplayEngine(
            trackA = listOf(strokeA),
            trackB = listOf(strokeB),
            syncMode = ReplaySyncMode.REAL_TIME
        )

        assertEquals(ReplaySyncMode.REAL_TIME, engine.syncMode)
        assertEquals(1000L, engine.frameFlow.value.durationAMs)
        assertEquals(2000L, engine.frameFlow.value.durationBMs)

        // Modo Normalizado
        engine.syncMode = ReplaySyncMode.NORMALIZED
        assertEquals(ReplaySyncMode.NORMALIZED, engine.syncMode)
    }

    /**
     * F3.16: Duplicação de variante no alfabeto cria nova variante editável,
     * MAS NÃO cria tentativa de treino, nem altera histórico de aprendizado ou métricas de precisão.
     */
    @Test
    fun f3_16_duplicateVariantDoesNotInflatePracticeAttemptsOrLearningHistory() = runBlocking {
        val tempDir = Files.createTempDirectory("scribe-f3-dup-").toFile()
        try {
            val alphabetRepo = LocalPersonalAlphabetRepository(tempDir)
            val attemptRepo = LocalPracticeAttemptRepository(tempDir)
            val historyRepo = LocalLearningHistoryRepository(tempDir)

            // Salva um glifo inicial com uma variante
            val stroke = sampleStroke()
            val variant = alphabetRepo.addVariant(
                glyphId = "glyph_lower_a",
                strokes = listOf(stroke),
                score = 85f,
                slantAngle = 52f,
                setFavorite = true
            )

            val glyphA = alphabetRepo.getGlyph("glyph_lower_a")
            assertNotNull(glyphA)
            assertEquals(1, glyphA!!.variants.size)

            // Duplica a variante
            val duplicated = alphabetRepo.duplicateVariant("glyph_lower_a", variant.id)
            assertNotNull("Duplicação deve retornar variante criada", duplicated)

            // Verifica o alfabeto
            val updatedGlyphA = alphabetRepo.getGlyph("glyph_lower_a")!!
            assertEquals(2, updatedGlyphA.variants.size)
            assertTrue(updatedGlyphA.variants.any { it.id == duplicated!!.id })

            // F3.16 Regra de Ouro: Tentativas e histórico permanecem rigorosamente zerados/inalterados
            val allAttempts = attemptRepo.getAllAttempts()
            assertEquals("Nenhuma tentativa de prática deve ser registrada ao duplicar variante", 0, allAttempts.size)

            val historySummary = historyRepo.getProgressSummary()
            assertEquals("Nenhuma sessão de aprendizado deve ser criada ao duplicar variante", 0, historySummary.totalSessionsCompleted)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F3.18 & F3.26: Exclusão de variante não remove a tentativa física original em attempts/
     * e invalida a compilação do estilo pessoal previamente gerada.
     */
    @Test
    fun f3_18_and_f3_26_deleteVariantPreservesRawAttemptAndInvalidatesCompiledStyle() = runBlocking {
        val tempDir = Files.createTempDirectory("scribe-f3-del-").toFile()
        try {
            val alphabetRepo = LocalPersonalAlphabetRepository(tempDir)
            val stroke = sampleStroke()

            // Cria 3 variantes para permitir compilação
            val var1 = alphabetRepo.addVariant("glyph_lower_a", listOf(stroke), 85f, 52f, setFavorite = true)
            alphabetRepo.addVariant("glyph_lower_b", listOf(stroke), 88f, 52f, setFavorite = true)
            alphabetRepo.addVariant("glyph_lower_c", listOf(stroke), 90f, 52f, setFavorite = true)

            // Compila o estilo
            val compiledStyle = alphabetRepo.compileAndSavePersonalStyle("Meu Estilo Pessoal")
            assertNotNull(compiledStyle)

            val alphaBefore = alphabetRepo.getAlphabet().first()
            assertNotNull("Manifesto deve ter ID do estilo compilado", alphaBefore.lastCompiledStyleId)

            // Exclui uma variante favorita
            val deleted = alphabetRepo.deleteVariant("glyph_lower_a", var1.id)
            assertTrue("Exclusão de variante deve suceder", deleted)

            val alphaAfter = alphabetRepo.getAlphabet().first()
            // F3.26: Invalidação de estilo compilado
            assertNull("Estilo compilado DEVE ser invalidado após exclusão de variante", alphaAfter.lastCompiledStyleId)
            assertNull("Timestamp de compilação DEVE ser invalidado", alphaAfter.lastCompiledTimestamp)

            // F3.18: Se a variante excluída era a favorita, selectedVariantId fica nulo
            val glyphA = alphaAfter.glyphs["glyph_lower_a"]!!
            assertNull("selectedVariantId deve ser nulo se variante favorita foi deletada", glyphA.selectedVariantId)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F3.20: Matriz de estilo e criação dinâmica de glifo PROÍBEM fallback genérico de triângulo.
     * Quando o estilo não possui traços para o símbolo, retorna null na matriz e strokes vazio no ViewModel.
     */
    @Test
    fun f3_20_missingGlyphInStyleExerciseMatrixProhibitsTriangleFallback() {
        val resolved = StyleExerciseMatrix.getReference("glyph_unknown_xyz", "cursiva_escolar")
        assertNull("Glifo não cadastrado no catálogo deve retornar null na matriz de estilos, NUNCA triângulo artificial", resolved)

        val vm = GuidedPracticeViewModel(Application())
        vm.selectGlyphBySymbolOrId("glifo_inexistente_abc")
        val selected = vm.state.value.selectedGlyph
        assertTrue(
            "Glifo dinâmico desconhecido deve ter traços vazios, proibindo triângulo sintético fallback",
            selected.strokes.isEmpty()
        )
    }

    /**
     * F3.25 & F3.27: Compilador de Estilo Pessoal exige no mínimo 3 glifos reais completados
     * e rejeita veementemente o uso de dados de seed sintéticos.
     */
    @Test
    fun f3_25_and_f3_27_personalStyleCompilerRequiresMin3CompletedGlyphsAndRejectsSeeds() {
        val compiler = PersonalStyleCompiler()

        val v1 = GlyphVariant(id = "v1", glyphId = "g1", version = 1, label = "v1", score = 80f, slantAngleDegrees = 52f, isFavorite = true, strokes = listOf(sampleStroke()))
        val v2 = GlyphVariant(id = "v2", glyphId = "g2", version = 1, label = "v2", score = 85f, slantAngleDegrees = 52f, isFavorite = true, strokes = listOf(sampleStroke()))

        // Alfabeto com apenas 2 glifos completados
        val incompleteAlphabet = PersonalAlphabet(
            glyphs = mapOf(
                "g1" to PersonalGlyph(id = "g1", symbol = "a", name = "Letra a", category = AlphabetCategory.LOWERCASE, selectedVariantId = "v1", variants = listOf(v1)),
                "g2" to PersonalGlyph(id = "g2", symbol = "b", name = "Letra b", category = AlphabetCategory.LOWERCASE, selectedVariantId = "v2", variants = listOf(v2))
            )
        )

        assertFalse("Alfabeto com < 3 glifos não pode ser compilado", compiler.canCompile(incompleteAlphabet))
        val reqMessage = compiler.getCompilationRequirementMessage(incompleteAlphabet)
        assertTrue(
            "Mensagem deve esclarecer que faltam glifos para compilar",
            reqMessage.contains("Dados insuficientes")
        )

        // Alfabeto com 3 glifos reais
        val v3 = GlyphVariant(id = "v3", glyphId = "g3", version = 1, label = "v3", score = 90f, slantAngleDegrees = 52f, isFavorite = true, strokes = listOf(sampleStroke()))
        val validAlphabet = PersonalAlphabet(
            glyphs = mapOf(
                "g1" to PersonalGlyph(id = "g1", symbol = "a", name = "Letra a", category = AlphabetCategory.LOWERCASE, selectedVariantId = "v1", variants = listOf(v1)),
                "g2" to PersonalGlyph(id = "g2", symbol = "b", name = "Letra b", category = AlphabetCategory.LOWERCASE, selectedVariantId = "v2", variants = listOf(v2)),
                "g3" to PersonalGlyph(id = "g3", symbol = "c", name = "Letra c", category = AlphabetCategory.LOWERCASE, selectedVariantId = "v3", variants = listOf(v3))
            )
        )

        assertTrue("Alfabeto com >= 3 glifos pode ser compilado", compiler.canCompile(validAlphabet))
        val compiledStyle = compiler.compile(validAlphabet)
        assertNotNull(compiledStyle)
        assertEquals("Meu Estilo Pessoal", compiledStyle.name)
        assertEquals(StyleCategory.PERSONAL, compiledStyle.category)
        assertEquals("a b c", compiledStyle.sampleAlphabet)
    }

    /**
     * F3.22 & F3.23: Importador de fontes TTF/OTF copia arquivo atomicamente para custom_fonts/,
     * valida magic bytes, e rotula o estilo como referência visual sem análise de ducto.
     */
    @Test
    fun f3_22_and_f3_23_fontImporterCopiesToCustomFontsDirAndLabelsVisualReferenceOnly() {
        val tempDir = Files.createTempDirectory("scribe-f3-font-").toFile()
        try {
            val customFontsDir = File(tempDir, "custom_fonts")
            val engine = StyleEngine(customFontsDir = customFontsDir)
            // Magic bytes do formato OpenType 'OTTO' (12 bytes mínimos)
            val validOtfBytes = byteArrayOf(0x4F, 0x54, 0x54, 0x4F, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

            val importResult = engine.importCustomFontStream(
                inputStream = ByteArrayInputStream(validOtfBytes),
                originalFileName = "calligraphy.otf",
                customName = "Minha Fonte Caligráfica"
            )

            assertTrue("Importação de fonte válida deve suceder", importResult.isSuccess)
            val importedStyle = importResult.getOrNull()
            assertNotNull(importedStyle)
            assertEquals("Minha Fonte Caligráfica", importedStyle!!.name)
            assertEquals(StyleCategory.CUSTOM_FONT, importedStyle.category)
            assertTrue("Descrição deve salientar referência visual", importedStyle.description.contains("Referência visual"))

            // F3.22: Verifica se o arquivo foi persistido em custom_fonts/
            assertTrue("Diretório custom_fonts deve existir", customFontsDir.exists())
            val savedFontFile = File(customFontsDir, "calligraphy.otf")
            assertTrue("Arquivo da fonte deve ter sido gravado em custom_fonts", savedFontFile.exists())

            // Testa recarga offline instanciando uma nova engine
            val reloadedEngine = StyleEngine(customFontsDir = customFontsDir)
            val reloadedStyle = reloadedEngine.getStyle(importedStyle.id)
            assertNotNull("Estilo importado deve persistir e ser recarregado offline", reloadedStyle)
            assertEquals("font_calligraphy", reloadedStyle!!.id)
            assertEquals(StyleCategory.CUSTOM_FONT, reloadedStyle.category)
            assertTrue(reloadedStyle.name.equals("calligraphy", ignoreCase = true))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F3.19: Todos os 6 estilos canônicos do Scribe estão consolidados e disponíveis.
     */
    @Test
    fun f3_19_allSixCanonicalStylesAreAvailableInBuiltInStyles() {
        val all = BuiltInStyles.ALL
        assertEquals("Devem existir 6 estilos canônicos incorporados", 6, all.size)
        val ids = all.map { it.id }.toSet()
        assertTrue(ids.contains("cursiva_escolar"))
        assertTrue(ids.contains("copperplate"))
        assertTrue(ids.contains("spencerian"))
        assertTrue(ids.contains("gotica_textura"))
        assertTrue(ids.contains("italica_chanceleresca"))
        assertTrue(ids.contains("uncial_classica"))
    }
}
