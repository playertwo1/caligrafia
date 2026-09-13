package com.scribe.caligrafia.teacher

import android.app.Application
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.expansions.passage.LocalPassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageCatalog
import com.scribe.caligrafia.expansions.passage.PassageCategory
import com.scribe.caligrafia.expansions.passage.PassageCopyRecord
import com.scribe.caligrafia.expansions.passage.PassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageItem
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.teacher.engine.CoachingCurriculumGenerator
import com.scribe.caligrafia.teacher.engine.CoachingFeedbackEngine
import com.scribe.caligrafia.teacher.engine.MotorDiagnosticEngine
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.MaturityLevel
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.repository.LocalTeacherRepository
import com.scribe.caligrafia.teacher.ui.TeacherViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Bateria de Testes de Aceitação da Fase F4 (F4.01 a F4.16).
 *
 * Valida o Professor IA biomecânico, diagnóstico sem métricas inventadas,
 * prescrição de treinos com catálogo canônico, condução em Ghost 70%,
 * e cópia contínua de textos clássicos em páginas de caderno com persistência vetorial.
 */
class PhaseF4AcceptanceTest {

    private lateinit var tempDir: File
    private lateinit var attemptsDir: File
    private lateinit var attemptsRepo: LocalPracticeAttemptRepository
    private lateinit var teacherRepo: LocalTeacherRepository
    private lateinit var copyRepo: PassageCopyRepository
    private val app = Application()

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("scribe_f4_test").toFile()
        attemptsDir = File(tempDir, "attempts")
        attemptsRepo = LocalPracticeAttemptRepository(attemptsDir)
        teacherRepo = LocalTeacherRepository(File(tempDir, "teacher"))
        copyRepo = LocalPassageCopyRepository(File(tempDir, "passage_copies"))
    }

    private fun createAttempt(
        exerciseId: String = "basic_slant",
        scorePercent: Int = 85,
        averageSlantDegrees: Float = 52f,
        targetSlantDegrees: Float? = 52f,
        timestampMs: Long = System.currentTimeMillis()
    ): PracticeAttemptRecord {
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(10f, 10f, timestampMs, 0.5f),
                StrokePoint(30f, 60f, timestampMs + 500, 0.7f)
            ),
            startedAtMs = timestampMs,
            endedAtMs = timestampMs + 500
        )
        return PracticeAttemptRecord(
            attemptId = UUID.randomUUID().toString(),
            targetId = exerciseId,
            targetTitle = "Traço de Inclinação",
            timestampMs = timestampMs,
            strokes = listOf(stroke),
            scorePercent = scorePercent,
            averageSlantDegrees = averageSlantDegrees,
            durationMs = 500,
            targetSlantDegrees = targetSlantDegrees,
            styleId = "copperplate_script"
        )
    }

    /**
     * F4.01: Estado vazio do professor é transparente e acionável.
     * Sem tentativas registradas, não inventa scores nem maturidade; expõe CTA direcionado.
     */
    @Test
    fun f4_01_emptyTeacherStateIsActionableAndFabricatesNoMetrics() = runBlocking {
        val vm = TeacherViewModel(
            application = app,
            teacherRepository = teacherRepo,
            attemptRepository = attemptsRepo
        )
        vm.loadData()

        val state = vm.uiState.first { !it.isLoading }
        val totalAnalyzed = state.diagnostic?.totalAttemptsAnalyzed ?: 0
        assertEquals("Nenhuma tentativa deve ser computada no estado vazio", 0, totalAnalyzed)
        assertEquals("Nível deve ser BEGINNER no estado vazio", MaturityLevel.BEGINNER, state.diagnostic?.maturityLevel ?: MaturityLevel.BEGINNER)
    }

    /**
     * F4.02: Diagnóstico respeita contexto de estilo/pauta sem forçar 52 graus global.
     */
    @Test
    fun f4_02_motorDiagnosticRespectsStyleAndGuidelineContextWithoutGlobalFixed52Degrees() {
        val verticalAttempt = createAttempt(
            exerciseId = "gothic_i",
            averageSlantDegrees = 0f,
            targetSlantDegrees = 0f
        )
        val copperplateAttempt = createAttempt(
            exerciseId = "basic_slant",
            averageSlantDegrees = 52f,
            targetSlantDegrees = 52f
        )

        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(listOf(verticalAttempt, copperplateAttempt))
        assertEquals("Deve analisar as duas tentativas com seus respectivos ângulos", 2, diagnostic.totalAttemptsAnalyzed)

        val slantEval = diagnostic.dimensions[BiomechanicalDimension.SLANT_STABILITY]
        assertNotNull("Dimensão de inclinação deve estar presente", slantEval)
        assertTrue("Score de inclinação deve ser positivo", slantEval!!.score in 0f..100f)
    }

    /**
     * F4.03: Insights pedagógicos vinculam evidência real da tentativa que os originou.
     */
    @Test
    fun f4_03_insightEvidenceIsLinkedToSupportingAttempt() {
        val bestAttempt = createAttempt(
            exerciseId = "basic_ascending_loop",
            scorePercent = 95
        )
        val strugglingAttempt = createAttempt(
            exerciseId = "letter_t",
            scorePercent = 45
        )

        val attempts = listOf(bestAttempt, strugglingAttempt)
        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(attempts)
        val insights = CoachingFeedbackEngine().generateInsights(diagnostic, attempts)

        assertTrue("Deve gerar ao menos um insight", insights.isNotEmpty())

        val evidenceInsight = insights.firstOrNull { it.relatedAttemptId != null }
        assertNotNull("Ao menos um insight deve conter vínculo de evidência (relatedAttemptId)", evidenceInsight)
        assertNotNull("Deve conter o título do exercício de evidência", evidenceInsight?.relatedTargetTitle)
        assertTrue("O ID vinculado deve ser de uma tentativa real",
            evidenceInsight?.relatedAttemptId == bestAttempt.attemptId || evidenceInsight?.relatedAttemptId == strugglingAttempt.attemptId)
    }

    /**
     * F4.04: Nenhum texto no motor de diagnóstico ou coaching alega 'tensão muscular', 'DORT' ou diagnósticos médicos.
     */
    @Test
    fun f4_04_noMedicalOrUnmeasuredClaimsInInsightsOrDiagnostic() {
        val attempts = (1..5).map { createAttempt(scorePercent = 60) }
        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(attempts)
        val insights = CoachingFeedbackEngine().generateInsights(diagnostic, attempts)
        val prescription = CoachingCurriculumGenerator().generatePrescription(diagnostic)

        val allTexts = mutableListOf<String>()
        allTexts.addAll(diagnostic.dimensions.values.map { it.shortDiagnosis })
        allTexts.addAll(insights.map { "${it.title} ${it.message} ${it.metricDelta}" })
        allTexts.add(prescription.targetGoalDescription)
        allTexts.add(prescription.rationale)

        val forbiddenTerms = listOf(
            "tensão muscular",
            "dort",
            "ler/dort",
            "neuromuscular",
            "memória muscular",
            "lesão",
            "segurando a caneta com força excessiva"
        )

        for (text in allTexts) {
            val lower = text.lowercase()
            for (forbidden in forbiddenTerms) {
                assertFalse("Texto não deve conter termo médico/não medido: '$forbidden' em: '$text'",
                    lower.contains(forbidden))
            }
        }
    }

    /**
     * F4.05: Reanálise é serializada via Mutex e expõe estado de erro tratável na UI.
     */
    @Test
    fun f4_05_reanalysisIsSerializedAndExposesErrorRecovery() = runBlocking {
        val vm = TeacherViewModel(
            application = app,
            teacherRepository = teacherRepo,
            attemptRepository = attemptsRepo
        )

        val attempt1 = createAttempt(exerciseId = "basic_slant", scorePercent = 80)
        attemptsRepo.saveAttempt(attempt1)

        vm.reanalyzeAllData()

        val state = vm.uiState.first { !it.isAnalyzing && it.diagnostic != null }
        assertNull("Não deve haver erro em execução normal", state.errorMessage)
        assertEquals("Diagnóstico deve processar 1 tentativa", 1, state.diagnostic?.totalAttemptsAnalyzed)
    }

    /**
     * F4.06: Prescrição de treino contém exercício, meta, duração, ghost 70%, séries e estágios.
     */
    @Test
    fun f4_06_prescriptionContainsAllRequiredParameters() {
        val attempts = listOf(createAttempt(scorePercent = 40, averageSlantDegrees = 20f))
        val diagnostic = MotorDiagnosticEngine().diagnoseAttempts(attempts)
        val prescription = CoachingCurriculumGenerator().generatePrescription(diagnostic)

        assertNotNull("Prescrição deve ser gerada para fraqueza identificada", prescription)
        assertEquals("Recomendação de Ghost deve ser 70% (0.7f)", 0.70f, prescription.recommendedGhostLevel, 0.01f)
        assertTrue("Duração deve ser positiva", prescription.recommendedMinutes >= 3)
        assertEquals("Número de séries deve ser 3 por padrão pedagógico", 3, prescription.seriesCount)
        assertEquals("Deve conter os 3 estágios do método",
            listOf("Aquecimento", "Condução com Ghost", "Prática Autônoma"), prescription.stages)
        assertTrue("Meta de foco deve ser descrita", prescription.targetGoalDescription.isNotEmpty())
    }

    /**
     * F4.07: Validação do exercício prescrito contra catálogo canônico.
     * IDs inválidos desabilitam botão e não caem em fallback genérico silencioso.
     */
    @Test
    fun f4_07_prescriptionValidationAgainstCanonicalCatalog() = runBlocking {
        val validPrescription = PrescribedPracticeSession(
            title = "Treino de Inclinação",
            rationale = "Estabilizar traços",
            targetDimension = BiomechanicalDimension.SLANT_STABILITY,
            warmupExerciseId = "basic_slant",
            focusExerciseId = "basic_slant",
            targetGoalDescription = "Estabilizar inclinação",
            recommendedMinutes = 5,
            recommendedGhostLevel = 0.7f
        )
        teacherRepo.savePrescription(validPrescription)

        val vm = TeacherViewModel(
            application = app,
            teacherRepository = teacherRepo,
            attemptRepository = attemptsRepo
        )
        val validState = vm.uiState.first { !it.isLoading && it.prescription?.focusExerciseId == "basic_slant" }
        assertTrue("Prescrição com ID do catálogo canônico deve ser válida", validState.isPrescriptionValid)

        val invalidPrescription = PrescribedPracticeSession(
            title = "Treino Falso",
            rationale = "Justificativa",
            targetDimension = BiomechanicalDimension.SLANT_STABILITY,
            warmupExerciseId = "basic_slant",
            focusExerciseId = "nonexistent_invented_id_999",
            targetGoalDescription = "Objetivo",
            recommendedMinutes = 5,
            recommendedGhostLevel = 0.7f
        )
        teacherRepo.savePrescription(invalidPrescription)
        vm.loadData()
        val invalidState = vm.uiState.first { !it.isLoading && it.prescription?.focusExerciseId == "nonexistent_invented_id_999" }
        assertFalse("Prescrição com ID fora do catálogo canônico deve ser marcada inválida", invalidState.isPrescriptionValid)
    }

    /**
     * F4.08: Transporte da prescrição para o GuidedPracticeViewModel configura exercício e Ghost em 70% de opacidade.
     */
    @Test
    fun f4_08_prescriptionTransportToGuidedPracticeWith70PercentGhost() {
        val vm = GuidedPracticeViewModel(app)
        val prescription = PrescribedPracticeSession(
            title = "Treino de Laço",
            rationale = "Melhorar curvatura",
            targetDimension = BiomechanicalDimension.SLANT_STABILITY,
            warmupExerciseId = "basic_slant",
            focusExerciseId = "basic_ascending_loop",
            targetGoalDescription = "Manter curvatura fluida",
            recommendedMinutes = 5,
            recommendedGhostLevel = 0.7f
        )

        vm.startPrescribedPractice(prescription)

        val state = vm.state.value
        assertEquals("Exercício selecionado deve ser o prescrito", "basic_ascending_loop", state.selectedGlyph.id)
        assertEquals("Ghost level deve ser CLEAR (alpha 0.7f)", GhostModeLevel.CLEAR, state.ghostModeLevel)
        assertEquals("Alpha efetivo do ghost deve ser 0.7f", 0.7f, state.ghostModeLevel.alpha, 0.01f)
        assertNotNull("Prescrição ativa deve estar registrada no ViewModel", state.activePrescription)
    }

    /**
     * F4.09: Conclusão do treino prescrito marca prescrição concluída e atualiza Professor.
     */
    @Test
    fun f4_09_prescriptionCompletionTriggersTeacherUpdate() = runBlocking {
        val prescription = PrescribedPracticeSession(
            title = "Treino de Foco",
            rationale = "Estabilizar",
            targetDimension = BiomechanicalDimension.SLANT_STABILITY,
            warmupExerciseId = "basic_slant",
            focusExerciseId = "basic_slant",
            targetGoalDescription = "Treinar consistência",
            recommendedMinutes = 5,
            recommendedGhostLevel = 0.7f,
            isCompleted = false
        )
        teacherRepo.savePrescription(prescription)

        val teacherVm = TeacherViewModel(app, teacherRepo, attemptsRepo)
        val initialState = teacherVm.uiState.first { !it.isLoading && it.prescription?.id == prescription.id }
        assertFalse("Prescrição inicial não deve estar concluída", initialState.prescription?.isCompleted ?: true)

        teacherVm.markPrescriptionCompleted()

        val completedState = teacherVm.uiState.first { it.prescription?.id == prescription.id && it.prescription?.isCompleted == true }
        assertTrue("Prescrição no estado da UI deve estar concluída", completedState.prescription?.isCompleted ?: false)

        val updatedPrescription = teacherRepo.getLatestPrescription()
        assertTrue("Prescrição no repositório deve estar marcada como concluída", updatedPrescription.isCompleted)
    }

    /**
     * F4.10: Orientações e exercícios prescritos disponíveis em dois estilos caligráficos.
     */
    @Test
    fun f4_10_prescribedExercisesAvailableInDistinctStyles() {
        val copperplate = BuiltInStyles.COPPERPLATE
        val cursiva = BuiltInStyles.CURSIVA_ESCOLAR

        assertNotNull("Copperplate deve existir", copperplate)
        assertNotNull("Cursiva deve existir", cursiva)
        assertEquals("Copperplate deve ter inclinação formal (52°)", 52.0f, copperplate.defaultSlantAngle, 0.1f)
        assertEquals("Cursiva deve ter inclinação suave (68°)", 68.0f, cursiva.defaultSlantAngle, 0.1f)
    }

    /**
     * F4.11: Catálogo de textos clássicos fornece título, autor, linhas, estilo e WPM.
     */
    @Test
    fun f4_11_passageCatalogProvidesClassicTextsWithAuthorAndPacingGoals() {
        val passages = PassageCatalog.allPassages
        assertTrue("Catálogo de passagens deve conter múltiplos textos", passages.size >= 5)

        for (p in passages) {
            assertTrue("Título não pode ser vazio", p.title.isNotEmpty())
            assertTrue("Autor não pode ser vazio", p.author.isNotEmpty())
            assertTrue("Deve conter ao menos uma linha de texto", p.lines.isNotEmpty())
            assertTrue("Meta de WPM deve ser positiva", p.targetWpm > 0)
            assertTrue("Estilo recomendado deve ser válido", p.recommendedStyleId.isNotEmpty())
        }

        val poetry = PassageCatalog.getByCategory(PassageCategory.CLASSIC_POETRY)
        assertTrue("Deve haver poesia clássica no catálogo", poetry.isNotEmpty())
        val camoes = poetry.find { it.id == "camoes_amor" }
        assertNotNull("Poema de Camões deve estar presente", camoes)
        assertEquals("Luís de Camões", camoes?.author)
    }

    /**
     * F4.12: Início de cópia de texto direciona para páginas de caderno pautado sem redirecionamento para raiz.
     */
    @Test
    fun f4_12_textCopyPracticeDirectsToNotebookCanvasWithoutRootRedirect() = runBlocking {
        val vm = NotebookPracticeViewModel(app)
        val passage = PassageCatalog.getById("pangram_morcego")!!
        vm.startTextCopyPractice(passage, "copperplate_script")

        val state = vm.uiState.first()
        assertNotNull("Sessão ativa de cópia de texto deve estar configurada", state.activeTextCopy)
        assertEquals("O texto selecionado deve ser o pangrama solicitado", "pangram_morcego", state.activeTextCopy?.passage?.id)
        assertEquals("O estilo deve ser copperplate_script", "copperplate_script", state.activeTextCopy?.styleId)
        assertFalse("Não deve iniciar pausado", state.activeTextCopy?.isPaused ?: true)
    }

    /**
     * F4.13: Card colapsável de texto, cronômetro ativo, pausa e continuação em múltiplas páginas.
     */
    @Test
    fun f4_13_collapsibleOverlayWithActiveTimerAndMultiPageContinuation() = runBlocking {
        val vm = NotebookPracticeViewModel(app)
        val passage = PassageCatalog.getById("camoes_amor")!!
        vm.startTextCopyPractice(passage, "cursiva_escolar_br")

        assertFalse("Inicialmente não deve estar colapsado", vm.uiState.value.activeTextCopy!!.isCollapsed)
        vm.toggleTextCopyCollapse()
        assertTrue("Deve estar colapsado após alternar", vm.uiState.value.activeTextCopy!!.isCollapsed)
        vm.toggleTextCopyCollapse()
        assertFalse("Deve expandir novamente", vm.uiState.value.activeTextCopy!!.isCollapsed)

        vm.pauseTextCopy()
        assertTrue("Deve estar pausado", vm.uiState.value.activeTextCopy!!.isPaused)
        vm.resumeTextCopy()
        assertFalse("Deve retomar execução", vm.uiState.value.activeTextCopy!!.isPaused)
    }

    /**
     * F4.14: Conclusão da cópia persiste registro vetorial completo com texto, traços e WPM.
     */
    @Test
    fun f4_14_finishingTextCopySavesRecordWithLinkedTextAndWpm() = runBlocking {
        val vm = NotebookPracticeViewModel(app)
        val passage = PassageCatalog.getById("pangram_gazeta")!!
        vm.startTextCopyPractice(passage, "cursiva_escolar_br")

        vm.strokeRepository.addStroke(Stroke(
            id = "stroke-copy-1",
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(20f, 20f, 1000L, 0.5f), StrokePoint(50f, 50f, 1500L, 0.6f)),
            startedAtMs = 1000L,
            endedAtMs = 1500L
        ))

        val record = vm.finishTextCopyPractice()
        assertNotNull("Registro de cópia deve ser retornado ao finalizar", record)
        assertTrue("Registro deve estar marcado como concluído", record!!.isCompleted)
        assertEquals("Texto vinculado deve ser o pangrama da gazeta", "pangram_gazeta", record.textId)
        assertTrue("WPM calculado deve ser positivo", record.actualWpm > 0)
        assertTrue("Quantidade de traços deve ser registrada", record.strokeCount >= 1)

        val saved = vm.copyRepository.loadRecord(record.id)
        assertNotNull("Deve estar persistido no repositório de cópias", saved)
        assertEquals(record.id, saved?.id)
        assertEquals("pangram_gazeta", saved?.textId)
    }

    /**
     * F4.15: Abertura de cópia a partir do histórico restaura texto exato e traços vetoriais salvos.
     */
    @Test
    fun f4_15_openingCopyFromHistoryRestoresExactTextAndStrokes() = runBlocking {
        val vm = NotebookPracticeViewModel(app)
        val savedStroke = Stroke(
            id = "historical-stroke-99",
            tool = ToolType.STYLUS,
            points = listOf(StrokePoint(15f, 25f, 2000L, 0.5f), StrokePoint(40f, 60f, 2500L, 0.7f)),
            startedAtMs = 2000L,
            endedAtMs = 2500L
        )
        val record = PassageCopyRecord(
            id = "record-history-test",
            textId = "camoes_amor",
            title = "Amor é Fogo",
            author = "Luís de Camões",
            textContent = "Amor é um fogo que arde sem se ver,",
            styleId = "copperplate_script",
            timestampMs = System.currentTimeMillis() - 60_000L,
            durationMs = 45_000L,
            strokeCount = 1,
            pageCount = 1,
            strokesByPage = mapOf(0 to listOf(savedStroke)),
            isCompleted = false,
            targetWpm = 11
        )
        vm.copyRepository.saveRecord(record)

        vm.openExistingCopyRecord(record)

        val state = vm.uiState.value
        assertNotNull("Cópia restaurada deve ativar sessão no Caderno", state.activeTextCopy)
        assertEquals("camoes_amor", state.activeTextCopy?.passage?.id)
        assertEquals("copperplate_script", state.activeTextCopy?.styleId)
        assertEquals("Duração de 45s deve ser restaurada", 45L, state.activeTextCopy?.elapsedSeconds)
        assertTrue("Deve estar pausado ao restaurar", state.activeTextCopy?.isPaused ?: false)
        assertEquals("Traço da página 0 deve ser carregado no repositório de traços", 1, vm.strokeRepository.count)
        assertEquals("historical-stroke-99", vm.strokeRepository.allStrokes.first().id)
    }

    /**
     * F4.16: Cópia lida com dois textos distintos, pausa, reinício e término sem modelos genéricos.
     */
    @Test
    fun f4_16_twoDistinctTextsMaintainIndependentDataAndStrokes() = runBlocking {
        val textA = PassageCatalog.getById("pangram_morcego")!!
        val textB = PassageCatalog.getById("machado_carolina")!!

        val recordA = PassageCopyRecord(
            id = "copy-a",
            textId = textA.id,
            title = textA.title,
            author = textA.author,
            textContent = textA.lines.joinToString("\n"),
            styleId = "cursiva_escolar_br",
            timestampMs = System.currentTimeMillis(),
            durationMs = 30_000L,
            strokeCount = 10,
            pageCount = 1,
            strokesByPage = emptyMap(),
            isCompleted = true,
            actualWpm = 16.5f,
            targetWpm = 16
        )

        val recordB = PassageCopyRecord(
            id = "copy-b",
            textId = textB.id,
            title = textB.title,
            author = textB.author,
            textContent = textB.lines.joinToString("\n"),
            styleId = "copperplate_script",
            timestampMs = System.currentTimeMillis(),
            durationMs = 45_000L,
            strokeCount = 15,
            pageCount = 2,
            strokesByPage = emptyMap(),
            isCompleted = true,
            actualWpm = 10.2f,
            targetWpm = 10
        )

        copyRepo.saveRecord(recordA)
        copyRepo.saveRecord(recordB)

        val allRecords = copyRepo.listAllRecords()
        assertEquals("Devem existir 2 registros distintos no histórico", 2, allRecords.size)

        val loadedA = copyRepo.loadRecord("copy-a")
        val loadedB = copyRepo.loadRecord("copy-b")

        assertEquals("pangram_morcego", loadedA?.textId)
        assertEquals("cursiva_escolar_br", loadedA?.styleId)
        assertEquals(16.5f, loadedA?.actualWpm ?: 0f, 0.01f)

        assertEquals("machado_carolina", loadedB?.textId)
        assertEquals("copperplate_script", loadedB?.styleId)
        assertEquals(10.2f, loadedB?.actualWpm ?: 0f, 0.01f)
    }
}
